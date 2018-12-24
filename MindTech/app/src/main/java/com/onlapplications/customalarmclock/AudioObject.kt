package com.onlapplications.customalarmclock

import android.content.Context
import android.media.MediaMetadataRetriever
import com.google.firebase.storage.FirebaseStorage
import org.jetbrains.anko.progressDialog
import java.io.File

// The audio object that is saved in the database.  References  to an object in the FirebaseStorage.
class AudioObject(var firebaseId: String = "", var name: String = "", var pathInPhone: String = "") {

    // downloads the file
    fun downloadFile(context: Context, onFinished: (Boolean) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().getReference("audioFiles")
        val localFile = File.createTempFile(firebaseId, "mp3")


        val prog = context.progressDialog(message = "אנא המתן...", title = "מוריד קובץ")
        prog.setCancelable(false)
        prog.show()

        storageRef.child(firebaseId).getFile(localFile).addOnProgressListener {
            // Whenever there is progress
            val progress = 100.0 * it.bytesTransferred / it.totalByteCount
            prog.progress = progress.toInt()
        }.addOnFailureListener {
            // If the download failed
            prog.dismiss()
            it.printStackTrace()
            onFinished(false)
        }.addOnSuccessListener {
            // If the download succeeded
            prog.dismiss()
            this.pathInPhone = localFile.path
            onFinished(true)
        }
    }

    // downloads the file
    fun downloadFileNoDialog() {
        val storageRef = FirebaseStorage.getInstance().getReference("audioFiles")
        val localFile = File.createTempFile(firebaseId, "mp3")

        storageRef.child(firebaseId).getFile(localFile).addOnFailureListener {
            // If the download failed
            it.printStackTrace()
        }.addOnSuccessListener {
            // If the download succeeded
            this.pathInPhone = localFile.path
        }
    }



    // Receives a uri, returns the length of the object in it
    fun getDurationInMillis(): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(this.pathInPhone)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durationStr.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
