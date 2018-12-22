package com.onlapplications.customalarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import android.app.TimePickerDialog
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.util.*


class MainActivity : AppCompatActivity(), Observer {
    companion object {
        const val spName = "appData"
    }

    private lateinit var pendingIntent: PendingIntent
    private lateinit var alarmManager: AlarmManager

    // the current alarm time in milliseconds. -1 if it is not active. Try to keep this object saved in the sp, as it is used from there when an alarm is made
    private var currentAlarm: AlarmObject = AlarmObject()
    private var alarmActive: Boolean = false

    // The audio data from the firebase
    private var data = DatabaseData()

    private var progressed = 0
    private var maxProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializations
        FirebaseApp.initializeApp(this)
        ObservableObject.getInstance().addObserver(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager


        // Downloading the audio data from the firebase
        downloadDatabaseData()

        // Setting the alarm time tv text to the current time
        setAlarmTimeTv(System.currentTimeMillis())

        // Setting the onClickListener to opening the timePickerDialog
        clickToChangeTimeLayout.setOnClickListener {
            startTimePickerDialog()
        }

        // Load prev alarms from shared prefs
        loadCurrentAlarm()
    }

    private fun downloadDatabaseData() {
        // First load the data from the device
        data = getDeviceData()
        updateSpinner()

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
                    updateSpinner()
                    data.downloadAudioFiles(::onItemDownloaded)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    // updates the spinner, from data
    private fun updateSpinner() {
        spinnerAudioFile.adapter = ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                data.audioObjects.map { it.name })
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

    // Loads the current alarm from the shared prefs
    private fun loadCurrentAlarm() {
        val jsonAlarm = getSharedPreferences(spName, MODE_PRIVATE).getString("currentAlarm", "")
        currentAlarm = Gson().fromJson(jsonAlarm, AlarmObject::class.java) ?: return
        if (currentAlarm.timeInMillis != (-1).toLong()) {

            // sets the time to the alarm time
            setAlarmTimeTv(currentAlarm.timeInMillis)

            // set the spinner to the alarm chosen item if exists
            for (i in 0 until spinnerAudioFile.adapter.count)
                if (spinnerAudioFile.getItemAtPosition(i).toString() == currentAlarm.audioObject.name)
                    spinnerAudioFile.setSelection(i)

            // sets the repetitions to the alarm's repetitions
            edReps.setText(currentAlarm.repetitions.toString())

            // set the rest of the layout to the active alarm's one
            toggleButton.isChecked = true
            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.alarmOnTextColor))
            tvClickDescription.setText(R.string.tv_clickToChange_alarmOn)
        }
    }

    // opens the TimePickerDialog window
    private fun startTimePickerDialog() {
        val mcurrentTime = Calendar.getInstance()
        val hour = mcurrentTime.get(Calendar.HOUR_OF_DAY)
        val minute = mcurrentTime.get(Calendar.MINUTE)
        val mTimePicker: TimePickerDialog
        mTimePicker = TimePickerDialog(this@MainActivity,
                TimePickerDialog.OnTimeSetListener { timePicker, selectedHour, selectedMinute ->
                    val f = DecimalFormat("00")
                    chosenTimeTv.text = f.format(selectedHour) + ":" + f.format(selectedMinute)
                    // Show a toast if we had an alarm active
                    toggleAlarm(false, alarmActive)
                }
                , hour, minute, true)
        mTimePicker.setTitle("בחר שעה")
        mTimePicker.show()
    }

    // Sets the text of the alarmTimeTv from the millis provided
    private fun setAlarmTimeTv(milliseconds: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = milliseconds
        val formatter = SimpleDateFormat("HH:mm")
        chosenTimeTv.text = formatter.format(cal.time)
    }

    // Called when the toggle button is clicked. If it is now on, set the alarm, else - cancel it
    fun onToggleClicked(view: View) {
        if ((view as ToggleButton).isChecked) {
            toggleAlarm(true)
        } else {
            toggleAlarm(false)

        }
    }

    private fun toggleAlarm(on: Boolean, showToast: Boolean = true) {
        if (on) {
            alarmActive = true

            setAlarmOn(showToast)
            toggleButton.isChecked = true
            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.alarmOnTextColor))
            tvClickDescription.setText(R.string.tv_clickToChange_alarmOn)
        } else {
            alarmActive = false

            if (this::pendingIntent.isInitialized)
                alarmManager.cancel(pendingIntent)
            toggleButton.isChecked = false

            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor))
            tvClickDescription.setText(R.string.tv_clickToChange_alarmOff)

            // Reset the alarm time holder
            currentAlarm.timeInMillis = -1

            // Show the toast
            if (showToast)
                Toast.makeText(this, "ההתראה כובתה", Toast.LENGTH_SHORT).show()
        }
    }


    // sets the alarm on.
    // needs to be called whenever alarm time is changed or the toggle button is clicked.
    private fun setAlarmOn(showToast: Boolean) {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance()

        val chosenTimes = chosenTimeTv.text.split(":")
        alarmTime.set(Calendar.HOUR_OF_DAY, chosenTimes[0].toInt())
        alarmTime.set(Calendar.MINUTE, chosenTimes[1].toInt())

        // if the time is lower than the current time, the alarm is set to tomorrow.
        val tomorrow = now.timeInMillis > alarmTime.timeInMillis
        if (tomorrow)
            alarmTime.timeInMillis += 24 * 60 * 60 * 1000

        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.timeInMillis, pendingIntent)
        if (showToast)
            Toast.makeText(this, "התראה חדשה ל" + (if (tomorrow) "מחר בשעה  " else "שעה ") + chosenTimeTv.text, Toast.LENGTH_LONG).show()


        currentAlarm.timeInMillis = alarmTime.timeInMillis
        saveCurrentAlarmData()
    }


    // Saves the current AlarmObject to the device
    private fun saveCurrentAlarmData() {
        val editor = getSharedPreferences(spName, MODE_PRIVATE).edit()
        val reps = edReps.text.toString()
        currentAlarm.repetitions = (if (reps.isEmpty()) "1" else reps).toInt()
        currentAlarm.audioObject = data.audioObjects[spinnerAudioFile.selectedItemPosition]
        editor.putString("currentAlarm", currentAlarm.toJson())
        editor.apply()
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

    // Called by the observer when the alarm is received
    override fun update(p0: Observable?, p1: Any?) {
        toggleAlarm(false, false)
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

    // called after all audio objects were downloaded
    private fun finishProgress() {
        //TODO("implement progress bar in the future")
    }
}
