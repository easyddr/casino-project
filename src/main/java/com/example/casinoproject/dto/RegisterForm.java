package com.example.casinoproject.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_.]{3,50}", message = "Логін: 3-50 латинських літер, цифр, _ або крапка")
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Пароль від 8 до 72 символів")
    private String password;

    @NotBlank
    private String confirmPassword;
}
