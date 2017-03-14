package fr.banana_station.hubbleplayer;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    private List<Song> songList;
    private ProximityHandler proximityHandler;
    private SongFinder songFinder;
    private MusicService musicService;
    private Intent intent;
    private boolean musicConnection = false;
    private boolean playing = false;
    private boolean kill = false;
    private boolean authorised = false;

    private ListView songListView;
    private ImageButton play;
    private Menu menu;
    private SeekBar timeBar;
    private TextView playerTitle;

    /**
     * onCreate
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("======\nCREATE\n======");
        setContentView(R.layout.activity_player);

        songListView = (ListView) findViewById(R.id.songList);
        play = (ImageButton) findViewById(R.id.play);
        RelativeLayout player = (RelativeLayout) findViewById(R.id.player);
        timeBar = (SeekBar) player.findViewById(R.id.timeBar);
        timeBar.setOnSeekBarChangeListener(seekBarChangeListener);
        playerTitle = (TextView) player.findViewById(R.id.playerTitle);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            fillSongList();
        }

        PackageManager packageManager = this.getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY)) {
            showNoProximitySensor();
        }
        proximityHandler = new ProximityHandler((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        proximityHandler.setProximityListener(proximityListener);
        proximityHandler.start();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    /**
     * onStart
     */
    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("=====\nSTART\n=====");

        if (authorised) {
            if (intent == null) {
                intent = new Intent(this, MusicService.class);
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                startService(intent);
            }

            if (playing) {
                startRunnable();
            }
        }
    }

    /**
     * onStop
     */
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("====\nSTOP\n====");
        stopRunnable();
    }

    /**
     * onDestroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("=======\nDESTROY\n=======");
        if (songFinder != null) {
            songFinder.closeCursor();
        }
        proximityHandler.stop();
        playing = false;
        stopRunnable();
        try {
            unbindService(serviceConnection);
        } catch (Exception e) {
            System.err.println("Service is not registered");
        }
        if (intent != null) {
            stopService(intent);
        }
    }

    /**
     * onBackPressed
     */
    @Override
    public void onBackPressed() {
        if (playing) {
            this.moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Exit the activity properly
     */
    private void exitApp() {
        this.finishAffinity();
    }

    /**
     * Called when the user select a song in the list.
     * Tell the music service to play the track selected.
     *
     * @param view View
     */
    public void onSongSelected(View view) {
        startRunnable();
        musicService.setSongPosition(view.getId());
        musicService.start();
        playing = true;
        play.setImageResource(R.drawable.pause);
        setPlayerTitleText();
    }

    /**
     * Called when the user press the previous button.
     * Tell the music service to play the previous song.
     *
     * @param view View
     */
    public void onSongPrevious(View view) {
        if (musicService != null) {
            musicService.previous();
            setPlayerTitleText();
        }
    }

    /**
     * Called when the user press the next button.
     * Tell the music service to play the new song.
     *
     * @param view View
     */
    public void onSongNext(View view) {
        if (musicService != null) {
            musicService.next();
            setPlayerTitleText();
        }
    }

    /**
     * Called when the user the play or pause button.
     * Tell the music service to play a song or pause it if already playing.
     *
     * @param view View
     */
    public void onPlayPause(View view) {
        if (musicService != null) {
            if (playing) {
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
    }

    /**
     * Set the song title in the title text view.
     */
    private void setPlayerTitleText() {
        playerTitle.setText(musicService.getSongTitle());
    }

    /**
     * Invoke the option menu
     *
     * @param menu Menu
     * @return boolean
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Event listener for the menu items
     *
     * @param item MenuItem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shuffle:
                if (musicService != null) {
                    if (musicService.isShuffle()) {
                        musicService.setShuffle(false);
                        menu.findItem(R.id.shuffle).setTitle(R.string.shuffle_on);
                    } else {
                        musicService.setShuffle(true);
                        menu.findItem(R.id.shuffle).setTitle(R.string.shuffle_off);
                    }
                    return true;
                }
                return false;
            case R.id.proximity:
                if (proximityHandler.isStarting()) {
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

    /**
     * Seek Bar management
     */
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private int lastSelected = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (b) {
                lastSelected = i;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (musicService != null) {
                musicService.seekTo(lastSelected);
            }
        }
    };

    /**
     * Service connection binding
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

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

    /**
     * Proximity Listener
     */
    private ProximityHandler.ProximityListener proximityListener = new ProximityHandler.ProximityListener() {
        @Override
        public void onProximityDetected() {
            if (musicConnection) {
                musicService.next();
                setPlayerTitleText();
            }
        }

        @Override
        public void onLongProximityDetected() {
            displaySpeechRecognizer();
        }
    };

    /**
     * Handler for the runnable
     */
    private Handler handler = new Handler();
    /**
     * Runnable to manage the seek bar progression
     */
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

    /**
     * Start the seek bar manager runnable
     */
    public void startRunnable() {
        kill = false;
        runOnUiThread(runnable);
    }

    /**
     * Stop the seek bar manager runnable
     */
    public void stopRunnable() {
        kill = true;
    }

    /**
     * Callback for permission requests
     *
     * @param requestCode  int
     * @param permissions  String
     * @param grantResults int
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fillSongList();
                    onStart();
                } else {
                    exitApp();
                }
            }
        }
    }

    /**
     * Call the SongFinder to fill the list of songs.
     */
    private void fillSongList() {
        songFinder = new SongFinder(getContentResolver());
        songList = songFinder.find();
        SongAdapter songAdapter = new SongAdapter(this, songList, songFinder.getCursor());
        songListView.setAdapter(songAdapter);
        if (songList.size() > 0) {
            authorised = true;
        } else {
            showNoMusicFound();
        }
    }

    /**
     * Build and show an Alert when no music were found
     */
    private void showNoMusicFound() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_music_message).setTitle(R.string.no_music_title);
        builder.setPositiveButton(R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Build and show an Alert when the device does not have a proximity sensor
     */
    private void showNoProximitySensor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_proximity_message).setTitle(R.string.no_proximity_title);
        builder.setPositiveButton(R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Display the speech recognition popup
     */
    private void displaySpeechRecognizer() {
        if (musicService != null) {
            musicService.setLowVolume();
        }

        Intent intent;
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isInteractive() || keyguardManager.inKeyguardRestrictedInputMode()) {
            intent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
            intent.putExtra(RecognizerIntent.EXTRA_SECURE, true);
        } else {
            intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        }
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.googlequicksearchbox"));
            startActivity(browserIntent);
        }

        new CountDownTimer(8000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                finishActivity(SPEECH_REQUEST_CODE);
                if (musicService != null) {
                    musicService.setHighVolume();
                }
            }
        }.start();
    }

    /**
     * Callback from the speech recognition popup
     *
     * @param requestCode int
     * @param resultCode  int
     * @param data        Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (musicService != null) {
            musicService.setHighVolume();
        }
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            switch (resultCode) {
                case RESULT_OK:
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);
                    if (musicService != null) {
                        musicService.findSong(spokenText);
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
