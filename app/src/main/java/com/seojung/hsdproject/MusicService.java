package com.seojung.hsdproject;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;
import android.app.Notification;
import android.app.PendingIntent;

/**
 * Created by SeoJung on 16. 6. 7..
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    final static String TAG = "MusicService";

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosition;
    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    private final MusicBinder musicBind = new MusicBinder();

    @Override
    public void onCreate(){
        //create the service
        super.onCreate();
        //initialize position
        songPosition = 0;
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        //create player
        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    public void playSong(){
        //play a song
        player.reset();
        //get song
        Song playSong = songs.get(songPosition);
        songTitle = playSong.getTitle();

        String title = TabMusic.trimString(playSong.getTitle());
        String artist = TabMusic.trimString(playSong.getArtist());
        String formattedInfo = "[" + title + "]" + artist;

        Log.d(TAG, "[send]" + formattedInfo);
        BluetoothService.sendData(formattedInfo);
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public void setSong(int songIndex){
        songPosition = songIndex;
    }

    // OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
        go();// 선택하자마자 재생 가능하도록
        musicBind.getFragment().controller.show();
    }

    public void setList(ArrayList<Song> theSongs){
        songs = theSongs;
    }

    // OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(player.getCurrentPosition() > 0){
            mediaPlayer.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    // For MediaPlayer Controller
    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        songPosition--;
        if(songPosition < 0) songPosition = songs.size()-1;
        playSong();
    }

    //skip to next
    public void playNext(){
        songPosition++;
        if(songPosition >= songs.size()) songPosition =0;
        playSong();
    }

    public int getCurrentSongPosition() {
        return songPosition;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopForeground(true);
    }

    public class MusicBinder extends Binder {

        TabMusic tabMusic;

        MusicService getService() {
            return MusicService.this;
        }

        TabMusic getFragment() {
            return tabMusic;
        }

        void setFragment(TabMusic tabMusic) {
            Log.d("MusicBinder", "setFragment");
            this.tabMusic = tabMusic;
        }
    }

}
