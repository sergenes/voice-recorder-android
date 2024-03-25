# voice-recorder-android
A dictaphone app where users can record and log short voice messages. The app keeps history of the
recordings so users can play them later.

# Scope
There’s a lot you could build in this so here are some indication to make the exercise simpler:

- The app is all local, no need for authentication or storing anything in the cloud
- Limit recordings to 60 seconds so you don’t need to deal with large files
- Recordings play continuously: when a user plays a recording, it will automatically play the
  following recordings in order
- No need for a nice UI. Focus rather on robust functionality/usability for a small-scale production
  app - No need for tests in the code
- You can program in the language, framework you prefer and use any libraries

# Android, Kotlin, Jetpack Compose App, MVVM
Example of one screen app created from the standard Android Studio Template (Empty Activity). This
project demonstrate Compose UI alongside with separation of code into a View, ViewModel, and
Repository.

         Initial Screen         |  A few recording added

:------------------------------:|:-------------------------:
![Image](Screenshot_1.webp)     |  ![Image](Screenshot_2.webp)