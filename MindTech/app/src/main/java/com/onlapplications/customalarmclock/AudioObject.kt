package com.onlapplications.customalarmclock

import com.google.firebase.storage.FirebaseStorage
import java.io.File

// The audio object that is saved in the database.  References  to an object in the FirebaseStorage.
class AudioObject (var name:String = "", var storageId: String = "", var pathInPhone: String = "") {

    fun downloadFile() {
        val storageRef = FirebaseStorage.getInstance().getReference("audioFiles")

        val localFile = File.createTempFile(storageId, "mp3")

        storageRef.getFile(localFile).addOnSuccessListener {
            // If the download managed to start
            pathInPhone = localFile.path

        }.addOnFailureListener {
            // Handle any errors
        }

    }
}
