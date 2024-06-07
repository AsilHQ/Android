package com.duckduckgo.app.safegaze.genderdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.duckduckgo.app.browser.ml.Gender
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.DataType.FLOAT32
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.BILINEAR
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber

class GenderDetector (val context: Context) {
    private val inputImageSize = 128
    private val model: Gender  = Gender.newInstance(context)

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputImageSize, inputImageSize, BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setMinFaceSize(40f)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    fun predict(
        bitmap: Bitmap,
        onComplete: (GenderPrediction) -> Unit
    ) {
        val prediction = GenderPrediction()
        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image).addOnSuccessListener { faces ->
            prediction.faceCount = faces.size

            faces.forEach { face ->
                val subjectFace = cropToBBox(bitmap, face.boundingBox)
                val genderPredictions = getGenderPrediction(subjectFace)

                val isMale = genderPredictions[0] > genderPredictions[1]
                prediction.hasMale = true

                // Timber.d("kLog gender: Male $isMale ${genderPredictions.joinToString()}")

                if (!isMale) {
                    prediction.hasFemale = true
                    return@forEach
                }
            }

            onComplete(prediction)
        }.addOnFailureListener {
            Timber.e("kLog Exception while detecting faces: $it")

            onComplete(prediction)
        }
    }

    private fun getGenderPrediction(bitmap: Bitmap): FloatArray {
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, inputImageSize, inputImageSize, 3), DataType.FLOAT32)

        val buffer = TensorImage(FLOAT32).let {
            it.load(bitmap)
            imageProcessor.process(it)
        }.tensorBuffer.buffer

        inputFeature.loadBuffer(buffer)
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer

        return outputFeature.floatArray
    }

    fun dispose() {
        model.close()
        faceDetector.close()
    }

    private fun cropToBBox(image: Bitmap, boundingBox: Rect): Bitmap {
        // Ensure boundingBox coordinates are within the image bounds
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(image.width)
        val bottom = boundingBox.bottom.coerceAtMost(image.height)

        // Calculate the width and height of the cropped area
        val width = right - left
        val height = bottom - top

        // If the bounding box is completely out of bounds, return null or a default bitmap
        if (width <= 0 || height <= 0) {
            return image // return a default bitmap if needed
        }

        // Create and return the cropped bitmap
        return Bitmap.createBitmap(image, left, top, width, height)
    }
}
