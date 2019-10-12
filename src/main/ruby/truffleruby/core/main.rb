# frozen_string_literal: true

# Copyright (c) 2014, 2019 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 2.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Methods on `main`, the top-level `self`
class << self
  def include(*mods)
    Truffle.privately do
      Object.include(*mods)
    end
  end

  def define_method(*args, &block)
    Object.define_method(*args, &block)
  end

  def to_s
    'main'
  end

  alias_method :inspect, :to_s
end

show_backtraces = -> {
  puts 'Threads and backtraces:'
  Thread.list.each do |thread|
    $stderr.puts thread
    if thread == Thread.current
      # Ignore the signal handler frames
      $stderr.puts thread.backtrace[6..-1]
    else
      $stderr.puts thread.backtrace
    end
    $stderr.puts
  end
}

Truffle::Boot.delay do
  # Use vm_watch_signal directly as those should be the 'DEFAULT' handlers

  if Truffle::Boot.get_option('platform-handle-interrupt')
    TrufflePrimitive.vm_watch_signal 'INT', -> do
      if Truffle::Boot.get_option('backtraces-on-interrupt')
        puts 'Interrupting...'
        show_backtraces.call
      end

      raise Interrupt
    end
  end

  if Truffle::Boot.get_option('backtraces-sigalrm')
    TrufflePrimitive.vm_watch_signal 'ALRM', -> do
      show_backtraces.call
    end
  end
end
