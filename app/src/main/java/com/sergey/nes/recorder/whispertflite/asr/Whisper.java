package com.sergey.nes.recorder.whispertflite.asr;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sergey.nes.recorder.app.Config;
import com.sergey.nes.recorder.models.RecordingItem;
import com.sergey.nes.recorder.whispertflite.engine.IWhisperEngine;
import com.sergey.nes.recorder.whispertflite.engine.WhisperEngineNative;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Whisper {
    public static final String TAG = "Whisper";
    public static final String ACTION_TRANSLATE = "TRANSLATE";
    public static final String ACTION_TRANSCRIBE = "TRANSCRIBE";
    public static final String MSG_PROCESSING = "Processing...";
    public static final String MSG_PROCESSING_DONE = "Processing done...!";
    public static final String MSG_FILE_NOT_FOUND = "Input file doesn't exist..!";

    private final Context context;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private final Object audioBufferQueueLock = new Object();  // Synchronization object
    private final Object whisperEngineLock = new Object();  // Synchronization object
    private final Queue<float[]> audioBufferQueue = new LinkedList<>();
    private Thread micTranscribeThread = null;

    private final IWhisperEngine whisperEngine = new WhisperEngineNative();
//    private final IWhisperEngine mWhisperEngine = new WhisperEngineTwoModel();

    private String action = null;
    private String wavFilePath = null;
    private Thread executorThread = null;
    private IWhisperListener updateListener = null;

    private String audioFileId = null;

    public Whisper(@Nullable Context context) {
        this.context = context;
    }

    public void setListener(IWhisperListener listener) {
        updateListener = listener;
        whisperEngine.setUpdateListener(updateListener);
    }

    public void loadModel(String modelPath, String vocabPath, boolean isMultilingual) {
        try {
            whisperEngine.initialize(modelPath, vocabPath, isMultilingual);

            // Start thread for mic data transcription in realtime
            startMicTranscriptionThread();
        } catch (IOException e) {
            Log.e(TAG, "Error...", e);
        }
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setFilePath(RecordingItem recording) {
        audioFileId = recording.getId();
        String fileName = recording.getId() + "." + Config.FILE_EXTENSION;
        assert context != null;
        File audioFile = new File(
                context.getExternalFilesDir(Config.RECORDINGS_FOLDER),
                fileName
        );
        wavFilePath = audioFile.getAbsolutePath();
    }

    public void start() {
        if (inProgress.get()) {
            Log.d(TAG, "Execution is already in progress...");
            return;
        }

        executorThread = new Thread(() -> {
            inProgress.set(true);
            threadFunction();
            inProgress.set(false);
        });
        executorThread.start();
    }

    public void stop() {
        inProgress.set(false);
        try {
            if (executorThread != null) {
                whisperEngine.interrupt();
                executorThread.join();
                executorThread = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isInProgress() {
        return inProgress.get();
    }

    private void sendUpdate(String message) {
        if (updateListener != null)
            updateListener.onUpdateReceived(message);
    }

    private void sendResult(String message) {
        if (updateListener != null)
            updateListener.onResultReceived(message, audioFileId);
    }

    private void threadFunction() {
        try {
            // Get Transcription
            if (whisperEngine.isInitialized()) {
                Log.d(TAG, "WaveFile: " + wavFilePath);

                File waveFile = new File(wavFilePath);
                if (waveFile.exists()) {
                    long startTime = System.currentTimeMillis();
                    sendUpdate(MSG_PROCESSING);

//                    String result = "";
//                    if (mAction.equals(ACTION_TRANSCRIBE))
//                        result = mWhisperEngine.getTranscription(mWavFilePath);
//                    else if (mAction == ACTION_TRANSLATE)
//                        result = mWhisperEngine.getTranslation(mWavFilePath);

                    // Get result from wav file
                    synchronized (whisperEngineLock) {
                        String result = whisperEngine.transcribeFile(wavFilePath);
                        sendResult(result);
                        Log.d(TAG, "Result len: " + result.length() + ", Result: " + result);
                    }

                    sendUpdate(MSG_PROCESSING_DONE);

                    // Calculate time required for transcription
                    long endTime = System.currentTimeMillis();
                    long timeTaken = endTime - startTime;
                    Log.d(TAG, "Time Taken for transcription: " + timeTaken + "ms");
                } else {
                    sendUpdate(MSG_FILE_NOT_FOUND);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error...", e);
            sendUpdate(e.getMessage());
        }
    }

    // Write buffer in Queue
    public void writeBuffer(float[] samples) {
        synchronized (audioBufferQueueLock) {
            audioBufferQueue.add(samples);
            audioBufferQueueLock.notify(); // Notify waiting threads
        }
    }

    // Read buffer from Queue
    private float[] readBuffer() {
        synchronized (audioBufferQueueLock) {
            while (audioBufferQueue.isEmpty()) {
                try {
                    // Wait for the queue to have data
                    audioBufferQueueLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return audioBufferQueue.poll();
        }
    }

    // Mic data transcription thread in realtime
    private void startMicTranscriptionThread() {
        if (micTranscribeThread == null) {
            // Create a transcribe thread
            micTranscribeThread = new Thread(() -> {
                while (true) {
                    float[] samples = readBuffer();
                    if (samples != null) {
                        synchronized (whisperEngineLock) {
                            String result = whisperEngine.transcribeBuffer(samples);
                            sendResult(result);
                        }
                    }
                }
            });

            // Start the transcribe thread
            micTranscribeThread.start();
        }
    }
}