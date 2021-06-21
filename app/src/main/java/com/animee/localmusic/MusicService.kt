package com.animee.localmusic

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import java.io.IOException
import java.util.*

class MusicService : Service() {

    private var mPosition = 0
    private var mMediaPlayer: MediaPlayer? = null
    private var mMusicList=mDatas
    private var notification: Notification? = null
    private lateinit var manager : NotificationManager
    private lateinit var remoteViews :RemoteViews

    //onCreate()只被执行一次，因此用来做初始化
    override fun onCreate() {
        super.onCreate()
        islive=true
//        mMusicList = MusicList.getMusicData(this)
        //在MusicService刚刚启动的时候就注册了一个广播，为的是让它来接收到在其他页面点击了上一曲、下一曲、暂停/播放等按钮时，来做相应的处理
        //因此，其他页面中点击上一曲、下一曲、暂停、播放按钮时都需要向MusicService发送广播通知，让它来更新音乐
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION)
        registerReceiver(receiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        islive=false
        Log.d("wwwwwwwwww","exit")
    }

    internal inner class MusicBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return MusicBinder()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mPosition = intent.extras!!.getInt("position", -1)
        nposition=mPosition
        initNotification()
        if (mPreMediaPlayer == null || mPosition != savePosition) {
            playMusic(mPosition)
        } else {
            mMediaPlayer = mPreMediaPlayer
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 初始化Notification（系统默认UI）
     * 这里是谷歌官方使用步骤：
     * A：要开始，您需要使用notificationCompat.builder对象设置通知的内容和通道。
     * B: 在Android 8.0及更高版本上传递通知之前，必须通过将NotificationChannel实例传递给CreateNotificationChannel（），在系统中注册应用程序的通知通道。
     * C: 每个通知都应该响应tap，通常是为了在应用程序中打开与通知对应的活动。为此，必须指定用PendingIntent对象定义的内容意图，并将其传递给setContentIntent（）。
     * D: 若要显示通知，请调用notificationManagerCompat.notify（），为通知传递唯一的ID以及notificationCompat.builder.build（）的结果。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initNotification() {
        manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val bm= mMusicList!![mPosition].path?.let { getArtBitmap(it) }
        remoteViews=RemoteViews(packageName,R.layout.notification).apply {
            setTextViewText(R.id.no_music_name,mMusicList!![mPosition].song)
            setTextViewText(R.id.no_music_singer,mMusicList!![mPosition].singer)
            setImageViewBitmap(R.id.no_pic,bm)
        }
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContent(remoteViews)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIFICATION_ID,notification)
    }

    fun updateNotification() {
        val bm= mMusicList!![mPosition].path?.let { getArtBitmap(it) }
        remoteViews.apply(){
            setTextViewText(R.id.no_music_name,mMusicList!![mPosition].song)
            setTextViewText(R.id.no_music_singer,mMusicList!![mPosition].singer)
            setImageViewBitmap(R.id.no_pic,bm)
        }
        manager.notify(NOTIFICATION_ID,notification)
    }

    /**
     * 广播接收器：接收playMusic()、nextMusic()、preMusic()的动态广播
     */

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action.equals(ACTION)) {
                when (intent.getIntExtra(BTN_STATE, -1)) {
                    PLAY_STATE -> play()
                    PRE_MUSIC_STATE -> next(-1)
                    NEXT_MUSIC_STATE -> next(1)
                    else -> Toast.makeText(context, "系统出错，请稍后重试！", Toast.LENGTH_SHORT).show()
                }
                updateNotification()
            }
            if (intent.action.equals(NOTIFICATION_ACTION)) {
                when (intent.getIntExtra(NOTIFICATION_BTN_STATE, -1)) {
                    NOTIFICATION_PRE_MUSIC -> next(-1)
                    NOTIFICATION_PLAY -> play()
                    NOTIFICATION_NEXT_MUSIC -> next(1)
                    else -> Toast.makeText(context, "系统出错，请稍后重试！", Toast.LENGTH_SHORT).show()
                }
                updateNotification()
            }
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

    /**
     * 播放音乐
     */
    private fun playMusic(position: Int) {
        mMediaPlayer = MediaPlayer()
        if (mPreMediaPlayer != null) {
            mPreMediaPlayer!!.stop()
            mPreMediaPlayer!!.release()
        }
        mPreMediaPlayer = mMediaPlayer
        savePosition = position
        try {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.setDataSource(mMusicList!![position].path)
            mMediaPlayer!!.prepare()
            mMediaPlayer!!.start()
            isPlaying=true
            val intent = Intent()
            intent.action = UPDATESTH
            val bundle = Bundle()
            bundle.putInt("update",1)
            intent.putExtras(bundle)
            sendBroadcast(intent)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mMediaPlayer!!.setOnCompletionListener {
            mPosition += 1
            mPosition = (mMusicList!!.size + mPosition) % mMusicList!!.size
            nposition=mPosition
            playMusic(mPosition)
            Toast.makeText(applicationContext, "自动为您切换下一首：" + mMusicList!![mPosition].song, Toast.LENGTH_SHORT).show()
            updateNotification()
        }
    }

    /**
     * 按钮点击：播放音乐
     */
    fun play() {
        isPlaying = if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.pause()
            false
        } else {
            mMediaPlayer?.start()
            true
        }
    }

    /**
     * 按钮点击：下一首
     */
    fun next(offset: Int) {
        mPosition += offset
        mPosition = (mMusicList!!.size + mPosition) % mMusicList!!.size
        nposition=mPosition
        playMusic(mPosition)
        isPlaying = true
    }

    /**
     * 设置音乐播放的进度
     */
    fun seekTo(progress: Int) {
        mMediaPlayer?.seekTo(progress)
    }

    /**
     * 获取当前音乐的名字
     */
    val name: String?
        get() = mMusicList!![mPosition].song

    /**
     * 获取当前音乐的播放时间
     */
    val time: Int
        get() = mMusicList!![mPosition].time!!.toInt()

    /**
     * 获取当前播放位置
     */
    val current: Int?
        get() = mMediaPlayer?.currentPosition

    companion object {
        //静态存储上次音乐条目的位置
        var savePosition = 0

        //静态存储上次音乐条目的MediaPlayer
        var mPreMediaPlayer: MediaPlayer? = null
        var isPlaying = false
        var islive=false
        var nposition=-1

        //动态广播的Action
        var ACTION = "action"

        //按钮点击状态标识符
        var BTN_STATE = "btn_state"
        const val PLAY_STATE = 0
        const val NEXT_MUSIC_STATE = 1
        const val PRE_MUSIC_STATE = 2
        private const val CHANNEL_ID = "1"
        private const val CHANNEL_NAME = "MyChannel"
        private const val NOTIFICATION_ID = 2
    }
}