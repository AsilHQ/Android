package com.duckduckgo.app.safegaze.genderdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.duckduckgo.app.browser.ml.Gender
import com.duckduckgo.common.utils.SAFE_GAZE_MIN_FACE_SIZE
import com.duckduckgo.common.utils.SAFE_GAZE_MIN_FEMALE_CONFIDENCE
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.DataType.FLOAT32
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.BILINEAR
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GenderDetector (val context: Context) {
    private val inputImageSize = 128
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(inputImageSize, inputImageSize, BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()
    val model by lazy { Gender.newInstance(context) }

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setMinFaceSize(80f)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    suspend fun predict(bitmap: Bitmap): GenderPrediction {

        return suspendCoroutine { continuation ->
            val prediction = GenderPrediction()
            val image = InputImage.fromBitmap(bitmap, 0)

            faceDetector.process(image).addOnSuccessListener { faces ->
                prediction.faceCount = faces.size

                for (i in 0 until faces.size) {
                    val face = faces[i]
                    val subjectFace = cropToBBox(bitmap, face.boundingBox) ?: continue

                    val currentTime = System.currentTimeMillis()
                    val genderPredictions = getGenderPrediction(subjectFace)
                    Timber.d("kLog elapsed time yolo8: ${System.currentTimeMillis() - currentTime}")

                    val isMale = genderPredictions.first > genderPredictions.second
                    prediction.hasMale = prediction.hasMale || isMale
                    prediction.maleConfidence = genderPredictions.first
                    prediction.femaleConfidence = genderPredictions.second

                    if (prediction.femaleConfidence >= SAFE_GAZE_MIN_FEMALE_CONFIDENCE) {
                        prediction.hasFemale = true
                        break
                    }
                }

                continuation.resume(prediction)
            }.addOnFailureListener {
                Timber.e("kLog Exception while detecting faces: $it")
                continuation.resume(prediction)
            }
        }
    }

    private fun getGenderPrediction(bitmap: Bitmap): Pair<Float, Float> {
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, inputImageSize, inputImageSize, 3), FLOAT32)

        val byteBuffer = TensorImage(FLOAT32).let {
            it.load(bitmap)
            imageProcessor.process(it)
        }.tensorBuffer.buffer

        inputFeature.loadBuffer(byteBuffer)
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer.floatArray

        return Pair(outputFeature[0], outputFeature[1])
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

    fun dispose() {
        model.close()
        faceDetector.close()
    }
}
