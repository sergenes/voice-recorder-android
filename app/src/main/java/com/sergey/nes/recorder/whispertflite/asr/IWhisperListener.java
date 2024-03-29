package com.sergey.nes.recorder.whispertflite.asr;

public interface IWhisperListener {
    void onUpdateReceived(String message);
    void onResultReceived(String result, String audioFileId);
}
