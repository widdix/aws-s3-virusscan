# S3 VirusScan

Antivirus for S3 buckets. You can connect as many buckets as you like by using [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html)

> The [S3 VirusScan](https://s3-virusscan.widdix.net/) with additional integrations is available in the [AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07XFR781T).

## Features

* Uses ClamAV to scan newly added files on S3 buckets
* Updates ClamAV database every 3 hours automatically
* Scales EC2 instance workers to distribute workload
* Publishes a message to SNS in case of a finding
* Can optionally delete compromised files automatically
* Logs to CloudWatch Logs

## Commercial Features

* CloudWatch Integration (Metrics and Dashboard)
* Security Hub Integration
* SSM OpsCenter Integration

The [S3 VirusScan](https://s3-virusscan.widdix.net/) with additional integrations is available in the [AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07XFR781T).

## How does it work

A picture is worth a thousand words:

![Architecture](./docs/architecture.png?raw=true "Architecture")

1. S3 VirusScan uses a SQS queue to decouple scan jobs from the ClamAV workers. Each S3 bucket can fire events to that SQS queue in case of new objects. This feature of S3 is called [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html).
1. The SQS queue is consumed by a fleet of EC2 instances running in an Auto Scaling Group. If the number of outstanding scan jobs reaches a threshold a new ClamAV worker is automatically added. If the queue is mostly empty workers are removed.
1. The ClamAV workers run a simple ruby script that executes the [clamscan](http://linux.die.net/man/1/clamscan) command. In the background the virus db is updated every three hours.
1. If `clamscan` finds a virus the file is directly deleted (you can configure that) and a SNS notification is published.

## Installation

### Create the CloudFormation Stack
1. This templates depends on one of our [`vpc-*azs.yaml`](https://templates.cloudonaut.io/en/stable/vpc/) templates. [Launch Stack](https://console.aws.amazon.com/cloudformation/home#/stacks/create/review?templateURL=https://s3-eu-west-1.amazonaws.com/widdix-aws-cf-templates-releases-eu-west-1/stable/vpc/vpc-2azs.yaml&stackName=vpc)
1. [Launch Stack](https://console.aws.amazon.com/cloudformation/home#/stacks/create/review?templateURL=https://s3-eu-west-1.amazonaws.com/widdix-aws-s3-virusscan/template.yaml&stackName=s3-virusscan&param_ParentVPCStack=vpc)
1. Click **Next** to proceed with the next step of the wizard.
1. Specify a name and all parameters for the stack.
1. Click **Next** to proceed with the next step of the wizard.
1. Click **Next** to skip the **Options** step of the wizard.
1. Check the **I acknowledge that this template might cause AWS CloudFormation to create IAM resources.** checkbox.
1. Click **Create** to start the creation of the stack.
1. Wait until the stack reaches the state **CREATE_COMPLETE**

### Configure the buckets
Configure the buckets you want to connect to S3 VirusScan as shown in the next figure:

![Configure Event Notifications 1](./docs/configure_event_notifications1.png?raw=true "Configure Event Notifications 1")

![Configure Event Notifications 2](./docs/configure_event_notifications2.png?raw=true "Configure Event Notifications 2")

**Make sure you select the *-ScanQueue-* NOT the *-ScanQueueDLQ-*!**

### Configure E-Mail subscription
If you like to receive emails if a virus was found you must subscribe to the SNS topic as shown in the next two figures:

![Subscribe Topic: Step 1](./docs/subscribe_topic_step1.png?raw=true "Subscribe Topic: Step 1")

![Subscribe Topic: Step 2](./docs/subscribe_topic_step2.png?raw=true "Subscribe Topic: Step 2")

You will receive a confirmation email.

> The [S3 VirusScan](https://s3-virusscan.widdix.net/) with additional integrations is available in the [AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07XFR781T).

## Test

### Extensive test

Thanks to [Objective-See](https://objective-see.com/) for providing infected files that we use for testing. Download one of the files upload it to your S3 bucket for testing.
We also have automated tests in place!

### Simple test

Create a [EICAR Standard Anti-Virus Test File](https://en.wikipedia.org/wiki/EICAR_test_file) with the following content:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

and upload that file to your S3 bucket.

## Troubleshooting

1. Go to [CloudWatch Logs in the AWS Management Console](https://console.aws.amazon.com/cloudwatch/home#logs:)
2. Click on the log group of the s3-virusscan
3. Click on the blue **Search Log Group** button
4. Search for `"s3-virusscan["`

## Known issues / limitations

* It was [reported](https://github.com/widdix/aws-s3-virusscan/issues/12) that the solution does not run on a t2.micro or smaller. Use at least a t2.small instance.
* An initial scan may also be useful but is not performed at the moment. This could be implemented with a Lambda function that pushes every key to SQS.
