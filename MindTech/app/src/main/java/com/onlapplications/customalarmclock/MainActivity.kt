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
import java.util.*


class MainActivity : AppCompatActivity(), Observer {
    companion object {
        const val spName = "appData"
    }

    private lateinit var pendingIntent: PendingIntent
    private lateinit var alarmManager: AlarmManager

    // the current alarm time in milliseconds. -1 if none is active.
    private var currentAlarmTimeInMillis: Long = -1
    private var alarmActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ObservableObject.getInstance().addObserver(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        setAlarmTimeTv(System.currentTimeMillis())

        // Setting the onClickListener to opening the timePickerDialog
        clickToChangeTimeLayout.setOnClickListener {
            startTimePickerDialog()
        }

        loadCurrentAlarm()
    }

    // Loads the current alarm from the shared prefs
    private fun loadCurrentAlarm() {
        currentAlarmTimeInMillis = getSharedPreferences(spName, MODE_PRIVATE).getLong("alarmTimeInMillis", -1)
        if (currentAlarmTimeInMillis != (-1).toLong()) {
            setAlarmTimeTv(currentAlarmTimeInMillis)
            toggleAlarm(true,  false)
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
        } else {
            alarmActive = false

            alarmManager.cancel(pendingIntent)
            toggleButton.isChecked = false

            chosenTimeTv.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor))

            // Reset the alarm time holder
            currentAlarmTimeInMillis = -1

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

        val ringtoneLength = 1000.toLong()
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.timeInMillis, ringtoneLength + 2000, pendingIntent)
        if (showToast)
            Toast.makeText(this, "התראה חדשה ל" + (if (tomorrow) "מחר בשעה  " else "שעה ") + chosenTimeTv.text, Toast.LENGTH_LONG).show()


        currentAlarmTimeInMillis = alarmTime.timeInMillis
    }


    override fun onStop() {
        val editor = getSharedPreferences(spName, MODE_PRIVATE).edit()
        editor.putLong("alarmTimeInMillis", currentAlarmTimeInMillis)
        editor.apply()
        super.onStop()
    }

    // Called by the observer when the
    override fun update(p0: Observable?, p1: Any?) {
        toggleAlarm(false, false)
    }
}
