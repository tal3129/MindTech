package com.onlapplications.customalarmclock

// Describes an alarm object, that has a set time, an audio object and a number of intervals
class alarmObject (var audioObject: AudioObject? = null, var repetitions: Int = 1, var timeInMillis: Long = -1) {

}