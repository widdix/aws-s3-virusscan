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

log.info "s3-virusscan started"

poller.poll do |msg|
  body = JSON.parse(msg.body)
  body['Records'].each do |record|
    key = URI.decode(record['s3']['object']['key']).gsub('+', ' ')
    log.debug "scanning #{key}..."
    s3.get_object(
      response_target: '/tmp/target',
      bucket: record['s3']['bucket']['name'],
      key: key
    )
    if system('clamscan /tmp/target')
      log.debug "#{key} was scanned without findings"
    else
      log.error "#{key} is infected, deleting..."
      s3.delete_object(
        bucket: record['s3']['bucket']['name'],
        key: key
      )
      log.error "#{key} was deleted"
    end
    system('rm /tmp/target')
  end
end
