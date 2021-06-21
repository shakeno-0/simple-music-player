package com.animee.localmusic

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_main.*

val UPDATESTH="updatesth"
val NOTIFICATION_ACTION = "notification_action";
val NOTIFICATION_BTN_STATE = "notification_btn_state";
val NOTIFICATION_PLAY = 0
val NOTIFICATION_NEXT_MUSIC = 1
val NOTIFICATION_PRE_MUSIC = 2;

class detailActivity : AppCompatActivity(), View.OnClickListener{


    private var isSeekBarChanging:Boolean = false
    lateinit var musicService: MusicService

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            if (msg.what === 0x01) {
                tv_cur_time.text = "00:00"
                tv_cur_time.text = musicService.current?.let { formatTime(it) }
                tv_total_time.text = formatTime(musicService.time)
                seekBar.max = musicService.time
                seekBar.progress = musicService.current!!
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isSeekBarChanging = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        isSeekBarChanging = false
                        musicService.seekTo(seekBar.progress)
                    }
                })
                this.sendEmptyMessage(0x01)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        initView()
        initService()
        initBroadcastReceiver()
        if (MusicService.isPlaying) {
            iv_play.setImageResource(R.mipmap.icon_pause)
        } else {
            iv_play.setImageResource(R.mipmap.icon_play)
        }
        updatesth()
    }

    private fun initBroadcastReceiver(){
        val intentFilter= IntentFilter()
        intentFilter.addAction(UPDATESTH)
        registerReceiver(receiver,intentFilter)
    }

    private fun initService(){
        val intent=Intent(this,MusicService::class.java)
        bindService(intent,conn,Context.BIND_AUTO_CREATE)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent!!.action.equals(UPDATESTH)){
                updatesth()
            }
        }
    }

    override fun onDestroy(){
        super.onDestroy()
        unbindService(conn)
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(receiver)
    }

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            musicService = (service as MusicService.MusicBinder).getService()
            handler.sendEmptyMessage(0x01)
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.iv_pre -> {
                preMusic()
                MusicService.isPlaying = false
            }
            R.id.iv_play ->{
                playMusic()
                updateBtnPlayOrPause()
            }
            R.id.iv_next -> {
                nextMusic()
                MusicService.isPlaying = false
            }
            R.id.btn_return -> onBackPressed()
        }
    }

    private fun updatesth(){
        val musicBean: LocalMusicBean = mDatas!![MusicService.nposition]
        if (MusicService.isPlaying) {
            iv_play.setImageResource(R.mipmap.icon_pause)
        } else {
            iv_play.setImageResource(R.mipmap.icon_play)
        }
        val bm= musicBean.path?.let { getArtBitmap(it) }
        musicPicture.setImageBitmap(bm)
        music_name.text = musicBean.song
        music_singer.text = musicBean.singer
    }

    private fun updateBtnPlayOrPause() {
        if (MusicService.isPlaying) {
            iv_play.setImageResource(R.mipmap.icon_play)
        } else {
            iv_play.setImageResource(R.mipmap.icon_pause)
        }
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

    private fun initView(){
        iv_pre.setOnClickListener(this)
        iv_play.setOnClickListener(this)
        iv_next.setOnClickListener(this)
        btn_return.setOnClickListener(this)
    }

    private fun formatTime(time: Int): String {
        val miao = time / 1000
        val minute = miao / 60
        val second = miao % 60
        return String.format("%02d:%02d", minute, second)
    }

    private fun playMusic() {
        val intent = Intent()
        intent.action = MusicService.ACTION
        val bundle = Bundle()
        bundle.putInt(MusicService.BTN_STATE, MusicService.PLAY_STATE)
        intent.putExtras(bundle)
        sendBroadcast(intent)
    }
    private fun nextMusic() {
        val intent = Intent()
        intent.action = MusicService.ACTION
        val bundle = Bundle()
        bundle.putInt(MusicService.BTN_STATE, MusicService.NEXT_MUSIC_STATE)
        intent.putExtras(bundle)
        sendBroadcast(intent)
    }
    private fun preMusic() {
        val intent = Intent()
        intent.action = MusicService.ACTION
        val bundle = Bundle()
        bundle.putInt(MusicService.BTN_STATE, MusicService.PRE_MUSIC_STATE)
        intent.putExtras(bundle)
        sendBroadcast(intent)
    }
}