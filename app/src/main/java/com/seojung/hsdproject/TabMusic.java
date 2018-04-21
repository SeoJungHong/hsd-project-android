package com.seojung.hsdproject;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import com.seojung.hsdproject.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by SeoJung on 16. 6. 7..
 */
public class TabMusic extends Fragment implements MediaPlayerControl {

    private final static String TAG = "TabMusic";
    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    private ArrayList<Song> songList = new ArrayList<>();
    private ListView songView;

    MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    // 하단부에 생기는 컨트롤러 뷰. show를 새로 호출해야 refresh 된다. 주의!
    MusicController controller;
    // 선택된 노래 색깔 바꾸기 위해 사용
    int oldSongPosition;
    int newSongPosition;

    boolean playbackPaused = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.tab_music, container, false);
        songView = view.findViewById(R.id.song_list);

        //set the controller up
        controller = new MusicController(getContext(), false);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(songView);

        Log.d(TAG, "[send]~4");
        BluetoothService.sendData("~4");

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            getSongList();
        }
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getSongList();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    public void songPicked(View view) {
        oldSongPosition = newSongPosition;
        newSongPosition = Integer.parseInt(view.getTag().toString());
        selectSong();
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if (playbackPaused) {
            Log.d(TAG, "songPicked, playpackPaused");
//            setController();
            playbackPaused = false;
        }
        controller.show();
    }

    public void selectSong() {
        songList.get(oldSongPosition).setSelected(false);
        songList.get(newSongPosition).setSelected(true);
        if (oldSongPosition != newSongPosition) {
            getViewByPosition(oldSongPosition, songView).setBackgroundColor(Color.parseColor("#fff880"));
            getViewByPosition(newSongPosition, songView).setBackgroundColor(Color.parseColor("#adffef"));
        }
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

//    public void setController() {
//        //set the controller up
//        controller = new MusicController(getContext(), false);
//        controller.setPrevNextListeners(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playNext();
//            }
//        }, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playPrev();
//            }
//        });
//        controller.setMediaPlayer(this);
//        controller.setAnchorView(view.findViewById(R.id.song_list));
//        controller.setEnabled(true);
//    }

    public void playNext() {
        Log.d(TAG, "playNext");
        musicSrv.playNext();

        oldSongPosition = newSongPosition;
        newSongPosition = musicSrv.getCurrentSongPosition();
        selectSong();

        if (playbackPaused) {
            Log.d(TAG, "playbackpaused!!");
//            setController();
            playbackPaused = false;
        }
        controller.show();
    }

    public void playPrev() {
        Log.d(TAG, "playPrev");
        musicSrv.playPrev();

        oldSongPosition = newSongPosition;
        newSongPosition = musicSrv.getCurrentSongPosition();
        selectSong();

        if (playbackPaused) {
            Log.d(TAG, "playbackpaused!!");
//            setController();
            playbackPaused = false;
        }
        controller.show();
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContext().getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
        // alphabet 순서로 섞기
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdaptor = new SongAdapter(getContext(), songList);
        songView.setAdapter(songAdaptor);
        controller.setEnabled(true);
    }

    public ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConntected!!");
            MusicBinder binder = (MusicBinder) service;
            binder.setFragment(TabMusic.this);
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;

            if (isPlaying()) {
                Log.d(TAG, "isPlaying");
                // 현재 재생중이라면 아두이노로 현재 노래 정보 다시 전송
                newSongPosition = musicSrv.getCurrentSongPosition();
                String title = trimString(songList.get(newSongPosition).getTitle());
                String artist = trimString(songList.get(newSongPosition).getArtist());
                String formattedInfo = "[" + title + "]" + artist;
                Log.d(TAG, "[send]" + formattedInfo);
                BluetoothService.sendData(formattedInfo);
                selectSong();

                // 현재 재생중이라면 controller 다시 나타나도록 하기
                controller.show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    // 너무 스트링이 길면 arduino에서 오류 발생함. 너무 길면 자르고 요약한다.
    public static String trimString(String input) {
        if (input.length() > 10) {
            input = input.substring(0, 8);
            input = input.concat("..");
            return input;
        } else {
            return input;
        }
    }

    // MediaPlayerControl interface method
    @Override
    public void pause() {
        Log.d(TAG, "pause!");
        playbackPaused = true;
        musicSrv.pausePlayer();
        controller.show();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        Log.d(TAG, "start!");
        musicSrv.go();
        controller.show();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(getContext(), MusicService.class);
            getContext().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            getContext().startService(playIntent);
        }
        if (isPlaying()) {
            controller.show();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        controller.hide();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        getContext().stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }
}
