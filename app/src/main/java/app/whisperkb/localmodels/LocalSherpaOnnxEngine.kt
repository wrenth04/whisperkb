package app.whisperkb.localmodels

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineZipformerCtcModelConfig
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.min

class LocalSherpaOnnxEngine(private val context: Context) {
    private val modelDirectory = File(context.filesDir, "models/local")

    val isAvailable: Boolean
        get() = resolveModelFiles() != null

    val availabilityMessage: String
        get() = when {
            !modelDirectory.exists() -> "No local model installed"
            !modelDirectory.isDirectory -> "Local model path is not a directory"
            resolveModelFiles() == null -> "No usable Sherpa model files found"
            else -> "Local Sherpa ONNX is available"
        }

    fun transcribe(path: String): Result<String> {
        val audioFile = File(path)
        if (!audioFile.exists() || !audioFile.isFile) {
            return Result.failure(IllegalStateException("Recording file is missing"))
        }
        val modelFiles = resolveModelFiles()
            ?: return Result.failure(IllegalStateException("No usable Sherpa model files found"))

        return runCatching {
            val recognizer = OfflineRecognizer(
                null,
                OfflineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80, dither = 0.0f),
                    modelConfig = OfflineModelConfig(
                        zipformerCtc = OfflineZipformerCtcModelConfig(model = modelFiles.model.absolutePath),
                        provider = "cpu",
                        modelType = "zipformer_ctc",
                        numThreads = 2,
                        tokens = modelFiles.tokens.absolutePath,
                    ),
                ),
            )
            try {
                val stream = recognizer.createStream()
                val pcm = extractPcm16(audioFile)
                var offset = 0
                while (offset < pcm.size) {
                    val end = min(offset + 4000, pcm.size)
                    stream.acceptWaveform(pcm.copyOfRange(offset, end).toFloatArray(), 16_000)
                    recognizer.decode(stream)
                    offset = end
                }
                recognizer.getResult(stream).text.trim().ifBlank {
                    throw IllegalStateException("Sherpa returned an empty transcript")
                }
            } finally {
                recognizer.release()
            }
        }
    }

    private fun extractPcm16(audioFile: File): ShortArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(audioFile.absolutePath)
        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: throw IllegalStateException("No audio track found in shared media")
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val output = ArrayList<Short>(64_000)
        var inputDone = false
        var outputDone = false
        var sawInputEOS = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)!!
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex >= 0 -> {
                        val buffer = decoder.getOutputBuffer(outputIndex)!!
                        appendDecodedPcm(buffer, bufferInfo.size, output)
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && sawInputEOS -> outputDone = true
                }
            }
        } finally {
            runCatching { decoder.stop() }
            runCatching { decoder.release() }
            runCatching { extractor.release() }
        }

        return output.toShortArray()
    }

    private fun appendDecodedPcm(buffer: ByteBuffer, size: Int, output: MutableList<Short>) {
        val shortBuffer = buffer.asShortBuffer()
        val count = min(size / 2, shortBuffer.remaining())
        repeat(count) { output.add(shortBuffer.get()) }
    }

    private fun ShortArray.toFloatArray(): FloatArray = FloatArray(size) { index -> this[index] / 32768f }

    private fun resolveModelFiles(): ModelFiles? {
        if (!modelDirectory.exists() || !modelDirectory.isDirectory) return null
        val files = modelDirectory.walkTopDown().filter { it.isFile }.associateBy { it.name }
        val model = files["model.int8.onnx"] ?: files.values.firstOrNull { it.name.endsWith(".onnx") || it.name.endsWith(".ort") }
        val tokens = files["tokens.txt"]
        return if (model != null && tokens != null) ModelFiles(model = model, tokens = tokens) else null
    }

    private data class ModelFiles(val model: File, val tokens: File)
}
