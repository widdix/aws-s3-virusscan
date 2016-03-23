# S3 VirusScan

Antivirus for S3 buckets. You can connect as many buckets as you like by using [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html)

## Features

* Uses ClamAV to scan newly added files on S3 buckets
* Automatically updates ClamAV db every 2 hours
* Scale EC2 instance workers to distribute workload

## Installation

Create a CloudFormation stack based on `template.json`.

Configure the buckets you want to connect to S3 VirusScan as shown in the next figure:

![Configure Event Notifications](./docs/configure_event_notifications.png?raw=true "Configure Event Notifications")

**Make sure you select the *-ScanQueue-* NOT the *-ScanQueueDLQ-*!**

If you like to receive emails if a virus was found you must subscribe to the SNS topic as sown in the next two figures:

![Subscribe Topic: Step 1](./docs/subscribe_topic_step1.png?raw=true "Subscribe Topic: Step 1")

![Subscribe Topic: Step 2](./docs/subscribe_topic_step2.png?raw=true "Subscribe Topic: Step 2")

You will receive a confirmation email.

## Test

Create a [EICAR Standard Anti-Virus Test File](https://en.wikipedia.org/wiki/EICAR_test_file) with the following content:

```
X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
```

and upload that file to your S3 bucket.

## TODO

* Support versioned S3 buckets
* An initial scan may also be useful. This could be implemented with a Lambda function that pushes every key to SQS.
