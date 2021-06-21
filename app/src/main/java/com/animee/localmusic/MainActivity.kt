package com.animee.localmusic

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.animee.localmusic.LocalMusicAdapter.OnItemClickListener
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

//    数据源
var mDatas: MutableList<LocalMusicBean>? = null

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mMusicService: MusicService
    private var adapter: LocalMusicAdapter? = null
    var mediaPlayer: MediaPlayer? = null

    //    记录当前正在播放的音乐的位置
    var currnetPlayPosition = -1

    //    记录暂停音乐时进度条的位置
    var currentPausePositionInSong = 0

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        mediaPlayer = MediaPlayer()
        mDatas=ArrayList<LocalMusicBean>()
        //     创建适配器对象
        adapter = LocalMusicAdapter(this, mDatas as ArrayList<LocalMusicBean>)
        local_music_rv.adapter = adapter
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        local_music_rv.layoutManager = layoutManager

        //      权限管理 + 加载本地数据源
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                   != PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(this,arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
            }
            requestPermissions(this,arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }else{
            loadLocalMusicData()
        }
        initBroadcastReceiver()
        //        设置每一项的点击事件
        setEventListener()
        if(MusicService.islive){
            updatesth()
            currnetPlayPosition=MusicService.nposition
        }
    }

    private fun initBroadcastReceiver(){
        val intentFilter= IntentFilter()
        intentFilter.addAction(UPDATESTH)
        registerReceiver(receiver,intentFilter)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadLocalMusicData()
                } else {
                    Toast.makeText(this, "You denied the READ permission",
                            Toast.LENGTH_SHORT).show()
                }
            }
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadLocalMusicData()
                } else {
                    Toast.makeText(this, "You denied the WRITE permission",
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setEventListener() {
        /* 设置每一项的点击事件*/
        adapter?.setOnItemClickListener(object : OnItemClickListener {
            override fun OnItemClick(view: View?, position: Int) {
                currnetPlayPosition = position
                val musicBean: LocalMusicBean = mDatas!![position]
                playMusicInMusicBean(musicBean)
            }
        })
    }

    fun playMusicInMusicBean(musicBean: LocalMusicBean) {
        local_music_bottom_tv_singer.text = musicBean.singer
        local_music_bottom_tv_song.text = musicBean.song
        val bm= musicBean.path?.let { getArtBitmap(it) }
        val intent=Intent(this,MusicService::class.java)
        val bundle=Bundle()
        bundle.putInt("position",currnetPlayPosition)
        intent.putExtras(bundle)
        startService(intent)
        local_music_bottom_iv_icon.setImageBitmap(bm)
        local_music_bottom_iv_play.setImageResource(R.mipmap.icon_pause)
    }

    private fun playMusic() {
        /* 播放音乐的函数*/
        if (MusicService.islive) {
            val intent = Intent()
            intent.action = MusicService.ACTION
            val bundle = Bundle()
            bundle.putInt(MusicService.BTN_STATE, MusicService.PLAY_STATE)
            intent.putExtras(bundle)
            sendBroadcast(intent)
        }
    }

    private fun nextMusic() {
        if (MusicService.islive) {
            val intent = Intent()
            intent.action = MusicService.ACTION
            val bundle = Bundle()
            bundle.putInt(MusicService.BTN_STATE, MusicService.NEXT_MUSIC_STATE)
            intent.putExtras(bundle)
            sendBroadcast(intent)
        }
    }
    private fun preMusic() {
        if (MusicService.islive) {
            val intent = Intent()
            intent.action = MusicService.ACTION
            val bundle = Bundle()
            bundle.putInt(MusicService.BTN_STATE, MusicService.PRE_MUSIC_STATE)
            intent.putExtras(bundle)
            sendBroadcast(intent)
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent!!.action.equals(UPDATESTH)){
                updatesth()
            }
        }
    }

    private fun updatesth(){
        val musicBean: LocalMusicBean = mDatas!![MusicService.nposition]
        local_music_bottom_tv_singer.text = musicBean.singer
        local_music_bottom_tv_song.text = musicBean.song
        if (MusicService.isPlaying) {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_pause)
        } else {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_play)
        }
        val bm= musicBean.path?.let { getArtBitmap(it) }
        local_music_bottom_iv_icon.setImageBitmap(bm)
    }

    private fun updateBtnPlayOrPause() {
        if (MusicService.isPlaying) {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_play)
        } else {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_pause)
        }
    }

    override fun onResume() {
        super.onResume()
        if (MusicService.isPlaying) {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_pause)
        } else {
            local_music_bottom_iv_play.setImageResource(R.mipmap.icon_play)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @SuppressLint("Recycle", "SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadLocalMusicData() {
        /* 加载本地存储当中的音乐mp3文件到集合当中*/
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null)?.apply {
            var id = 0
            while (moveToNext()){
                val song = getString(getColumnIndex(MediaStore.Audio.Media.TITLE))
                val singer = getString(getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val album = getString(getColumnIndex(MediaStore.Audio.Media.ALBUM))
                id++
                val sid = id.toString()
                val path = getString(getColumnIndex(MediaStore.Audio.Media.DATA))
                val duration = getLong(getColumnIndex(MediaStore.Audio.Media.DURATION))
                val sdf = SimpleDateFormat("mm:ss")
                val time = sdf.format(Date(duration))
                //          获取专辑图片主要是通过album_id进行查询
                val album_id = getString(getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                val albumArt = getAlbumArt(album_id)
                //            将一行当中的数据封装到对象当中
                val bean: com.animee.localmusic.LocalMusicBean = com.animee.localmusic.LocalMusicBean(sid, song, singer, album, time, path, albumArt,duration)
                mDatas!!.add(bean)
            }
        }
        //        数据源变化，提示适配器更新
        adapter?.notifyDataSetChanged()
    }

    fun getArtBitmap(path : String): Bitmap? {
        val myRetriever = MediaMetadataRetriever()
        try {
            myRetriever.setDataSource(path) // the URI of audio file
        } catch (e: Exception) {
            Log.e("error", "getArtBitmapError: $e")
            return null
        }
        val artwork = myRetriever.embeddedPicture
        return if (artwork != null) {
            BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
        } else {
            null
        }
    }

    private fun getAlbumArt(album_id: String): String? {
        val mUriAlbums = "content://media/external/audio/albums"
        val projection = arrayOf("album_art")
        var cur = this.contentResolver.query(
                Uri.parse("$mUriAlbums/$album_id"),
                projection, null, null, null)
        var album_art: String? = null
        if (cur!!.count > 0 && cur.columnCount > 0) {
            cur.moveToNext()
            album_art = cur.getString(0)
        }
        cur.close()
        return album_art
    }

    private fun initView() {
        /* 初始化控件的函数*/
        local_music_bottom_iv_next.setOnClickListener(this)
        local_music_bottom_iv_last.setOnClickListener(this)
        local_music_bottom_iv_play.setOnClickListener(this)
        local_music_bottom_iv_icon.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.local_music_bottom_iv_last -> {
                if(currnetPlayPosition==-1){
                    Toast.makeText(this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show()
                    return
                }
                preMusic()
            }
            R.id.local_music_bottom_iv_next -> {
                if(currnetPlayPosition==-1){
                    Toast.makeText(this, "没有可播放的歌曲！", Toast.LENGTH_SHORT).show()
                    return
                }
                nextMusic()
            }
            R.id.local_music_bottom_iv_play -> {
                if (currnetPlayPosition == -1) {
//                    并没有选中要播放的音乐
                    Toast.makeText(this, "请选择想要播放的音乐", Toast.LENGTH_SHORT).show()
                    return
                }
                playMusic()
                updateBtnPlayOrPause()
            }
            R.id.local_music_bottom_iv_icon -> {
                if(currnetPlayPosition!=-1){
                    val intent=Intent(this,detailActivity::class.java)
                    startActivity(intent)
                    return
                }
                else{
                    Toast.makeText(this, "请选择想要播放的音乐", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
    }
}