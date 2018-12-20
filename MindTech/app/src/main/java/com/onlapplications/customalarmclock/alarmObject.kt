package com.onlapplications.customalarmclock

import com.google.gson.Gson

// Describes an alarm object, that has a set time, an audio object and a number of intervals

class AlarmObject (var audioObject: AudioObject? = null, var repetitions: Int = 1, var timeInMillis: Long = -1) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}