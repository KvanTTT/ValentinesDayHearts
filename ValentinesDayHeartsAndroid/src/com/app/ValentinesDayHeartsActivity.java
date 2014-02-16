package com.app;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;

public class ValentinesDayHeartsActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new ValentinesDayHearts(), false);
    }
}