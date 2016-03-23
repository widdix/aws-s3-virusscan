#!/usr/bin/env ruby

require 'daemons'

Daemons.run('worker.rb')
