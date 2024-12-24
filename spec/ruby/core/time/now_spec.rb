require_relative '../../spec_helper'
require_relative 'shared/now'

describe "Time.now" do
  it_behaves_like :time_now, :now

  ruby_version_is '3.1' do # https://bugs.ruby-lang.org/issues/17485
    describe ":in keyword argument" do
      it "could be UTC offset as a String in '+HH:MM or '-HH:MM' format" do
        time = Time.now(in: "+05:00")

        time.utc_offset.should == 5*60*60
        time.zone.should == nil

        time = Time.now(in: "-09:00")

        time.utc_offset.should == -9*60*60
        time.zone.should == nil
      end

      it "could be UTC offset as a number of seconds" do
        time = Time.now(in: 5*60*60)

        time.utc_offset.should == 5*60*60
        time.zone.should == nil

        time = Time.now(in: -9*60*60)

        time.utc_offset.should == -9*60*60
        time.zone.should == nil
      end

      it "returns a Time with UTC offset specified as a single letter military timezone" do
        Time.now(in: "W").utc_offset.should == 3600 * -10
      end

      it "could be a timezone object" do
        zone = TimeSpecs::TimezoneWithName.new(name: "Asia/Colombo")
        time = Time.now(in: zone)

        time.utc_offset.should == 5*3600+30*60
        time.zone.should == zone

        zone = TimeSpecs::TimezoneWithName.new(name: "PST")
        time = Time.now(in: zone)

        time.utc_offset.should == -9*60*60
        time.zone.should == zone
      end

      it "raises ArgumentError if format is invalid" do
        -> { Time.now(in: "+09:99") }.should raise_error(ArgumentError)
        -> { Time.now(in: "ABC") }.should raise_error(ArgumentError)
      end
    end
  end

  ruby_version_is '3.1' do # https://bugs.ruby-lang.org/issues/17485
    describe "Timezone object" do
      it "raises TypeError if timezone does not implement #utc_to_local method" do
        zone = Object.new
        def zone.local_to_utc(time)
          time
        end

        -> {
          Time.now(in: zone)
        }.should raise_error(TypeError, /can't convert Object into an exact number/)
      end

      it "does not raise exception if timezone does not implement #local_to_utc method" do
        zone = Object.new
        def zone.utc_to_local(time)
          time
        end

        Time.now(in: zone).should be_kind_of(Time)
      end

      # The result also should be a Time or Time-like object (not necessary to be the same class)
      # or Integer. The zone of the result is just ignored.
      describe "returned value by #utc_to_local and #local_to_utc methods" do
        it "could be Time instance" do
          zone = Object.new
          def zone.utc_to_local(t)
            Time.new(t.year, t.mon, t.day, t.hour + 1, t.min, t.sec, t.utc_offset)
          end

          Time.now(in: zone).should be_kind_of(Time)
          Time.now(in: zone).utc_offset.should == 3600
        end

        it "could be Time subclass instance" do
          zone = Object.new
          def zone.utc_to_local(t)
            Class.new(Time).new(t.year, t.mon, t.day, t.hour + 1, t.min, t.sec, t.utc_offset)
          end

          Time.now(in: zone).should be_kind_of(Time)
          Time.now(in: zone).utc_offset.should == 3600
        end

        it "could be Integer" do
          zone = Object.new
          def zone.utc_to_local(time)
            time.to_i + 60*60
          end

          Time.now(in: zone).should be_kind_of(Time)
          Time.now(in: zone).utc_offset.should == 60*60
        end

        it "could have any #zone and #utc_offset because they are ignored" do
          zone = Object.new
          def zone.utc_to_local(t)
            Struct.new(:year, :mon, :mday, :hour, :min, :sec, :isdst, :to_i, :zone, :utc_offset)
                  .new(t.year, t.mon, t.mday, t.hour, t.min, t.sec, t.isdst, t.to_i, 'America/New_York', -5*60*60)
          end
          Time.now(in: zone).utc_offset.should == 0

          zone = Object.new
          def zone.utc_to_local(t)
            Struct.new(:year, :mon, :mday, :hour, :min, :sec, :isdst, :to_i, :zone, :utc_offset)
                  .new(t.year, t.mon, t.mday, t.hour, t.min, t.sec, t.isdst, t.to_i, 'Asia/Tokyo', 9*60*60)
          end
          Time.now(in: zone).utc_offset.should == 0
        end

        it "raises ArgumentError if difference between argument and result is too large" do
          zone = Object.new
          def zone.utc_to_local(t)
            Time.utc(t.year, t.mon, t.day - 1, t.hour, t.min, t.sec, t.utc_offset)
          end

          -> {
            Time.now(in: zone)
          }.should raise_error(ArgumentError, "utc_offset out of range")
        end
      end
    end
  end
end
