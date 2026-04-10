//Here we are configuring the S3 client and presigner beans for our application. 
// We are using the AWS SDK for Java v2, which provides a more modern and efficient way to interact with AWS services(build for Java 8 and above and with immutable objects). 
// The S3Client bean will be used for regular S3 operations, while the S3Presigner bean will be used to generate pre-signed URLs for secure access to S3 objects. We are also using Spring's @Value annotation to inject the AWS credentials and region from our application properties.
package com.liftsync.config.amazon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${aws.region}") String region,
            @Value("${aws.access.key.id}") String accessKey,
            @Value("${aws.secret.access.key}") String secretKey
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${aws.region}") String region,
            @Value("${aws.access.key.id}") String accessKey,
            @Value("${aws.secret.access.key}") String secretKey
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }
}

