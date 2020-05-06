package fr.banana_station.hubble_player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;
    private final IBinder iBinder = new MusicBinder();
    private List<Song> songList;
    private int songPosition;
    private static final int NOTIFY_ID = 1;
    private boolean prepared = false;
    private boolean shuffle = false;

    /**
     * onCreate
     */
    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes.Builder builder = new AudioAttributes.Builder();
        builder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);

        songPosition = 0;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(builder.build());
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    /**
     * onDestroy
     */
    @Override
    public void onDestroy() {
        mediaPlayer.release();
        stopForeground(true);
    }

    /**
     * Music Binder
     */
    class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Load and prepare a song to be played.
     */
    public void start() {
        mediaPlayer.reset();
        Song song = songList.get(songPosition);
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
        } catch (IOException e) {
            Log.e("Music Service", "Error while try to get the song");
        }
        try {
            mediaPlayer.prepareAsync();
            prepared = true;
        } catch (Exception ignored) {
            Log.e("Music Service", "Error while try to read the song");
        }
    }

    /**
     * Play a song
     */
    public void play() {
        if (prepared) {
            mediaPlayer.start();
        } else {
            start();
        }
    }

    /**
     * Pause a song
     */
    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Change the music to the previous one.
     */
    public void previous() {
        if (shuffle) {
            shuffle();
        } else {
            if (songPosition == 0) {
                songPosition = songList.size() - 1;
            } else {
                songPosition--;
            }
            if (mediaPlayer.isPlaying()) {
                start();
            } else {
                prepared = false;
            }
        }
    }

    /**
     * Change the music to the next one.
     */
    public void next() {
        if (shuffle) {
            shuffle();
        } else {
            if (songPosition == songList.size() - 1) {
                songPosition = 0;
            } else {
                songPosition++;
            }
            if (mediaPlayer.isPlaying()) {
                start();
            } else {
                prepared = false;
            }
        }
    }

    /**
     * Low down the volume for voice recognition
     */
    public void setLowVolume() {
        try {
            mediaPlayer.setVolume((float) 0.05, (float) 0.05);
        } catch (IllegalStateException e) {
            System.out.println("Can't switch volume, app is closed");
        }
    }

    /**
     * Max the volume after voice recognition
     */
    public void setHighVolume() {
        try {
            mediaPlayer.setVolume(1, 1);
        } catch (IllegalStateException e) {
            System.out.println("Can't switch volume, app is closed");
        }
    }

    /**
     * Change to a random song
     */
    public void shuffle() {
        Random random = new Random();
        songPosition = random.nextInt(songList.size());
        if (mediaPlayer.isPlaying()) {
            start();
        } else {
            prepared = false;
        }
    }

    /**
     * Try to find a song play it.
     *
     * @param search String
     */
    public void findSong(String search) {
        List<Song> matchSongs = new ArrayList<>();
        search = search.toLowerCase();
        // Split to match every words
        String[] pattern = search.split(" ");
        for (Song song : songList) {
            int matches = 0;
            for (String word : pattern) {
                Pattern p = Pattern.compile(".*" + word + ".*");
                Matcher m = p.matcher((song.getTitle() + " " + song.getArtist()).toLowerCase());
                if (m.matches()) {
                    matches++;
                }
            }
            if (matches == pattern.length) {
                matchSongs.add(song);
            }
        }
        // Match the best based on Levenstein Distance
        Song bestMatch = null;
        int minimumDistance = 0;
        for (Song song : matchSongs) {
            String title = (song.getTitle() + " " + song.getArtist()).toLowerCase();
            if (bestMatch == null) {
                bestMatch = song;
                minimumDistance = LevenshteinDistance.compute(search, title);
            } else {
                int tempDistance = LevenshteinDistance.compute(search, title);
                if (tempDistance < minimumDistance) {
                    bestMatch = song;
                    minimumDistance = tempDistance;
                }
            }
        }
        if (bestMatch != null) {
            songPosition = songList.indexOf(bestMatch);
            if (mediaPlayer.isPlaying()) {
                start();
            } else {
                prepared = false;
            }
        }
    }

    /**
     * Seek a given part of the song
     *
     * @param i int
     */
    public void seekTo(int i) {
        if (mediaPlayer.isPlaying()) {
            float percentage = (float) i / 100;
            float seek = percentage * mediaPlayer.getDuration();
            mediaPlayer.seekTo((int) seek);
        }
    }

    /**
     * Return the time progression of the song.
     *
     * @return int
     */
    public int getProgress() {
        if (mediaPlayer.isPlaying()) {
            return (int) ((float) mediaPlayer.getCurrentPosition() / (float) mediaPlayer.getDuration() * 100);
        }
        return 0;
    }

    /**
     * Callback when the service is binded.
     *
     * @param intent Intent
     * @return IBinder
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    /**
     * Callback when the service is unbinded.
     *
     * @param intent Intent
     * @return boolean
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    /**
     * Callback when a song is fully loaded.
     *
     * @param mediaPlayer MediaPlayer
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel() : "";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.vinyl)
                .setTicker(getSongTitle())
                .setOngoing(true)
                .setContentTitle("Hubble Player").setContentText(getSongTitle());

        Notification notification = builder.build();
        startForeground(NOTIFY_ID, notification);
        mediaPlayer.start();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "hubble_notification_service";
        String channelName = "Hubble Notification Service";
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setLightColor(Color.BLUE);
        notificationChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) {
            service.createNotificationChannel(notificationChannel);
        }
        return channelId;
    }

    /**
     * Callback when a song is ended.
     *
     * @param mediaPlayer MediaPlayer
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        next();
        play();
    }

    /**
     * Callback when an error occurs
     *
     * @param mediaPlayer MediaPlayer
     * @param i           int
     * @param i1          int
     * @return boolean
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return false;
    }

    /* GETTERS AND SETTERS */

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
}
