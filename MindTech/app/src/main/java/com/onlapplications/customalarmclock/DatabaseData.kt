package com.onlapplications.customalarmclock

import com.google.gson.Gson

class DatabaseData (val audioObjects: ArrayList<AudioObject> = ArrayList(), var appSettings: AppSettings = AppSettings()) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    // Downloads ALL audio files it the audioObject list - may take time
    fun downloadAllAudioFiles() {
        audioObjects.forEach {
            it.downloadFileNoDialog()
        }
    }
}