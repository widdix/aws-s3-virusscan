#!/usr/bin/env ruby

require 'aws-sdk'
require 'json'
require 'uri'
require 'yaml'
require 'syslog/logger'
require 'securerandom'

class ClamScanWorker

  attr_reader :log, :conf, :sns

  CLEAN_STATUS = 'clean'
  INFECTED_STATUS = 'infected'

  def initialize
    @log = Syslog::Logger.new 's3-virusscan'
    @conf = YAML::load_file(__dir__ + '/s3-virusscan.conf')
    Aws.config.update(region: conf['region'])
    @sns = Aws::SNS::Client.new()
  end

  def perform
    log.info "s3-virusscan started"

    s3 = Aws::S3::Client.new()
    poller = Aws::SQS::QueuePoller.new(conf['queue'])

    poller.poll do |msg|
      body = JSON.parse(msg.body)
      if body.key?('Records')
        body['Records'].each do |record|
          bucket = record['s3']['bucket']['name']
          key = URI.decode(record['s3']['object']['key']).gsub('+', ' ')
          Thread.new {
            fileName = "/tmp/#{SecureRandom.uuid}"
            log.debug "scanning s3://#{bucket}/#{key}..."
            begin
              s3.get_object(
                response_target: fileName,
                bucket: bucket,
                key: key
              )
            rescue Aws::S3::Errors::NoSuchKey
              log.debug "s3://#{bucket}/#{key} does no longer exist"
              next
            end

            if system("clamdscan #{fileName}")
              if conf['report_clean']
                publish_notification(bucket, key, CLEAN_STATUS);
              else
                log.debug "s3://#{bucket}/#{key} is clean"
              end
            else
              publish_notification(bucket, key, INFECTED_STATUS);
              if conf['delete']
                s3.delete_object(
                  bucket: bucket,
                  key: key
                )
                log.debug "s3://#{bucket}/#{key} was deleted"
              end
            end
            system("rm #{fileName}")
          }
        end
      end
    end
  end

  private

  def publish_notification(bucket, key, status)
    msg = "s3://#{bucket}/#{key} is #{status}"
    if conf['delete']
      msg << ", deleting..."
    end
    log.debug msg
    sns.publish(
      topic_arn: conf['topic'],
      message: msg,
      subject: "s3-virusscan s3://#{bucket}",
      message_attributes: {
        "key" => {
          data_type: "String",
          string_value: key
        },
        "status" => {
          data_type: "String",
          string_value: status
        }
      }
    )
  end

end

# do some work
ClamScanWorker.new.perform
