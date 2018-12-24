package com.onlapplications.mindtechmanager

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_audio_obj.*
import org.jetbrains.anko.progressDialog

class MainActivity : AppCompatActivity() {
    companion object {
        const val READ_REQUEST_CODE: Int = 1241
        const val SP_NAME = "appData"
    }

    // has the uri of the last chosen file
    private var lastChosenFile: Uri? = null

    // The audio data from the firebase
    private var data = DatabaseData()

    private var progressed = 0
    private var maxProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializations
        FirebaseApp.initializeApp(this)
        rvAudioFiles.layoutManager = LinearLayoutManager(this)

        btnAddAudioFile.setOnClickListener {
            openUploadItemDialog()
        }

        // Downloading the audio data from the firebase
        downloadDatabaseData()
    }

    private fun openUploadItemDialog() {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_audio_obj)
        dialog.show()
        dialog.setCancelable(true)

        dialog.attachFile.setOnClickListener {
            performFileSearch()
        }

        dialog.btnFinish.setOnClickListener {
            if (lastChosenFile == null || dialog.etName.text.toString().isEmpty()) {
                Toast.makeText(this, "יש לבחור קובץ ושם", Toast.LENGTH_LONG).show()
            } else {
                val newObject = AudioObject()

                var ref = FirebaseDatabase.getInstance().getReference("databaseData")
                var storageRef = FirebaseStorage.getInstance().getReference("audioFiles")

                newObject.name = dialog.etName.text.toString()
                newObject.firebaseId = ref.push().key.toString()

                ref = ref.child("audioObjects").child(newObject.firebaseId)
                storageRef = storageRef.child(newObject.firebaseId)

                ref.setValue(newObject)
                updateDatabaseVersion()

                val prog = progressDialog(message = "אנא המתן, התהליך יכול לקחת זמן עם קבצים גדולים…", title = "מעלה קובץ")
                prog.setCancelable(false)
                prog.show()

                storageRef.putFile(lastChosenFile!!).addOnCompleteListener({
                    dialog.dismiss()
                    prog.dismiss()
                    updateAdapter()
                    Toast.makeText(this,"הקובץ הועלה", Toast.LENGTH_SHORT).show()
                }).addOnProgressListener {
                    val progress = 100.0 * it.bytesTransferred / it.totalByteCount
                    prog.progress = progress.toInt()
                }

                // reset the last chosen file
                lastChosenFile = null
            }
        }
    }


    /**
     * Fires an intent to spin up the "file chooser" UI and select an ausio file.
     */
    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                lastChosenFile = uri
            }
        }
    }

    private fun updateAdapter() {
        rvAudioFiles.adapter = AudioFileAdapter(data.audioObjects, this, onItemLongClick = {
            FirebaseDatabase.getInstance().getReference("databaseData").child("audioObjects").child(it.firebaseId).removeValue()
            FirebaseStorage.getInstance().getReference("audioFiles").child(it.firebaseId).delete()
            updateDatabaseVersion()
            return@AudioFileAdapter false
        })
    }

    // update the version
    private fun updateDatabaseVersion() {
        val ref = FirebaseDatabase.getInstance().getReference("databaseData")
        ref.child("settings").child("dataVersion").setValue(data.appSettings.dataVersion + 1)
    }

    private fun downloadDatabaseData() {
        // First load the data from the device
        data = getDeviceData()
        updateAdapter()

        // Then download the data from the firebase
        val dataRef = FirebaseDatabase.getInstance().getReference("databaseData")
        dataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tempDbData = DatabaseData()
                val audioObjectsSnap = dataSnapshot.child("audioObjects")
                val settingsSnap = dataSnapshot.child("settings")

                tempDbData.appSettings = settingsSnap.getValue(AppSettings::class.java) ?: AppSettings()

                audioObjectsSnap.children.forEach { snap ->
                    val obj = snap.getValue(AudioObject::class.java)
                    if (obj != null)
                        tempDbData.audioObjects.add(obj)
                }

                // Now the database data has been downloaded. check if our device data is up to date.
                // if it isn't, download the audio files again
                if (tempDbData.appSettings.dataVersion > data.appSettings.dataVersion) {
                    data = tempDbData
                    updateAdapter()
                    startProgress()
                    data.downloadAudioFiles(::onItemDownloaded)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    // starts the download progress
    private fun startProgress() {
        //TODO("implement progress bar in the future")
        progressed = 0
        maxProgress = data.audioObjects.size
    }

    // called whenever an audio object was downloaded
    private fun onItemDownloaded() {
        //TODO("implement progress bar in the future")
        progressed++
        // we want to update the adapter with the current new time
        updateAdapter()
        // we want to save the path of the new item
        saveCurrentData()
        if (progressed == maxProgress)
            finishProgress()
    }

    private fun finishProgress() {
        //TODO("implement progress bar in the future")
    }

    // Returns the data loaded from the sharedPrefs
    private fun getDeviceData(): DatabaseData {
        val sp = getSharedPreferences(SP_NAME, MODE_PRIVATE)
        val strDeviceData: String = sp.getString("databaseData", "")
        return if (strDeviceData != "")
            Gson().fromJson(strDeviceData, DatabaseData::class.java)
        else
            DatabaseData()
    }

    // We want to save the data when the app is stopped
    override fun onStop() {
        saveCurrentData()
        super.onStop()
    }

    // Saves the current data to the device
    private fun saveCurrentData() {
        val editor = getSharedPreferences(SP_NAME, MODE_PRIVATE).edit()
        editor.putString("databaseData", data.toJson())
        editor.apply()
    }

}
