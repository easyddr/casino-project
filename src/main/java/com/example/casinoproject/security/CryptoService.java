package com.example.casinoproject.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Шифрування чутливих даних (номери карток) алгоритмом AES-256-GCM і підписи HMAC-SHA256.
 * Ключ береться з властивості casino.crypto.key у base64, він має бути рівно 32 байти.
 */
@Component
public class CryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random;

    public CryptoService(@Value("${casino.crypto.key}") String base64Key, SecureRandom random) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalStateException("casino.crypto.key має бути 32 байти в base64 (AES-256)");
        }
        this.key = new SecretKeySpec(raw, "AES");
        this.random = random;
    }

    /**
     * Шифрує рядок і повертає base64 від (IV + шифротекст). Для кожного виклику генерується новий IV,
     * тому той самий номер картки двічі дасть різний результат, і це нормально.
     */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не вдалося зашифрувати дані", e);
        }
    }

    /**
     * Розшифровує те, що повернув encrypt. Якщо дані підмінили або ключ інший, GCM це помітить і кине виняток.
     */
    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Не вдалося розшифрувати дані", e);
        }
    }

    /**
     * HMAC-SHA256 від рядка у hex. Використовується, щоб перевірити, що відповідь платіжного шлюзу ніхто не підробив.
     */
    public String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Порівняння підписів за сталий час, щоб не можна було підбирати підпис по часу відповіді.
     */
    public boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Маска для показу: лишає перші 4 і останні 4 цифри, наприклад 4242 **** **** 4242.
     */
    public static String maskCard(String digits) {
        String last = digits.substring(digits.length() - 4);
        return digits.substring(0, 4) + " **** **** " + last;
    }
}
