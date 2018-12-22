package com.onlapplications.mindtechmanager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.audio_file_row.view.*
import java.text.DecimalFormat

class AudioFileAdapter(val items : ArrayList<AudioObject>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.audio_file_row, parent, false))
    }

    // Binds each animal in the ArrayList to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = items[position].name
        val audioLen = items[position].getDurationInMillis() / 1000
        val f = DecimalFormat("00")
        holder.tvLength.text = (audioLen/60).toString() + ":" + f.format(audioLen%60)
    }
}

class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val tvName: TextView = view.tvName
    val tvLength: TextView = view.tvLength
}
