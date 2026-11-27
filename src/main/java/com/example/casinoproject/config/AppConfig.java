package com.example.casinoproject.config;

import com.example.casinoproject.game.PricePathGenerator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.SecureRandom;

@Configuration
@EnableScheduling
public class AppConfig {

    /**
     * Один спільний криптостійкий генератор випадкових чисел (singleton) для всіх ігор і шифрування.
     * SecureRandom потокобезпечний, тому ділити його між запитами можна.
     */
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * Генератор графіка ціни для крипто-гри. Він тримає стан конкретного раунду (поточну ціну),
     * тому це prototype: кожен виклик ObjectProvider.getObject() дає новий екземпляр.
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public PricePathGenerator pricePathGenerator(SecureRandom random) {
        return new PricePathGenerator(random);
    }

    /**
     * Загальний опис API для Swagger UI (/swagger-ui.html). Кнопка Authorize приймає логін і пароль
     * звичайного акаунта, адмінські операції потребують акаунта з роллю ADMIN.
     */
    @Bean
    public OpenAPI casinoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Casino API")
                        .version("1.0")
                        .description("REST API навчального казино: каталог ігор, гаманець, псевдо-платежі, ігри та адмінка."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
