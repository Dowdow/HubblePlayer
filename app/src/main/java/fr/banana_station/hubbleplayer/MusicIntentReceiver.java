package fr.banana_station.hubbleplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MusicIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    musicIntentListener.onHeadsetUnplug();
                    break;
                case 1:
                    musicIntentListener.onHeadsetPlug();
                    break;
            }
        }
    }

    private MusicIntentListener musicIntentListener;

    public void setMusicIntentListener(MusicIntentListener musicIntentListener) {
        this.musicIntentListener = musicIntentListener;
    }

    interface MusicIntentListener {
        void onHeadsetPlug();

        void onHeadsetUnplug();
    }
}
