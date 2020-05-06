package fr.banana_station.hubble_player;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;

public class Intro extends AppIntro2 {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntro2Fragment.newInstance(
                getResources().getString(R.string.intro_title_1),
                getResources().getString(R.string.intro_description_1),
                R.drawable.intro1,
                getResources().getColor(R.color.colorPrimary, getTheme())
        ));
        addSlide(AppIntro2Fragment.newInstance(
                getResources().getString(R.string.intro_title_2),
                getResources().getString(R.string.intro_description_2),
                R.drawable.intro2,
                getResources().getColor(R.color.colorPrimary, getTheme())
        ));
        addSlide(AppIntro2Fragment.newInstance(
                getResources().getString(R.string.intro_title_3),
                getResources().getString(R.string.intro_description_3),
                R.drawable.intro3,
                getResources().getColor(R.color.colorPrimary, getTheme())
        ));
        showStatusBar(false);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        finish();
        overridePendingTransition(0, 0);
    }
}
