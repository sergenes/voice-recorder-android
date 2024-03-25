package com.sergey.nes.recorder.ui.home

import android.content.Context
import com.sergey.nes.recorder.app.Config
import com.sergey.nes.recorder.app.Config.DATE_TIME_FORMAT
import com.sergey.nes.recorder.models.RecordingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.UUID

class HomeRepository(private val context: Context? = null) {

    suspend fun getRecordings(): Flow<Result<List<RecordingItem>>> = flow {
        emit(runCatching {
            val result = mutableListOf<RecordingItem>()
            val files = context?.getExternalFilesDir(Config.RECORDINGS_FOLDER)?.listFiles() ?: throw IOException("Directory not found or inaccessible")

            files.mapNotNullTo(result) { file ->
                if (!file.absolutePath.endsWith(".${Config.FILE_EXTENSION}")) return@mapNotNullTo null

                val name = file.name.removeSuffix(".${Config.FILE_EXTENSION}")
                val lastModified = file.lastModified()

                try {
                    RecordingItem(
                        id = UUID.fromString(name),
                        dateTime = DATE_TIME_FORMAT.format(Date(lastModified))
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            result.sortByDescending { it.dateTime }
            result
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