package com.onlapplications.customalarmclock

import android.media.MediaMetadataRetriever
import com.google.firebase.storage.FirebaseStorage
import java.io.File

// The audio object that is saved in the database.  References  to an object in the FirebaseStorage.
class AudioObject (var firebaseId: String = "", var name:String = "", var pathInPhone: String = "") {

    // downloads the file
    fun downloadFile(onItemDownload: () -> Unit) {
        val storageRef = FirebaseStorage.getInstance().getReference("audioFiles")

        val localFile = File.createTempFile(firebaseId, "mp3")

        storageRef.child("$firebaseId.mp3").getFile(localFile).addOnSuccessListener {
            // If the download managed to start
            pathInPhone = localFile.path

        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener({
            onItemDownload()
        })
    }


    // Receives a uri, returns the length of the object in it
    fun getDurationInMillis() :Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(this.pathInPhone)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durationStr.toLong()
        }
        catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
