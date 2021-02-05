package de.widdix.awss3virusscan;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.s3.model.Tag;
import org.junit.Test;

import java.util.List;

public class TestEicarFile extends AVirusTest {

    @Test
    public void testWithoutFileDeletion() {
        final Context context = new Context();
        final String vpcStackName = "vpc-2azs-" + this.random8String();
        final String stackName = "s3-virusscan-" + this.random8String();
        final String bucketName = "s3-virusscan-" + this.random8String();
        try {
            this.createWiddixStack(context, vpcStackName, "vpc/vpc-2azs.yaml");
            try {
                this.createStack(context, stackName,
                        "template.yaml",
                        new Parameter().withParameterKey("ParentVPCStack").withParameterValue(vpcStackName),
                        new Parameter().withParameterKey("TagFiles").withParameterValue("true"),
                        new Parameter().withParameterKey("DeleteInfectedFiles").withParameterValue("false")
                );
                try {
                    this.createBucketWithSQSNotification(bucketName, this.getStackOutputValue(stackName, "ScanQueueArn"));
                    this.createObject(bucketName, "no-virus.txt", "not a virus");
                    this.createObject(bucketName, "virus.txt", "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*");
                    this.retry(context, () -> {
                        final List<Tag> tags = this.getObjectTags(bucketName, "no-virus.txt");
                        if (tags.size() == 1) {
                            final Tag tag = tags.get(0);
                            if ("clamav-status".equals(tag.getKey())) {
                                if ("clean".equals(tag.getValue())) {
                                    return tags;
                                } else {
                                    throw new RuntimeException("clamav-status tag value expected to be clean, but saw " + tag.getValue());
                                }
                            } else {
                                throw new RuntimeException("one and only tag key expected to be clamav-status, but saw " + tag.getKey());
                            }
                        } else {
                            throw new RuntimeException("one tag expected, but saw " + tags.size());
                        }
                    });
                    this.retry(context, () -> {
                        final List<Tag> tags = this.getObjectTags(bucketName, "virus.txt");
                        if (tags.size() == 1) {
                            final Tag tag = tags.get(0);
                            if ("clamav-status".equals(tag.getKey())) {
                                if ("infected".equals(tag.getValue())) {
                                    return tags;
                                } else {
                                    throw new RuntimeException("clamav-status tag value expected to be infected, but saw " + tag.getValue());
                                }
                            } else {
                                throw new RuntimeException("one and only tag key expected to be clamav-status, but saw " + tag.getKey());
                            }
                        } else {
                            throw new RuntimeException("one tag expected, but saw " + tags.size());
                        }
                    });
                    this.deleteObject(context, bucketName, "no-virus.txt");
                    this.deleteObject(context, bucketName, "virus.txt");
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

    @Test
    public void testWithFileDeletion() {
        final Context context = new Context();
        final String vpcStackName = "vpc-2azs-" + this.random8String();
        final String stackName = "s3-virusscan-" + this.random8String();
        final String bucketName = "s3-virusscan-" + this.random8String();
        try {
            this.createWiddixStack(context, vpcStackName, "vpc/vpc-2azs.yaml");
            try {
                this.createStack(context, stackName,
                        "template.yaml",
                        new Parameter().withParameterKey("ParentVPCStack").withParameterValue(vpcStackName)
                );
                try {
                    this.createBucketWithSQSNotification(bucketName, this.getStackOutputValue(stackName, "ScanQueueArn"));
                    this.createObject(bucketName, "no-virus.txt", "not a virus");
                    this.createObject(bucketName, "virus.txt", "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*");
                    this.retry(context, () -> {
                        if (this.doesObjectExist(bucketName, "virus.txt") == true) { // expected to be deleted
                            throw new RuntimeException("virus.txt must be deleted");
                        }
                        return false;
                    });
                    this.retry(context, () -> {
                        if (this.doesObjectExist(bucketName, "no-virus.txt") == false) { // expected to exist
                            throw new RuntimeException("no-virus.txt must be existing");
                        }
                        return true;
                    });
                    this.deleteObject(context, bucketName, "no-virus.txt");
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
