# Tests

The goal of this tests is to ensure that our templates are always working. The test are implemented in Java 8 and run in JUnit 4.

If you run this tests, many AWS CloudFormation tests are created and **charges will apply**!

[widdix GmbH](https://widdix.net) sponsors the test runs on every push and once per week to ensure that everything is working as expected.

## Supported env variables

* `IAM_ROLE_ARN` if the tests should assume an IAM role before they run supply the ARN of the IAM role
* `TEMPLATE_DIR` Load templates from local disk (instead of S3 bucket `widdix-aws-cf-templates`). Must end with an `/`. See `BUCKET_NAME` as well.
* `DELETION_POLICY` (default `delete`, allowed values [`delete`, `retain`]) should resources be deleted?
* `INFECTED_FILES_BUCKET_NAME` S3 bucket name with infected files (all objects must be infected!). Ignored if not set.
* `INFECTED_FILES_BUCKET_REGION` **required if INFECTED_FILES_BUCKET_NAME is set** Region of the bucket.

## Usage

### AWS credentials

The AWS credentials are passed in as defined by the AWS SDK for Java: http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html

One addition: you can supply the env variable `IAM_ROLE_ARN` which let's the tests assume a role with the default credentials before running the tests.

### Region selection

The region selection works like defined by the AWS SDK for Java: http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html

### Run all tests

```
AWS_REGION="us-east-1" mvn test
```

### Run a single test suite

to run the `TestJenkins` tests:

```
AWS_REGION="us-east-1" mvn -Dtest=TestS3VirusScan test
```

### Run a single test

to run the `TestS3VirusScan.test` test:

```
AWS_REGION="us-east-1" mvn -Dtest=TestS3VirusScan#testWithoutFileDeletion test
```

### Load templates from local file system

```
AWS_REGION="us-east-1" TEMPLATE_DIR="/path/to/widdix-aws-s3-virusscan/" mvn test
```

### Assume role

This is useful if you run on a integration server like Jenkins and want to assume a different IAM role for this tests.

```
IAM_ROLE_ARN="arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME" mvn test
```
