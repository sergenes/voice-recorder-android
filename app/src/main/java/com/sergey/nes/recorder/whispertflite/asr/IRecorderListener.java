package com.sergey.nes.recorder.whispertflite.asr;

public interface IRecorderListener {
    void onUpdateReceived(String message);

    void onDataReceived(float[] samples);
}
