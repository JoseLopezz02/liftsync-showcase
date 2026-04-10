// Here we handle internationalization (i18n) in our application. 
// We define a LocaleResolver bean that determines the user's locale based on the Accept-Language 
// header in the HTTP request. We also specify a default locale (English) and a list of supported locales 
// (English and Spanish). Additionally, we configure a MessageSource bean that loads message properties 
// files (e.g., messages_en.properties, messages_es.properties) to provide localized messages for our application. 
// This setup allows us to easily support multiple languages and provide a better user experience for users from different regions.
package com.liftsync.config;

public package com.liftsync.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class InternationalizationConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, new Locale("es")));
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
 {
    
}
