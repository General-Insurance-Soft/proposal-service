package app.g_agent.proposal_service.service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;

@Service
public class B2ClientFactory {

    @Value("${backblaze.s3.endpoint-url}")
    private  String ENDPOINT_URL;
    @Value("${backblaze.s3.bucket-name}")
    private String bucketName;

    private  final Logger logger = LoggerFactory.getLogger(ProposalService.class);

    public  S3Client createClient() {

        Matcher matcher = Pattern.compile("https://s3\\.([a-z0-9-]+)\\.backblazeb2\\.com")
                .matcher(ENDPOINT_URL.trim());

        String region = matcher.find() ? matcher.group(1) : null;
        if (region == null) {
            logger.error("Can't find a region in the endpoint URL: " + ENDPOINT_URL);
            return null;
        }

        try {
            return S3Client.builder()
                    .endpointOverride(new URI(ENDPOINT_URL))
                    .region(Region.of(region))
                    .credentialsProvider(ProfileCredentialsProvider.create("gisca"))
                    .build();
        } catch (Exception e) {
            logger.error("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}