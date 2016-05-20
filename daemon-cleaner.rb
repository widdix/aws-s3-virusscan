#!/usr/bin/env ruby

require 'daemons'

Daemons.run(__dir__ + '/cleaner.rb', {:monitor => true})
