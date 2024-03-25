package com.sergey.nes.recorder.ui.home

import android.content.Context
import com.sergey.nes.recorder.app.Config
import com.sergey.nes.recorder.models.RecordingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.IOException
import java.util.UUID

class HomeRepository(private val context: Context? = null) {

    suspend fun getRecordings(): Flow<Result<List<RecordingItem>>> = flow {
        emit(runCatching {
            val recordingItemsList = mutableListOf<RecordingItem>()
            val files = context?.getExternalFilesDir(Config.RECORDINGS_FOLDER)?.listFiles()
                ?: throw IOException("Directory not found or inaccessible")

            files.filter { it.absolutePath.endsWith(".${Config.FILE_EXTENSION}") }
                .mapNotNullTo(recordingItemsList) { file ->
                    val name = file.name.removeSuffix(".${Config.FILE_EXTENSION}")
                    val lastModified = file.lastModified()

                    try {
                        RecordingItem(
                            id = UUID.fromString(name),
                            dateTime = lastModified
                        )
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

            recordingItemsList.sortByDescending { it.dateTime }
            recordingItemsList
        })
    }

    suspend fun deleteRecording(
        recordingId: String
    ): Flow<Result<Unit>> = flow {
        emit(runCatching {
            val fileName = "$recordingId.${Config.FILE_EXTENSION}"
            val file = File(context?.getExternalFilesDir(Config.RECORDINGS_FOLDER), fileName)

            if (file.exists() && !file.delete()) throw IOException("Unable to delete file: $fileName")
            Unit
        })
    }
}