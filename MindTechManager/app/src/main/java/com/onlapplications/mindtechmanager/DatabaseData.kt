package com.onlapplications.mindtechmanager

import com.google.gson.Gson

class DatabaseData {
    val audioObjects: HashMap<String, AudioObject> = HashMap()
    var dataVersion: Int = -1

    fun toJson(): String {
        return Gson().toJson(this)
    }

    // downloads all audio files it the audioObject list
    fun downloadAudioFiles(onItemDownloaded: () -> Unit) {
        audioObjects.forEach {
            it.value.downloadFile(onItemDownloaded)
        }
    }
}