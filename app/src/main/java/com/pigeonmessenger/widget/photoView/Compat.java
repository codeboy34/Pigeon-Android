package com.pigeonmessenger.widget.photoView;

import android.annotation.TargetApi;
import android.view.View;

class Compat {

    private static final int SIXTY_FPS_INTERVAL = 1000 / 60;

    static void postOnAnimation(View view, Runnable runnable) {
        postOnAnimationJellyBean(view, runnable);
    }

    @TargetApi(16)
    private static void postOnAnimationJellyBean(View view, Runnable runnable) {
        view.postOnAnimation(runnable);
    }
}