# S3 VirusScan

Antivirus for S3 buckets. You can connect as many buckets as you like by using [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html)

## Features

* Uses ClamAV to scan newly added files on S3 buckets
* Updates ClamAV database every 3 hours automatically
* Scales EC2 instance workers to distribute workload
* Publishes a message to SNS in case of a finding
* Can optionally delete compromised files automatically
* Logs to CloudWatch Logs

## How does it work

A picture is worth a thousand words:

![Architecture](./docs/architecture.png?raw=true "Architecture")

1. S3 VirusScan uses a SQS queue to decouple scan jobs from the ClamAV workers. Each S3 bucket can fire events to that SQS queue in case of new objects. This feature of S3 is called [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html).
1. The SQS queue is consumed by a fleet of EC2 instances running in an Auto Scaling Group. If the number of outstanding scan jobs reaches a threshold a new ClamAV worker is automatically added. If the queue is mostly empty workers are removed.
1. The ClamAV workers run a simple ruby script that executes the [clamscan](http://linux.die.net/man/1/clamscan) command. In the background the virus db is updated every three hours.
1. If `clamscan` finds a virus the file is directly deleted (you can configure that) and a SNS notification is published.

## Installation

### Create the CloudFormation Stack
1. This templates depends on our [`vpc-*azs.yaml`](https://github.com/widdix/aws-cf-templates/tree/master/vpc) template. The scanners will will use 2 AZs only. <a href="https://console.aws.amazon.com/cloudformation/home#/stacks/new?stackName=vpc-2azs&templateURL=https://s3-eu-west-1.amazonaws.com/widdix-aws-cf-templates/vpc/vpc-2azs.yaml">Launch Stack</a>
1. <a href="https://console.aws.amazon.com/cloudformation/home#/stacks/new?stackName=s3-virusscan&templateURL=https://s3-eu-west-1.amazonaws.com/widdix-aws-s3-virusscan/template.yaml">Launch Stack</a>
1. Click **Next** to proceed with the next step of the wizard.
1. Specify a name and all parameters for the stack.
1. Click **Next** to proceed with the next step of the wizard.
1. Click **Next** to skip the **Options** step of the wizard.
1. Check the **I acknowledge that this template might cause AWS CloudFormation to create IAM resources.** checkbox.
1. Click **Create** to start the creation of the stack.
1. Wait until the stack reaches the state **CREATE_COMPLETE**

### Configure the buckets
Configure the buckets you want to connect to S3 VirusScan as shown in the next figure:

![Configure Event Notifications](./docs/configure_event_notifications.png?raw=true "Configure Event Notifications")

**Make sure you select the *-ScanQueue-* NOT the *-ScanQueueDLQ-*!**

### Configure E-Mail subscription
If you like to receive emails if a virus was found you must subscribe to the SNS topic as shown in the next two figures:

![Subscribe Topic: Step 1](./docs/subscribe_topic_step1.png?raw=true "Subscribe Topic: Step 1")

![Subscribe Topic: Step 2](./docs/subscribe_topic_step2.png?raw=true "Subscribe Topic: Step 2")

You will receive a confirmation email.

## Test

Create a [EICAR Standard Anti-Virus Test File](https://en.wikipedia.org/wiki/EICAR_test_file) with the following content:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

and upload that file to your S3 bucket.

## Known issues / limitations

* It was [reported](https://github.com/widdix/aws-s3-virusscan/issues/12) that the solution does not run on a t2.micro or smaller. Use at least a t2.small instance.
* Versioned buckets are not supported
* An initial scan may also be useful but is not performed at the moment. This could be implemented with a Lambda function that pushes every key to SQS.
