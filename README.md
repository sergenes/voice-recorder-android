# Android Voice Recorder Demo App (Kotlin, Jetpack Compose, MVVM, JNI, OpenAI Whisper, and TensorFlow Lite)

This is a dictaphone app where users can record and log short voice messages. The app maintains a
history of recordings, allowing users to play them later. Users can transcribe selected recordings,
search through recordings by transcribed text, delete recordings, and share selected items. When a
user plays a recording, subsequent recordings automatically play in descending order.

## Brief Project Description

This project was created using the default Android Studio New App template (Empty Activity) and
implemented as a Kotlin Jetpack Compose project.

### Environment

- Android Studio Iguana | 2023.2.1 Patch 1
- Gradle Version: 8.4

### Changes Made

- Added dependency to an extended icons library for Mic and Stop icons for the Record/Stop Button.
- Added user permissions for recording audio and wake lock to keep the screen on while playing
  audio.
- Declared the microphone feature as required for compatibility with devices equipped with a
  microphone.

### Components

1. **Record/Stop Button**: Includes a circle gauge indicating the remaining time until the end of
   recording (capped at 1 minute).
    - [RecordButton](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/ui/components/RecordButton.kt)
2. **List Item View Components**:
    - Regular item (ItemView): Displays only the recording time.
    - Selected item (SelectedItemView): Allows users to play audio, control feedback, and delete the
      recording.
    - [ItemViews](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/ui/components/ItemViews.kt)

### Architecture

- MVVM pattern:
  - View: [HomeScreen](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/ui/home/HomeScreen.kt)
  - Data Model: [RecordingItem](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/models/DataModels.kt)
  - ViewModel: [HomeViewModel](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/ui/home/HomeViewModel.kt)
  - Repository: [HomeRepository](https://github.com/sergenes/voice-recorder-android/blob/main/app/src/main/java/com/sergey/nes/recorder/ui/home/HomeRepository.kt)

## TODO:

- Integrate Hilt for dependency injection.
- Add MVI implementation for comparison.
- Add Timber as a logger.
- Add functionality to share audio files and transcriptions.
- Add navigation and a Settings screen.
- Add UI/Unit tests.
- Experiment with different models to make the transcriber work for languages other than English and
  generate timecodes for transcribed words to highlight them synchronously with audio playback.

## Offline Speech Recognition (Transcription) with OpenAI Whisper and TensorFlow Lite

The offline transcription in this project based
on [vilassn](https://github.com/vilassn/whisper_android). This Whisper implementation transcribes
only English audio to English text and Any Language audio to Translated to English text. The project
includes the Whisper Tiny Model (39M parameters), TensorFlow Lite, and FlatBuffers.

I updated the `transcribeFile` function in `TFLiteEngine.cpp` to support audio files longer than 30
seconds, although testing has been limited to 60-second files.

**Before Fix:**

```java
std::string TFLiteEngine::transcribeFile(const char*waveFile){
        std::vector<float>pcmf32=readWAVFile(waveFile);
        pcmf32.resize((WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE),0);
        std::string text=transcribeBuffer(pcmf32);
        return text;
        }
```

**After Fix:**

```java
std::string TFLiteEngine::transcribeFile(const char*waveFile){
        // make transcription work for files longer than 30 seconds
        std::vector<float>pcmf32=readWAVFile(waveFile);
        size_t originalSize=pcmf32.size();

        // Determine the number of chunks required to process the entire file
        size_t totalChunks=(originalSize+(WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE)-1)/
        (WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE);

        std::string text;
        for(size_t chunkIndex=0;chunkIndex<totalChunks; ++chunkIndex){
        // Extract a chunk of audio data
        size_t startSample=chunkIndex*WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE;
        size_t endSample=std::min(startSample+(WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE),
        originalSize);
        std::vector<float>chunk(pcmf32.begin()+startSample,pcmf32.begin()+endSample);

        // Pad the chunk if it's smaller than the expected size
        if(chunk.size()<WHISPER_SAMPLE_RATE *WHISPER_CHUNK_SIZE){
        chunk.resize(WHISPER_SAMPLE_RATE*WHISPER_CHUNK_SIZE,0);
        }

        // Transcribe the chunk and append the result to the text
        std::string chunkText=transcribeBuffer(chunk);
        text+=chunkText;
        }
        return text;
        }
```

## Screenshots

| Initial Screen                 | Recordings Added                 | Transcription Example                 |
|--------------------------------|----------------------------------|---------------------------------------|
| ![Initial Screen](screen1.png) | ![Recordings Added](screen2.png) | ![Transcription Example](screen3.png) |

## Contact

Connect and follow me on
LinkedIn: [Sergey N](https://www.linkedin.com/in/sergey-neskoromny-86662a10/)