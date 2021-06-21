package com.animee.localmusic

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LocalMusicAdapter(var context: Context, mDatas: List<LocalMusicBean>) : RecyclerView.Adapter<LocalMusicAdapter.LocalMusicViewHolder>() {
    var mDatas: List<LocalMusicBean>
    var onItemClickListener: OnItemClickListener? = null
    @JvmName("setOnItemClickListener1")
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun OnItemClick(view: View?, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalMusicViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_local_music, parent, false)
        return LocalMusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocalMusicViewHolder, position: Int) {
        val musicBean: LocalMusicBean = mDatas[position]
        holder.idTv.text = musicBean.id
        holder.songTv.text = musicBean.song
        holder.singerTv.text = musicBean.singer
        holder.albumTv.text = musicBean.album
        holder.timeTv.text = musicBean.duration
        holder.itemView.setOnClickListener { v -> onItemClickListener!!.OnItemClick(v, position) }
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }

    inner class LocalMusicViewHolder(itemView: View) : ViewHolder(itemView) {
        var idTv: TextView
        var songTv: TextView
        var singerTv: TextView
        var albumTv: TextView
        var timeTv: TextView

        init {
            idTv = itemView.findViewById(R.id.item_local_music_num)
            songTv = itemView.findViewById(R.id.item_local_music_song)
            singerTv = itemView.findViewById(R.id.item_local_music_singer)
            albumTv = itemView.findViewById(R.id.item_local_music_album)
            timeTv = itemView.findViewById(R.id.item_local_music_durtion)
        }
    }

    init {
        this.mDatas = mDatas
    }
}