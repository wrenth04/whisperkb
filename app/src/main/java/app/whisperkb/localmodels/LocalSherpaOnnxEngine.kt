package app.whisperkb.localmodels

import android.content.Context
import java.io.File

class LocalSherpaOnnxEngine(private val context: Context) {
    private val modelDirectory = File(context.filesDir, "models/local")

    val isAvailable: Boolean = false

    val availabilityMessage: String
        get() = when {
            !modelDirectory.exists() -> "No local model installed"
            !modelDirectory.isDirectory -> "Local model path is not a directory"
            modelDirectory.listFiles().isNullOrEmpty() -> "Local model directory is empty"
            else -> "Local Sherpa ONNX binding is not wired in yet"
        }

    fun transcribe(path: String): Result<String> {
        val audioFile = File(path)
        return Result.failure(
            IllegalStateException(
                when {
                    !audioFile.exists() -> "Recording file is missing"
                    !audioFile.isFile -> "Recording file path is invalid"
                    else -> availabilityMessage
                }
            )
        )
    }
}
