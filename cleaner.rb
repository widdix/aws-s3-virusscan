#!/usr/bin/env ruby

require 'aws-sdk'
require 'json'
require 'uri'
require 'yaml'
require 'syslog/logger'

log = Syslog::Logger.new 's3-virusscan-cleaner'
conf = YAML::load_file(__dir__ + '/s3-virusscan.conf')

Aws.config.update(region: conf['region'])
s3 = Aws::S3::Client.new()
sns = Aws::SNS::Client.new()

poller = Aws::SQS::QueuePoller.new(conf['dlq'])

log.info "s3-virusscan-cleaner started"

poller.poll do |msg|
  body = JSON.parse(msg.body)
  if body.key?('Records')
    body['Records'].each do |record|
      bucket = record['s3']['bucket']['name']
      key = URI.decode(record['s3']['object']['key']).gsub('+', ' ')
      begin
        s3.get_object(
            response_target: '/tmp/target',
            bucket: bucket,
            key: key
        )
      rescue Aws::S3::Errors::NoSuchKey
        log.debug "no key found s3://#{bucket}/#{key}"
        next
      end
      sns.publish(
          topic_arn: conf['topic'],
          message: "s3://#{bucket}/#{key} please recheck this object",
          subject: "s3-virusscan s3://#{bucket}"
      )
    end
  end
end

