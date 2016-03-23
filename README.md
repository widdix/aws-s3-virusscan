# S3 VirusScan

Antivirus for S3 buckets. You can connect as many buckets as you like by using [S3 Event Notifications](http://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html)

## Features

* Uses ClamAV to scan newly added files on S3 buckets
* Automatically updates ClamAV db every 2 hours
* Scale EC2 instance workers to distribute workload

## Installation

1. Create a CloudFormation stack based on `template.json`
1. 

## Configuration

connect Create Events of the S3 buckets to be scanned with the SQS queue from the stack

## TODO

* Notify SNS topic about findings
* Support versioned S3 buckets
