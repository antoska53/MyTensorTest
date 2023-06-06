package com.example.mytensortest

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.mytensortest.ui.DetectorFragment
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


data class ModelConfiguration(
    // always 640/480
    val imgHeight: Int,
    val imgWidth: Int,
)

data class Detection(
    var rectF: RectF,
    var fl: Float
)

class TFDetector(
    assets: AssetManager,
    private val configuration: ModelConfiguration,
    private val cacheDirPath: String
) {

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0f, 255f))
        .build()
    private val tflite: Interpreter
    private val tensorImage = TensorImage(DataType.FLOAT32)
    private val boxesTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000, 4), DataType.FLOAT32)
    private val detectionsCountTensor = TensorBuffer.createFixedSize(intArrayOf(4), DataType.UINT8)
    private val labelsTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val scoresTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val outputs = mutableMapOf<Int, Any>(
        0 to boxesTensor.buffer, // 1000 values (4 float)
        1 to detectionsCountTensor.buffer, // 1 value (objects count)
        2 to labelsTensor.buffer, // 1000 values
        3 to scoresTensor.buffer, // 1000 values
    )

    private fun loadTFLiteModelFromAsset(assets: AssetManager): File {
        val file = File("$cacheDirPath/${DetectorFragment.MODEL_NAME_CACHE}")
        if (!file.exists()) try {
            val inputStream = assets.open(DetectorFragment.MODEL_NAME)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return file
    }

    init {
        val tfliteModel = loadTFLiteModelFromAsset(assets) // here you should get tf file
        val tfliteOptions = Interpreter.Options()
        tflite = Interpreter(tfliteModel, tfliteOptions)
        tflite.allocateTensors()
    }

    // function for detect bboxes
    fun detect(image: Bitmap): List<Detection> {

        for (buffer in outputs.values) {
            (buffer as ByteBuffer).rewind()
        }
        val paddedImage: Bitmap =
            resizeWithPadding(image, configuration.imgWidth, configuration.imgHeight)
        tensorImage.load(paddedImage)
        val tensorImage = imageProcessor.process(tensorImage)
        tflite.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)
        return convert(
            image.width,
            image.height,
            configuration.imgWidth,
            configuration.imgHeight
        )
    }

    private fun resizeWithPadding(image: Bitmap, imgWidth: Int, imgHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(image, imgWidth, imgHeight, true)
    }

    fun close() {
        tflite.close()
    }

    /*
    * Function for convert coordinates to src size
    * You must implement a Detection class that has 2 attributes:
    * bbox: RectF, score: Float
    */
    private fun convert(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): List<Detection> {
        var detectionsCount = 0
        detectionsCountTensor.intArray.forEach { count ->
            detectionsCount += count
            if (count < 255)
                return@forEach
        }
        val boxesTensor = boxesTensor.floatArray
        val scoresTensor = scoresTensor.floatArray
        val detections = ArrayList<Detection>(detectionsCount)
        val srcRatio = 1f * srcWidth / srcHeight
        val dstRatio = 1f * dstWidth / dstHeight
        var ax = 1f
        var bx = 0f
        var ay = 1f
        var by = 0f
        if (dstRatio >= srcRatio) {
            val notScaledDstWidth = (srcWidth * dstRatio / srcRatio).toInt()
            ax = 1f * notScaledDstWidth / srcWidth
            bx = -ax * ((notScaledDstWidth - srcWidth) / 2) / notScaledDstWidth
        } else {
            val notScaledDstHeight = (srcHeight * srcRatio / dstRatio).toInt()
            ay = 1f * notScaledDstHeight / srcHeight
            by = -ay * ((notScaledDstHeight - srcHeight) / 2) / notScaledDstHeight
        }

        for (k in 0 until detectionsCount) {
            val det = Detection(
                RectF(
                    ax * boxesTensor[k * 4 + 0] + bx,
                    ay * boxesTensor[k * 4 + 1] + by,
                    ax * boxesTensor[k * 4 + 2] + bx,
                    ay * boxesTensor[k * 4 + 3] + by,
                ),
                scoresTensor[k]
            )
            detections.add(det)
        }
        return detections
    }
}
