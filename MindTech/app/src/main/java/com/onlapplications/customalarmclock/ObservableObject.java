package com.onlapplications.customalarmclock;

import android.media.MediaPlayer;

import org.jetbrains.annotations.Nullable;

import java.util.Observable;

public class ObservableObject extends Observable {
    private static ObservableObject instance = new ObservableObject();
    @Nullable
    public MediaPlayer mp = null;

    public static ObservableObject getInstance() {
        return instance;
    }

    private ObservableObject() {
    }

    public void updateValue(Object data) {
        synchronized (this) {
            setChanged();
            notifyObservers(data);
        }
    }

    public MediaPlayer getMpOrNewOne() {
        if (mp == null)
            return new MediaPlayer();
        return mp;
    }

    public void resetMp(){
        if (mp != null) {
            mp.stop();
            mp.release();
        }
        mp = null;
    }
}