#!/usr/bin/env ruby

require 'aws-sdk'
require 'json'
require 'uri'
require 'yaml'

conf = YAML::load_file('s3-virusscan.conf')

Aws.config.update(region: conf['region'])
s3 = Aws::S3::Client.new()
poller = Aws::SQS::QueuePoller.new(conf['queue'])

poller.poll do |msg|
  body = JSON.parse(msg.body)
  body['Records'].each do |record|
    key = URI.decode(record['s3']['object']['key']).gsub('+', ' ')
    s3.get_object(
      response_target: '/tmp/target',
      bucket: record['s3']['bucket']['name'],
      key: key
    )
    if system('clamscan /tmp/target')
      puts "file #{key} is save"
    else
      puts "file #{key} is infected, deleting"
      s3.delete_object(
        bucket: record['s3']['bucket']['name'],
        key: key
      )
      puts "file #{key} was deleted"
    end
    system('rm /tmp/target')
  end
end
