package com.nantaaditya.cronscheduler.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
  @Bean
  public OpenAPI api() {
    return new OpenAPI()
        .info(new Info()
            .title("DB Migration API")
            .version("0.0.1")
            .contact(new Contact()
                .name("Nanta Aditya")
                .url("https://nantaaditya.com")
                .email("personal@nantaaditya.com")
            )
            .license(new License()
                .name("GNU AGPLv3")
                .url("https://www.gnu.org/licenses/agpl-3.0.en.html")
            )
        );
  }
}
