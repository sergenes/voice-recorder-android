package com.sergey.nes.recorder.ui.home

import android.content.Context
import com.sergey.nes.recorder.app.Config
import com.sergey.nes.recorder.models.RecordingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class HomeRepository(private val context: Context? = null) {

    suspend fun getRecordings(): Flow<Result<List<RecordingItem>>> = flow {
        emit(runCatching {
            val recordingItemsList = mutableListOf<RecordingItem>()
            val files = context?.getExternalFilesDir(Config.RECORDINGS_FOLDER)?.listFiles()
                ?: throw IOException("Directory not found or inaccessible")

            files.filter { it.absolutePath.endsWith(".json") }
                .mapNotNullTo(recordingItemsList) { file ->
                    val fileInfo = File(file.absolutePath)
                    try {
                        readRecordingInfo(fileInfo.absolutePath)
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
            val folderPath = context?.getExternalFilesDir(Config.RECORDINGS_FOLDER)
            val fileName = "$recordingId.${Config.FILE_EXTENSION}"
            val file = File(folderPath, fileName)

            val fileInfoName = "$recordingId.json"
            val fileInfo = File(folderPath, fileInfoName)

            if (file.exists() && fileInfo.exists()) {
                if (!file.delete() || !fileInfo.delete()) throw IOException(
                    "Unable to delete file: $fileName"
                )
            }
            Unit
        })
    }

    suspend fun saveRecordingInfo(recording: RecordingItem): Flow<Result<Unit>> = flow {
        emit(runCatching {
            val fileName = "${recording.id}.json"
            val file = File(context?.getExternalFilesDir(Config.RECORDINGS_FOLDER), fileName)
            val json = Json.encodeToString(recording)
            file.writeText(json)
        })
    }

    private fun readRecordingInfo(filePath: String): RecordingItem {
        val json = File(filePath).readText()
        return Json.decodeFromString(json)
    }
}