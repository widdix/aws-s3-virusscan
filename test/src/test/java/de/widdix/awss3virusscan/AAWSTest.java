package de.widdix.awss3virusscan;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public abstract class AAWSTest extends ATest {

    public final static String IAM_SESSION_NAME = "aws-s3-virusscan";

    protected final AWSCredentialsProvider credentialsProvider;

    private final AmazonS3 s3;

    public AAWSTest() {
        super();
        if (Config.has(Config.Key.IAM_ROLE_ARN)) {
            final AWSSecurityTokenService local = AWSSecurityTokenServiceClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
            this.credentialsProvider = new STSAssumeRoleSessionCredentialsProvider.Builder(Config.get(Config.Key.IAM_ROLE_ARN), IAM_SESSION_NAME).withStsClient(local).build();
        } else {
            this.credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }
        this.s3 = AmazonS3ClientBuilder.standard().withCredentials(this.credentialsProvider).build();
    }

    protected final void createBucket(final String name, final String queueArn) {
        this.s3.createBucket(new CreateBucketRequest(name, Region.fromValue(this.getRegion())));
        this.s3.setBucketNotificationConfiguration(name, new BucketNotificationConfiguration("test", new QueueConfiguration(queueArn, EnumSet.of(S3Event.ObjectCreated))));
    }

    protected final void createObject(final String bucketName, final String key, final String content) {
        this.s3.putObject(bucketName, key, content);
    }

    protected final void createObject(final String bucketName, final String key, final InputStream content, final String contentType, final long contentLength) {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        this.s3.putObject(bucketName, key, content, metadata);
    }

    protected final boolean doesObjectExist(final String bucketName, final String key) {
        return this.s3.doesObjectExist(bucketName, key);
    }

    protected final List<Tag> getObjectTags(final String bucketName, final String key) {
        return this.s3.getObjectTagging(new GetObjectTaggingRequest(bucketName, key)).getTagSet();
    }

    protected final void deleteObject(final String bucketName, final String key) {
        if (Config.get(Config.Key.DELETION_POLICY).equals("delete")) {
            this.s3.deleteObject(bucketName, key);
        }
    }

    private void emptyBucket(final String name) {
        ObjectListing objectListing = s3.listObjects(name);
        while (true) {
            objectListing.getObjectSummaries().forEach((summary) -> s3.deleteObject(name, summary.getKey()));
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        VersionListing versionListing = s3.listVersions(new ListVersionsRequest().withBucketName(name));
        while (true) {
            versionListing.getVersionSummaries().forEach((vs) -> s3.deleteVersion(name, vs.getKey(), vs.getVersionId()));
            if (versionListing.isTruncated()) {
                versionListing = s3.listNextBatchOfVersions(versionListing);
            } else {
                break;
            }
        }
    }

    protected long countBucket(final String name) {
        long count = 0;
        ObjectListing objectListing = s3.listObjects(name);
        while (true) {
            count += objectListing.getObjectSummaries().size();
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        return count;
    }

    protected final void deleteBucket(final String name) {
        if (Config.get(Config.Key.DELETION_POLICY).equals("delete")) {
            this.emptyBucket(name);
            this.s3.deleteBucket(new DeleteBucketRequest(name));
        }
    }

    protected final String getRegion() {
        return new DefaultAwsRegionProviderChain().getRegion();
    }

    protected final String random8String() {
        final String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        final int beginIndex = (int) (Math.random() * (uuid.length() - 7));
        final int endIndex = beginIndex + 7;
        return "r" + uuid.substring(beginIndex, endIndex); // must begin [a-z]
    }

}
