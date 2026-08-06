package com.liftsync.messaging.email;

import com.liftsync.config.rabbitmq.properties.EmailMessagingProperties;
import com.liftsync.messaging.common.MessagingTopology;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class EmailMessageConfig {

    @Bean
    public Declarables emailTopology(EmailMessagingProperties properties) {
        return MessagingTopology.declarables(properties);
    }

}
