package com.animee.localmusic

class LocalMusicBean {
    var id : String? = null
    var song : String? = null
    var singer : String? = null
    var album : String? = null
    var duration : String? = null
    var path : String? = null
    var albumArt : String? = null
    var time : Long?=null

    constructor() {}
    constructor(id: String?, song: String?, singer: String?, album: String?, duration: String?, path: String?, albumArt: String?,time:Long?) {
        this.id = id
        this.song = song
        this.singer = singer
        this.album = album
        this.duration = duration
        this.path = path
        this.albumArt = albumArt
        this.time = time
    }
}