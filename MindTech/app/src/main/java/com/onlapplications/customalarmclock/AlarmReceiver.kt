package com.onlapplications.customalarmclock


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.Gson
import android.app.PendingIntent
import android.app.AlarmManager
import android.media.MediaPlayer
import org.jetbrains.anko.doAsync


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.alarm_received), Toast.LENGTH_LONG).show()

        // Get the alarm data from the sp
        val sp = context.getSharedPreferences(MainActivity.spName, AppCompatActivity.MODE_PRIVATE)
        val alarmData: AlarmObject = Gson().fromJson(sp.getString("currentAlarm", ""), AlarmObject::class.java)


        // plays the sound for the amount of repetitions
        val audioLen = alarmData.audioObject.getDurationInMillis()
        val interval = 1000

        // play the alarm sound asyncly
        doAsync {
            if (audioLen > 0) {
                ObservableObject.getInstance().resetMp()
                ObservableObject.getInstance().mp = MediaPlayer()
                for (i in 1..alarmData.repetitions) {
                    playAlarmSound(alarmData)
                    Thread.sleep(audioLen + interval)
                }
            }

            // cancel the current alarm
            alarmData.timeInMillis = -1
            val amg = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            amg.cancel(pendingIntent)

            // save the changes
            val editor = sp.edit()
            editor.putString("currentAlarm", alarmData.toJson())
            editor.apply()

            // notify MainActivity that the alarm was activated
            ObservableObject.getInstance().updateValue(intent)
        }
    }

    // plays the sound
    private fun playAlarmSound(alarmData: AlarmObject) {
        val audioPath = alarmData.audioObject.pathInPhone
        val mp = ObservableObject.getInstance().mp
        if (mp!= null) {
            try {
                mp.setDataSource(audioPath)
                mp.prepare()
                mp.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}