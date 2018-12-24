package com.onlapplications.customalarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.TransitionDrawable
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.transition.Transition
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.new_main.*
import org.jetbrains.anko.*
import java.util.*
import kotlin.jvm.java


class MainActivity : AppCompatActivity(), Observer {
    companion object {
        const val spName = "appData"
    }

    private lateinit var pendingIntent: PendingIntent
    private lateinit var alarmManager: AlarmManager

    // the current alarm time in milliseconds. -1 if it is not active. Try to keep this object saved in the sp, as it is used from there when an alarm is made
    private var currentAlarm: AlarmObject = AlarmObject()
    private var currentAlarmActive: Boolean = false
        get() = currentAlarm.timeInMillis != (-1).toLong()

    // The audio data from the firebase
    private var data = DatabaseData()

    private var progressed = 0
    private var maxProgress = 0

    private var selectedItemPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_main)

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

        setLayoutToAlarmMode(false)

        // Load prev alarms from shared prefs
        loadCurrentAlarm()
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

    // updates the adapter, from data
    private fun updateAdapter() {
        listViewAudioFile.adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                data.audioObjects.map { it.name }) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val newView = TextView(context)
                newView.setPadding(50, 20, 50, 20)
                newView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                newView.text = getItem(position)
                newView.setTextColor(ContextCompat.getColor(context, if (position == selectedItemPosition) R.color.alarmOnTextColor else R.color.defaultTextColor))
                if (currentAlarmActive && position == selectedItemPosition)
                    newView.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
                else
                    newView.backgroundResource = R.drawable.list_background_animation
                return newView
            }
        }
        listViewAudioFile.setOnItemClickListener { adapterView, view, position, id ->
            if (toggleButton.isOrWillBeHidden)
                toggleButton.show()
            selectedItemPosition = position
            runOnUiThread {
                (listViewAudioFile.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            }
        }
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
        currentAlarm = Gson().fromJson(jsonAlarm, AlarmObject::class.java) ?: AlarmObject()
        if (currentAlarmActive) {

            // sets the time to the alarm time
            setAlarmTimeTv(currentAlarm.timeInMillis)

            // sets the repetitions to the alarm's repetitions
            edReps.setText(currentAlarm.repetitions.toString())

            // set the selected item position to the position of the first one with a matching id
            selectedItemPosition = data.audioObjects.indexOfFirst { it.firebaseId == currentAlarm.audioObject.firebaseId }

            // set the rest of the layout to the active alarm's one
            setLayoutToAlarmMode(alarmOn = true)
        }
    }

    // Changes the layout depending on the alarm mode
    private fun setLayoutToAlarmMode(alarmOn: Boolean) {
        if (alarmOn) {
            toggleButton.isChecked = true
            edReps.isEnabled = false
            listViewAudioFile.isEnabled = false
            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.alarmOnTextColor))
            tvClickDescription.setTextColor(ContextCompat.getColor(this, R.color.alarmOnTextColor))
            tvClickDescription.setText(R.string.tv_clickToChange_alarmOn)
            val selectedItem = listViewAudioFile.getChildAt(selectedItemPosition)
            if (selectedItem != null && selectedItem.background is TransitionDrawable) {
                (selectedItem.background as TransitionDrawable).startTransition(1250)
                Thread {
                    Thread.sleep(1250)
                    runOnUiThread {
                        (listViewAudioFile.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                    }
                }
            }
        } else {
            toggleButton.isChecked = false
            edReps.isEnabled = true
            listViewAudioFile.isEnabled = true
            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor))
            tvClickDescription.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor))
            tvClickDescription.setText(R.string.tv_clickToChange_alarmOff)
            (listViewAudioFile.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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
                    if (currentAlarmActive)
                        toggleAlarm(false, currentAlarmActive)
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
        if ((view as FloatingActionButton).isChecked) {
            toggleAlarm(false)
        } else {
            //first we need to check if the fields are full
            if (edReps.text.toString().isEmpty() || selectedItemPosition == -1)
                toast("יש לבחור כמות חזרות וקובץ אודיו")
            else
                toggleAlarm(true)
        }
    }

    // toggles the alarm on or off
    private fun toggleAlarm(on: Boolean, showToast: Boolean = true) {
        if (on) {
            setAlarmOn(showToast)
            setLayoutToAlarmMode(alarmOn = true)
        } else {
            if (this::pendingIntent.isInitialized)
                alarmManager.cancel(pendingIntent)

            selectedItemPosition = -1
            setLayoutToAlarmMode(alarmOn = false)

            // Reset the alarm time holder
            currentAlarm.timeInMillis = -1

            // Show the toast
            if (showToast)
                Toast.makeText(this, "ההתראה כובתה", Toast.LENGTH_SHORT).show()
        }
        // whenever we change the state of the current alarm, save it
        saveCurrentAlarmData()
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
    }


    // Saves the current AlarmObject to the device
    private fun saveCurrentAlarmData() {
        val editor = getSharedPreferences(spName, MODE_PRIVATE).edit()
        // if the current alarm has no time, it is an empty one - don't read the rest of the values
        if (!currentAlarmActive) {
            currentAlarm = AlarmObject()
        } else {
            val reps = edReps.text.toString()
            currentAlarm.repetitions = (if (reps.isEmpty()) "1" else reps).toInt()
            currentAlarm.audioObject = data.audioObjects[selectedItemPosition]
        }
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


private var FloatingActionButton.isChecked: Boolean
    get() : Boolean {
        return tag == 1
    }
    set(on) {
        if (on) {
            tag = 1
            this.hide()

            doAsync {
                Thread.sleep(1000)
                uiThread {
                    it.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_red_light))
                    it.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_delete))
                    it.show()
                }
            }
        } else {
            tag = -1
            this.hide()

            doAsync {
                Thread.sleep(250)
                uiThread {
                    it.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_green_light))
                    it.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done))
                }
            }
        }
    }
