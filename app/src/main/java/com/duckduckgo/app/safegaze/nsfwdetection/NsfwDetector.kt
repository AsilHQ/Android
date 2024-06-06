package com.duckduckgo.app.safegaze.nsfwdetection

import android.content.Context
import android.graphics.Bitmap
import com.duckduckgo.app.browser.ml.Gender
import com.duckduckgo.app.browser.ml.Nsfw
import org.tensorflow.lite.DataType
import org.tensorflow.lite.DataType.FLOAT32
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.BILINEAR
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber

class NsfwDetector(val context: Context) {
    private val labels = listOf("drawing", "hentai", "neutral", "porn", "sexy")
    private val inputImageSize = 224

    var model: Nsfw  = Nsfw.newInstance(context)
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputImageSize, inputImageSize, NEAREST_NEIGHBOR))
        .add(NormalizeOp(0f, 255f))
        .build()

    fun isNsfw(bitmap: Bitmap): Boolean {
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, inputImageSize, inputImageSize, 3), DataType.FLOAT32)

        val buffer = TensorImage(FLOAT32).let {
            it.load(bitmap)
            imageProcessor.process(it)
        }.tensorBuffer.buffer

        inputFeature.loadBuffer(buffer)
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer
        val predictionArray = outputFeature.floatArray

        /*val maxIndex = predictionArray.indices.maxByOrNull { i -> predictionArray[i] } ?: -1
        val predictedLabel = if (maxIndex != -1 && maxIndex < labels.size) {
            labels[maxIndex]
        } else {
            "Unknown"
        }*/

        // hentai + porn + sexy
        return predictionArray[1] + predictionArray[3] + predictionArray[4] > 0.5
    }

    fun dispose() {
        model.close()
    }

}
