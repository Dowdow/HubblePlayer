package fr.banana_station.hubbleplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private List<Song> songList;
    private ProximityHandler proximityHandler;
    private SongFinder songFinder;
    private MusicService musicService;
    private Intent intent;
    private boolean musicConnection = false;
    private boolean playing = false;
    private boolean kill = false;

    private ImageButton play;
    private Menu menu;
    private SeekBar timeBar;
    private TextView playerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("======\nCREATE\n======");
        setContentView(R.layout.activity_player);

        ListView songListView = (ListView) findViewById(R.id.songList);
        play = (ImageButton) findViewById(R.id.play);
        RelativeLayout player = (RelativeLayout) findViewById(R.id.player);
        timeBar = (SeekBar) player.findViewById(R.id.timeBar);
        timeBar.setOnSeekBarChangeListener(seekBarChangeListener);
        playerTitle = (TextView) player.findViewById(R.id.playerTitle);

        songFinder = new SongFinder(getContentResolver());
        songList = songFinder.find();
        SongAdapter songAdapter = new SongAdapter(this, songList, songFinder.getCursor());
        songListView.setAdapter(songAdapter);

        proximityHandler = new ProximityHandler((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        proximityHandler.setProximityListener(proximityListener);
        proximityHandler.start();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("=====\nSTART\n=====");

        if(intent == null){
            intent = new Intent(this, MusicService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(intent);
        }

        if(playing) {
            startRunnable();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("====\nSTOP\n====");
        stopRunnable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("=======\nDESTROY\n=======");
        songFinder.closeCursor();
        proximityHandler.stop();
        playing = false;
        stopRunnable();
        unbindService(serviceConnection);
        stopService(intent);
    }

    @Override
    public void onBackPressed() {
        if(playing) {
            this.moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }

    public void onSongSelected(View view) {
        startRunnable();
        musicService.setSongPosition(view.getId());
        musicService.start();
        playing = true;
        play.setImageResource(R.drawable.pause);
        setPlayerTitleText();
    }

    public void onSongPrevious(View  view) {
        musicService.previous();
        setPlayerTitleText();
    }

    public void onSongNext(View view) {
        musicService.next();
        setPlayerTitleText();
    }

    public void onPlayPause(View view) {
        if(playing) {
            playing = false;
            stopRunnable();
            musicService.pause();
            play.setImageResource(R.drawable.play);
        } else {
            playing = true;
            startRunnable();
            musicService.play();
            play.setImageResource(R.drawable.pause);
        }
        setPlayerTitleText();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shuffle:
                if (musicService.isShuffle()) {
                    musicService.setShuffle(false);
                    menu.findItem(R.id.shuffle).setTitle(R.string.shuffle_on);
                } else {
                    musicService.setShuffle(true);
                    menu.findItem(R.id.shuffle).setTitle(R.string.shuffle_off);
                }
                return true;
            case R.id.proximity:
                if(proximityHandler.isStarting()) {
                    proximityHandler.stop();
                    menu.findItem(R.id.proximity).setTitle(R.string.proximity_on);
                } else {
                    proximityHandler.start();
                    menu.findItem(R.id.proximity).setTitle(R.string.proximity_off);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setPlayerTitleText() {
        playerTitle.setText(musicService.getSongTitle());
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (musicConnection && playing) {
                setPlayerTitleText();
                timeBar.setProgress(musicService.getProgress());
            }
            if (!kill) {
                handler.postDelayed(this, 500);
            }
        }

    };

    public void startRunnable() {
        kill = false;
        runOnUiThread(runnable);
    }

    public void stopRunnable() {
        kill = true;
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private int lastSelected = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if(b) {
                lastSelected = i;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            musicService.seekTo(lastSelected);
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setSongList(songList);
            musicConnection = true;
            setPlayerTitleText();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicConnection = false;
        }
    };

    private ProximityHandler.ProximityListener proximityListener = new ProximityHandler.ProximityListener() {
        @Override
        public void onProximityDetected() {
            if(musicConnection) {
                musicService.next();
                setPlayerTitleText();
            }
        }
    };

}
