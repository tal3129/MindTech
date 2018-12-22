package com.onlapplications.mindtechmanager

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val spName = "appData"
    }

    // The audio data from the firebase
    private var data = DatabaseData()

    private var progressed = 0
    private var maxProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializations
        FirebaseApp.initializeApp(this)

        // Downloading the audio data from the firebase
        downloadDatabaseData()
        rvAudioFiles.layoutManager = LinearLayoutManager(this)
    }

    private fun updateAdapter() {
        rvAudioFiles.adapter = AudioFileAdapter(data.audioObjects, this)
    }

    private fun downloadDatabaseData() {
        // First load the data from the device
        data = getDeviceData()
        updateAdapter()

        // Then download the data from the firebase
        var tempDbData: DatabaseData
        val dataRef = FirebaseDatabase.getInstance().getReference("databaseData")
        dataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                tempDbData = dataSnapshot.getValue(DatabaseData::class.java) ?: return

                // Now the database data has been downloaded. check if our device data is up to date.
                // if it isn't, download the audio files again
                if (tempDbData.dataVersion > data.dataVersion) {
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
        maxProgress = data.audioObjects.size
    }

    // called whenever an audio object was downloaded
    private fun onItemDownloaded() {
        //TODO("implement progress bar in the future")
        progressed++
        if (progressed == maxProgress)
            finishProgress()
    }

    private fun finishProgress() {
        //TODO("implement progress bar in the future")
        updateAdapter()
    }

    // Returns the data loaded from the sharedPrefs
    private fun getDeviceData(): DatabaseData {
        val sp = getSharedPreferences(spName, MODE_PRIVATE)
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
        val editor = getSharedPreferences(spName, MODE_PRIVATE).edit()
        editor.putString("databaseData", data.toJson())
        editor.apply()
    }

}
