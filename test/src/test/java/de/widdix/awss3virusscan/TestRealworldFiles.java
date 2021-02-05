package de.widdix.awss3virusscan;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.s3.model.Tag;
import org.junit.Test;

import java.util.List;

public class TestRealworldFiles extends AVirusTest {

    @Test
    public void test() {
        final Context context = new Context();
        final String vpcStackName = "vpc-2azs-" + this.random8String();
        final String stackName = "s3-virusscan-" + this.random8String();
        final String bucketName = "s3-virusscan-" + this.random8String();
        final InfectedFileCache cache = new InfectedFileCache();
        try {
            this.createWiddixStack(context, vpcStackName, "vpc/vpc-2azs.yaml");
            try {
                this.createStack(context, stackName,
                        "template.yaml",
                        new Parameter().withParameterKey("ParentVPCStack").withParameterValue(vpcStackName)
                );
                try {
                    this.createBucketWithSQSNotification(bucketName, this.getStackOutputValue(stackName, "ScanQueueArn"));
                    cache.getFiles().forEach(file -> this.createObject(bucketName, file.getkey(), file.getContent(), file.getContentType(), file.getContentLength()));
                    this.retry(context, () -> {
                        final long count = this.countBucket(bucketName);
                        if (count != 0) { // all files are expected to be deleted
                            throw new RuntimeException("there are " + count + " infected files left");
                        }
                        return false;
                    });
                } catch (final RuntimeException e) {
                    final List<String> objects = this.listBucket(bucketName, 100);
                    System.out.println("Remaining objects:");
                    for (final String object : objects) {
                        System.out.println(object);
                    }
                    throw e;
                } finally {
                    this.deleteBucket(context, bucketName);
                }
            } finally {
                this.deleteStack(context, stackName);
            }
        } finally {
            this.deleteStack(context, vpcStackName);
        }
    }
}
