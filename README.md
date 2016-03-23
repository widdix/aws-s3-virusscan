# S3 VirusScan

Antivirus for S3 buckets.

## Features

* Uses ClamAV to scan your files on S3
* Automatically updates ClamAV db every 2 hours
* Scale from 1 or 0 to $N EC2 instances to distribute workload

## Installation

create stack based on `template.json`

## Configuration

connect Create Events of the S3 buckets to be scanned with the SQS queue from the stack

## TODO

* Support versioned S3 buckets
