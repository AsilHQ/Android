package com.duckduckgo.app.safegaze.ondeviceobjectdetection

import android.content.Context
import android.graphics.Bitmap
import com.duckduckgo.app.browser.ml.DetectPerson
import org.tensorflow.lite.support.image.TensorImage

/**
 * Created by Asif Ahmed on 2/1/24.
 */

class ObjectDetectionHelper(val context: Context) {

    var model: DetectPerson = DetectPerson.newInstance(context)

    fun isImageContainsHuman(bitmap: Bitmap): Boolean {
        val image = TensorImage.fromBitmap(bitmap)
        val outputs = model.process(image)
        val detectionsList = outputs.detectionResultList

        return detectionsList.any { detection ->
            detection.categoryAsString.equals("person", ignoreCase = true)
        }
    }

    fun dispose() {
        model.close()
    }
}
