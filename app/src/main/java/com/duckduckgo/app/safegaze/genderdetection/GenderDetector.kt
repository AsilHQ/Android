package com.duckduckgo.app.safegaze.genderdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.duckduckgo.app.browser.ml.FaceModelV5
import com.duckduckgo.app.browser.ml.Gender
import com.duckduckgo.common.utils.SAFE_GAZE_MIN_FACE_SIZE
import com.duckduckgo.common.utils.SAFE_GAZE_MIN_FEMALE_CONFIDENCE
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
    private val v5Model = FaceModelV5.newInstance(context)


    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputImageSize, inputImageSize, BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()
    private val imageProcessor2 = ImageProcessor.Builder()
        .add(ResizeOp(80, 80, BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setMinFaceSize(80f)
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

            // convert to regular for loop
            for (i in 0 until faces.size) {
                val face = faces[i]
                val subjectFace = cropToBBox(bitmap, face.boundingBox) ?: continue
                val genderPredictions = getGenderPredictionV5(subjectFace)

                val isMale = genderPredictions.first > genderPredictions.second
                prediction.hasMale = prediction.hasMale || isMale
                prediction.maleConfidence = genderPredictions.first
                prediction.femaleConfidence = genderPredictions.second

                if (prediction.femaleConfidence >= SAFE_GAZE_MIN_FEMALE_CONFIDENCE) {
                    prediction.hasFemale = true
                    break
                }
            }

            onComplete(prediction)
        }.addOnFailureListener {
            Timber.e("kLog Exception while detecting faces: $it")

            onComplete(prediction)
        }
    }

    private fun getGenderPrediction(bitmap: Bitmap): Triple<Float, Float, Boolean> {
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, inputImageSize, inputImageSize, 3), DataType.FLOAT32)

        val buffer = TensorImage(FLOAT32).let {
            it.load(bitmap)
            imageProcessor.process(it)
        }.tensorBuffer.buffer

        inputFeature.loadBuffer(buffer)
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer

        return Triple(outputFeature.floatArray[0], outputFeature.floatArray[1],false)
    }

    private fun getGenderPredictionV5(bitmap: Bitmap): Triple<Float, Float, Boolean> {
        // val currentTime = System.currentTimeMillis()

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 80, 80, 3), FLOAT32)
        val byteBuffer = TensorImage(FLOAT32).let {
            it.load(bitmap)
            imageProcessor2.process(it)
        }.tensorBuffer.buffer
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = v5Model.process(inputFeature0)
        val ageFeature = outputs.outputFeature0AsTensorBuffer
        val genderFeature = outputs.outputFeature1AsTensorBuffer

        // get index of max value from ageFeature
        val maxIndex = ageFeature.floatArray.indices.maxByOrNull { ageFeature.floatArray[it] } ?: 0
        val isAdult = maxIndex > 0

        // Timber.d("kLog elapsed time v2: ${System.currentTimeMillis() - currentTime}")

        return Triple(genderFeature.floatArray[1], genderFeature.floatArray[0], isAdult)
    }

    fun dispose() {
        model.close()
        v5Model.close()
        faceDetector.close()
    }

    private fun cropToBBox(image: Bitmap, boundingBox: Rect): Bitmap? {
        // Ensure boundingBox coordinates are within the image bounds
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(image.width)
        val bottom = boundingBox.bottom.coerceAtMost(image.height)

        // Calculate the width and height of the cropped area
        val width = right - left
        val height = bottom - top

        if (width < SAFE_GAZE_MIN_FACE_SIZE || height < SAFE_GAZE_MIN_FACE_SIZE) {
            return null
        }

        // Create and return the cropped bitmap
        return Bitmap.createBitmap(image, left, top, width, height)
    }
}
