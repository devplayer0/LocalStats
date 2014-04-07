package com.jackos2500.localstats.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.jackos2500.localstats.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
    }
}