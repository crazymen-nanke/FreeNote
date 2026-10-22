package com.crazymen.freenote;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.crazymen.freenote.ui.ProfileFragment;
import com.crazymen.freenote.ui.TimelineFragment;
import com.crazymen.freenote.ui.TodayFragment;

public class MainActivity extends AppCompatActivity {

    public static final int TAB_TODAY = 0;
    public static final int TAB_TIMELINE = 1;
    public static final int TAB_PROFILE = 2;

    private static final String STATE_TAB = "state_tab";

    private final LinearLayout[] tabs = new LinearLayout[3];
    private final ImageView[] icons = new ImageView[3];
    private final TextView[] labels = new TextView[3];

    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs[TAB_TODAY] = findViewById(R.id.tabToday);
        tabs[TAB_TIMELINE] = findViewById(R.id.tabTimeline);
        tabs[TAB_PROFILE] = findViewById(R.id.tabProfile);

        icons[TAB_TODAY] = findViewById(R.id.iconToday);
        icons[TAB_TIMELINE] = findViewById(R.id.iconTimeline);
        icons[TAB_PROFILE] = findViewById(R.id.iconProfile);

        labels[TAB_TODAY] = findViewById(R.id.labelToday);
        labels[TAB_TIMELINE] = findViewById(R.id.labelTimeline);
        labels[TAB_PROFILE] = findViewById(R.id.labelProfile);

        for (int i = 0; i < tabs.length; i++) {
            final int tab = i;
            tabs[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(tab);
                }
            });
        }

        int startTab = savedInstanceState == null ? TAB_TODAY : savedInstanceState.getInt(STATE_TAB, TAB_TODAY);
        selectTab(startTab);
    }

    public void selectTab(int tab) {
        if (tab == currentTab) {
            return;
        }
        currentTab = tab;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        String tag = "tab-" + tab;
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            target = createFragment(tab);
            tx.add(R.id.container, target, tag);
        }
        for (Fragment f : fm.getFragments()) {
            if (f != target && !f.isHidden()) {
                tx.hide(f);
            }
        }
        tx.show(target);
        tx.commit();

        updateNav(tab);
    }

    @NonNull
    private Fragment createFragment(int tab) {
        switch (tab) {
            case TAB_TIMELINE:
                return new TimelineFragment();
            case TAB_PROFILE:
                return new ProfileFragment();
            case TAB_TODAY:
            default:
                return new TodayFragment();
        }
    }

    private void updateNav(int active) {
        int onPrimary = getColor(R.color.on_primary);
        int inactive = getColor(R.color.text_tertiary);
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == active;
            tabs[i].setBackgroundResource(on ? R.drawable.bg_tab_active : 0);
            icons[i].setColorFilter(on ? onPrimary : inactive);
            labels[i].setTextColor(on ? onPrimary : inactive);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    @Override
    public void onBackPressed() {
        if (currentTab != TAB_TODAY && currentTab != -1) {
            selectTab(TAB_TODAY);
            return;
        }
        super.onBackPressed();
    }
}
