package com.example.mytensortest.ui

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytensortest.Detection
import com.example.mytensortest.ModelConfiguration
import com.example.mytensortest.TFDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DetectorViewModel : ViewModel() {
    val bitmapLiveData = MutableLiveData<Bitmap>()
    val progressLiveData = MutableLiveData<ProgressStatus>()

    fun startDetect(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            progressLiveData.postValue(Loading)
            val bitmap = getBitmap(uri, context)
            runObjectDetection(bitmap, context.assets, context.cacheDir.path)
            progressLiveData.postValue(Success)
        }
    }

    private fun getBitmap(uri: Uri, context: Context): Bitmap {
        return if (Build.VERSION.SDK_INT < 28) {
            var bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            var bitmap = ImageDecoder.decodeBitmap(source)
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap
        }
    }

    private fun runObjectDetection(bitmap: Bitmap, asset: AssetManager, cacheDirPath: String) {
        val configuration =
            ModelConfiguration(DetectorFragment.IMAGE_HEIGHT, DetectorFragment.IMAGE_WIDTH)
        val detector = TFDetector(asset, configuration, cacheDirPath)
        val result = detector.detect(bitmap)
        val bitmapResult = drawDetectionResult(bitmap, result)
        bitmapLiveData.postValue(bitmapResult)
        detector.close()
    }

    private fun drawDetectionResult(bitmap: Bitmap, detectionResults: List<Detection>): Bitmap {
        val canvas = Canvas(bitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT
        detectionResults.forEach {
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = RectF(
                it.rectF.left * bitmap.width,
                it.rectF.top * bitmap.height,
                it.rectF.right * bitmap.width,
                it.rectF.bottom * bitmap.height
            )
            canvas.drawRect(box, pen)
        }
        return bitmap
    }
}