package de.widdix.awss3virusscan;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class InfectedFileCache extends AAWSTest {

    public static class InfectedFile {

        private final String key;

        private final String contentType;

        private final byte[] content;

        public InfectedFile(final String key, final byte[] content, final String contentType) {
            this.key = key;
            this.contentType = contentType;
            this.content = content;
        }

        public String getkey() {
            return this.key;
        }

        public String getContentType() {
            return this.contentType;
        }

        public long getContentLength() {
            return this.content.length;
        }

        public InputStream getContent() {
            return new ByteArrayInputStream(this.content);
        }
    }

    public List<InfectedFile> getFiles() {
        final List<InfectedFile> files = new ArrayList<>();
        if (Config.has(Config.Key.INFECTED_FILES_BUCKET_NAME)) {
            final AmazonS3 s3local = AmazonS3ClientBuilder.standard().withCredentials(this.credentialsProvider).withRegion(Config.get(Config.Key.INFECTED_FILES_BUCKET_REGION)).build();
            ObjectListing objectListing = s3local.listObjects(Config.get(Config.Key.INFECTED_FILES_BUCKET_NAME));
            while (true) {
                objectListing.getObjectSummaries().forEach((summary) -> {
                    final S3Object object = s3local.getObject(summary.getBucketName(), summary.getKey());
                    final byte[] content;
                    try {
                        content = IOUtils.toByteArray(object.getObjectContent());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                    files.add(new InfectedFile(summary.getKey(), content, object.getObjectMetadata().getContentType()));
                });
                if (objectListing.isTruncated()) {
                    objectListing = s3local.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
        }
        return files;
    }
}
