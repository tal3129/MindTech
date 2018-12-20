package com.onlapplications.customalarmclock


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.Gson
import android.app.PendingIntent
import android.app.AlarmManager



class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.alarm_received), Toast.LENGTH_LONG).show()

        // Get the alarm data from the sp
        val sp = context.getSharedPreferences(MainActivity.spName, AppCompatActivity.MODE_PRIVATE)
        val alarmData: AlarmObject = Gson().fromJson(sp.getString("currentAlarm", ""), AlarmObject::class.java)


        // plays the sound
        playAlarmSound(context, alarmData)


        // decrement the repetitions
        alarmData.repetitions --


        //reset the alarm
        if(alarmData.repetitions == 0) {
            alarmData.timeInMillis = -1

            val amg = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            amg.cancel(pendingIntent)

            ObservableObject.getInstance().updateValue(intent)
        }

        // save the changes
        val editor = sp.edit()
        editor.putString("currentAlarm", alarmData.toJson())
        editor.apply()
    }

    // plays the sound
    private fun playAlarmSound(context: Context, alarmData: AlarmObject) {
        val audioPath = alarmData.audioObject?.pathInPhone
        if (audioPath != null) {
            val alarmUri: Uri? = Uri.parse(audioPath)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone.play()
        }
    }
}