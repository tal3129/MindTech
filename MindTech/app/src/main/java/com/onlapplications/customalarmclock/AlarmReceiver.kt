package com.onlapplications.customalarmclock


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.alarm_received), Toast.LENGTH_LONG).show()
        var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val ringtone = RingtoneManager.getRingtone(context, alarmUri)
        ringtone.play()

        //reset the alarm
        val editor = context.getSharedPreferences(MainActivity.spName, AppCompatActivity.MODE_PRIVATE).edit()
        editor.putLong("alarmTimeInMillis", -1)
        editor.apply()

        ObservableObject.getInstance().updateValue(intent)
    }
}