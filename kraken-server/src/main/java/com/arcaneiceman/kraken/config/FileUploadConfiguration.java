package com.arcaneiceman.kraken.config;

import com.amazonaws.regions.Regions;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import com.ttt.eru.libs.fileupload.listener.AmazonS3ResultListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Wali on 27/02/18.
 */
@Configuration
public class FileUploadConfiguration extends AmazonS3Configuration {

    @Value("${amazon.aws.accesskey}")
    private String accessKey;

    @Value("${amazon.aws.secretkey}")
    private String secretKey;

    @Value("${amazon.s3.bucketname}")
    private String bucketName;

    @Value("${amazon.region}")
    private String region;

    @Override
    public Regions getAmazonS3Region() {
        return Regions.valueOf(region);
    }

    @Override
    public String getAmazonS3AccessKey() {
        return accessKey;
    }

    @Override
    public String getAmazonS3SecretKey() {
        return secretKey;
    }

    @Override
    public String getAmazonS3BucketName() {
        return bucketName;
    }

    @Override
    public AmazonS3ResultListener getAmazonS3ResultListener() {
        return new AmazonS3ResultListener() {
            private Logger log = LoggerFactory.getLogger(AmazonS3ResultListener.class);

            @Override
            public void onFailure(Exception e) {
                log.error(e.getMessage());
            }

            @Override
            public void onSuccess() {
                log.info("File Uploaded Successfully");
            }

            @Override
            public void onFailure() {
                log.error("File failed to upload");
            }
        };
    }

    @Override
    public Integer getMultipartUploadTimeout() {
        return null;
    }

    @Override
    public Long getLinkActiveTimeout() {
        return 300000L;
    }
}
