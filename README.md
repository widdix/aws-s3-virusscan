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

If you like to receive emails if a virus was found you must subscribe to the SNS topic as sown in the next two figures:

![Subscribe Topic: Step 1](./docs/subscribe_topic_step1.png?raw=true "Subscribe Topic: Step 1")

![Subscribe Topic: Step 2](./docs/subscribe_topic_step2.png?raw=true "Subscribe Topic: Step 2")

You will receive a confirmation email.

## Configuration

connect Create Events of the S3 buckets to be scanned with the SQS queue from the stack

## TODO

* Support versioned S3 buckets
