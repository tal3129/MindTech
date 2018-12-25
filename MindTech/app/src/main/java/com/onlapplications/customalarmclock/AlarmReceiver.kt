package com.onlapplications.customalarmclock


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.Gson
import org.jetbrains.anko.doAsync


class AlarmReceiver : BroadcastReceiver() {
    private val intervalMs: Long = 1000

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.alarm_received), Toast.LENGTH_LONG).show()

        // Get the alarm data from the sp
        val sp = context.getSharedPreferences(MainActivity.spName, AppCompatActivity.MODE_PRIVATE)
        val alarmData: AlarmObject = Gson().fromJson(sp.getString("currentAlarm", ""), AlarmObject::class.java)

        // play the alarm sound asyncly
        doAsync {
            // Remove any prior media playing
                ObservableObject.getInstance().resetMp()
                ObservableObject.getInstance().mp = MediaPlayer()
            playAlarmSound(context, alarmData, intent)
            }

    }

    // plays the sound alarm's repetitions times - call this async
    private fun playAlarmSound(context: Context, alarmData: AlarmObject, intent: Intent) {
        val audioPath = alarmData.audioObject.pathInPhone
        val mp = ObservableObject.getInstance().mp
        if (mp != null) {
            try {
                mp.reset()
                mp.setDataSource(audioPath)
                mp.setOnCompletionListener {
                    alarmData.repetitions--
                    if (alarmData.repetitions == 0)
                        saveAlarmDataUpdateObserer(context, alarmData, intent)
                    else {
                        Thread.sleep(intervalMs)
                        playAlarmSound(context, alarmData, intent)
                    }
                }
                mp.prepare()
                mp.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // notify MainActivity that the alarm was activated
    private fun saveAlarmDataUpdateObserer(context: Context, alarmData: AlarmObject, intent: Intent) {
        val sp = context.getSharedPreferences(MainActivity.spName, AppCompatActivity.MODE_PRIVATE)

        ObservableObject.getInstance().updateValue(intent)
        // cancel the current alarm
        alarmData.timeInMillis = -1
        val amg = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        amg.cancel(pendingIntent)

        // save the changes
        val editor = sp.edit()
        editor.putString("currentAlarm", alarmData.toJson())
        editor.apply()
    }
}