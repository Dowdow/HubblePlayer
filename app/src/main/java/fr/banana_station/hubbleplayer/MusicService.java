package fr.banana_station.hubbleplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;
    private final IBinder iBinder = new MusicBinder();
    private List<Song> songList;
    private int songPosition;
    private static final int NOTIFY_ID = 1;
    private boolean prepared = false;
    private boolean shuffle = false;

    @Override
    public void onCreate() {
        super.onCreate();

        songPosition = 0;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onDestroy() {
        mediaPlayer.release();
        stopForeground(true);
    }

    public void start() {
        mediaPlayer.reset();
        Song song = songList.get(songPosition);
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
        } catch (IOException e) {
            Log.e("Music Service", "Error while try to read the song");
        }
        mediaPlayer.prepareAsync();
        prepared = true;
    }

    public void play() {
        if(prepared) {
            mediaPlayer.start();
        } else {
            start();
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void previous() {
        if(shuffle) {
            shuffle();
        } else {
            if(songPosition == 0) {
                songPosition = songList.size() - 1;
            } else {
                songPosition--;
            }
            if(mediaPlayer.isPlaying()) {
                start();
            } else {
                prepared = false;
            }
        }
    }

    public void next() {
        if(shuffle) {
            shuffle();
        } else {
            if (songPosition == songList.size() - 1) {
                songPosition = 0;
            } else {
                songPosition++;
            }
            if(mediaPlayer.isPlaying()) {
                start();
            } else {
                prepared = false;
            }
        }
    }

    public void shuffle() {
        Random random = new Random();
        songPosition = random.nextInt(songList.size());
        if(mediaPlayer.isPlaying()) {
            start();
        } else {
            prepared = false;
        }
    }

    public void seekTo(int i) {
        if(mediaPlayer.isPlaying()) {
            float percentage = (float) i / 100;
            float seek = percentage * mediaPlayer.getDuration();
            mediaPlayer.seekTo((int) seek);
        }
    }

    public int getProgress() {
        if(mediaPlayer.isPlaying()) {
            return (int) ((float) mediaPlayer.getCurrentPosition() / (float) mediaPlayer.getDuration() * 100);
        }
        return 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.vinyl)
                .setTicker(getSongTitle())
                .setOngoing(true)
                .setContentTitle("Hubble Player").setContentText(getSongTitle());

        Notification notification = builder.build();
        startForeground(NOTIFY_ID, notification);
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
            next();
            play();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return false;
    }

    public void setSongList(List<Song> songList) {
        this.songList = songList;
    }

    public void setSongPosition(int songPosition) {
        this.songPosition = songPosition;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public String getSongTitle() {
        return songList.get(songPosition).getTitle() + " - " + songList.get(songPosition).getArtist();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
