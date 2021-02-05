package de.widdix.awss3virusscan;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.s3.model.*;

import java.util.EnumSet;

public class AVirusTest extends ACloudFormationTest {

    protected final void createWiddixStack(final Context context, final String stackName, final String template, final Parameter... parameters) {
        final CreateStackRequest req = new CreateStackRequest()
                .withStackName(stackName)
                .withParameters(parameters)
                .withCapabilities(Capability.CAPABILITY_IAM)
                .withTemplateURL("https://s3-eu-west-1.amazonaws.com/widdix-aws-cf-templates-releases-eu-west-1/stable/" + template);
        this.cf.createStack(req);
        this.waitForStack(context, stackName, FinalStatus.CREATE_COMPLETE);
    }

    protected final void createBucketWithSQSNotification(final String name, final String queueArn) {
        this.s3.createBucket(new CreateBucketRequest(name, Region.fromValue(this.getRegion())));
        this.s3.setBucketNotificationConfiguration(name, new BucketNotificationConfiguration("test", new QueueConfiguration(queueArn, EnumSet.of(S3Event.ObjectCreated))));
    }
}
