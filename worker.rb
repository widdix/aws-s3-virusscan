#!/usr/bin/env ruby

require 'aws-sdk'
require 'json'
require 'uri'
require 'yaml'
require 'syslog/logger'

log = Syslog::Logger.new 's3-virusscan'
conf = YAML::load_file(__dir__ + '/s3-virusscan.conf')

Aws.config.update(region: conf['region'])
s3 = Aws::S3::Client.new()

poller = Aws::SQS::QueuePoller.new(conf['queue'])

clean_status = 'clean';
infected_status = 'infected';

log.info "s3-virusscan started"

def publish_notification(msg,status)
  log.info msg
  sns = Aws::SNS::Client.new()
  sns.publish(
    topic_arn: conf['topic'],
    message: msg,
    subject: "s3-virusscan s3://#{bucket}",
    message_attributes: {
      "key" => {
        data_type: "String",
        string_value: "s3://#{bucket}/#{key}"
      },
      "status" => {
        data_type: "String",
        string_value: status
      }
    }
  )
end

poller.poll do |msg|
  body = JSON.parse(msg.body)
  if body.key?('Records')
    body['Records'].each do |record|
      bucket = record['s3']['bucket']['name']
      key = URI.decode(record['s3']['object']['key']).gsub('+', ' ')
      log.debug "scanning s3://#{bucket}/#{key}..."
      begin
        s3.get_object(
          response_target: '/tmp/target',
          bucket: bucket,
          key: key
        )
      rescue Aws::S3::Errors::NoSuchKey
        log.error "s3://#{bucket}/#{key} does no longer exist"
        next
      end
      if system('clamscan /tmp/target')
        if conf['reportClean']
          publish_notification("s3://#{bucket}/#{key} is clean",clean_status);
        else
          # log only, no notification
          log.info "s3://#{bucket}/#{key} is clean"
        end
      else
        if conf['delete']
          publish_notification("s3://#{bucket}/#{key} is infected, deleting...",infected_status);
          s3.delete_object(
            bucket: bucket,
            key: key
          )
          log.info "s3://#{bucket}/#{key} was deleted"
        else
          publish_notification("s3://#{bucket}/#{key} is infected",infected_status);
        end
      end
      system('rm /tmp/target')
    end
  end
end

