package com.onlapplications.customalarmclock

import com.google.gson.Gson

class DatabaseData (val audioObjects: ArrayList<AudioObject> = ArrayList(), var dataVersion: Int = -1) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    // downloads all audio files it the audioObject list
    fun downloadAudioFiles(onItemDownloaded : () -> Unit) {
        audioObjects.forEach {
            it.downloadFile(onItemDownloaded)
        }
    }
}