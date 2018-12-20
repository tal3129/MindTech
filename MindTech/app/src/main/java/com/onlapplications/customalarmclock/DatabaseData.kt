package com.onlapplications.customalarmclock

import com.google.gson.Gson

class DatabaseData (val audioObjects: ArrayList<AudioObject> = ArrayList(), var dataVersion: Int = -1) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun copy(value: DatabaseData?) {
    }

    // downloads all audio files it the audioObject list
    fun downloadAudioFiles() {
        audioObjects.forEach {
            it.downloadFile()
        }
    }
}