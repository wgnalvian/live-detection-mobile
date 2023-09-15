package com.example.testai

import java.util.*

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.example.testai.ml.Detect
import com.example.testai.ml.Detect10
import com.example.testai.ml.New
import org.tensorflow.lite.support.image.TensorImage


import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import java.util.Vector
import java.util.logging.Logger
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model: New
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest
    lateinit var handler: Handler
    lateinit var handleThread: HandlerThread
    lateinit var capReq: CaptureRequest.Builder
    lateinit var imageReader: ImageReader
    lateinit var launcher: ActivityResultLauncher<String>

    val REQUEST_CODE_PICK_IMAGE = 100
    private var boundingBoxRect: RectF? = null
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
        textSize = 90f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//         nativeWindowWrapper = NativeWindowWrapper(this)
        setContentView(R.layout.activity_main)
        getPermission()
        textureView = findViewById<TextureView>(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handleThread = HandlerThread("videoThread")
        handleThread.start()
        handler = Handler((handleThread).looper)

        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(p0: ImageReader?) {

                var image = p0?.acquireLatestImage()
                var buffer = image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "img.jpeg")
                var opStream = FileOutputStream(file)
                opStream.write(bytes)
                opStream.close()
                image.close()

                val bitmap = BitmapFactory.decodeFile(file.path)


            }

        }, handler)





        findViewById<Button>(R.id.button).apply {
            setOnClickListener {
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(), null, null)
            }


        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
                model = New.newInstance(applicationContext)
                boundingBoxRect = RectF(100f, 100f, 300f, 300f)

// Dalam onSurfaceTextureAvailable atau tempat lain yang sesuai
//                val canvas = boundingBoxView.lockCanvas()
//                if (canvas != null) {
//                    boundingBoxRect?.let { rect ->
//                        canvas.drawRect(rect, paint)
//                    }
//                    boundingBoxView.unlockCanvasAndPost(canvas)
//                }

            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {


// Creates inputs for reference.
                val image = TensorImage.fromBitmap(textureView.getBitmap())

// Runs model inference and gets result.
                val outputs = model.process(image)
                val detectionResult = outputs.detectionResultList.get(0)

// Gets result from DetectionResult.
                val conf = detectionResult.scoreAsFloat;
                val location = detectionResult.locationAsRectF;
                val score = detectionResult.categoryAsString;

                               val boundingBoxView = findViewById<com.example.testai.BoundingBoxView>(R.id.boundingBoxView)
// Releases model resources if no longer used.

                if(conf > 0.8){

                    boundingBoxView.setBoundingBox(
                        location.left ,
                        location.top ,
                        location.right ,
                        location.bottom,
                        score
                    )

                    findViewById<Button>(R.id.button2).text = score
                }else{
                    boundingBoxView.setBoundingBox(
                       0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat(),""
                    )
                }

            }

        }




        findViewById<Button>(R.id.button2).apply {
            setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                // Mulai aktivitas pemilihan gambar
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)


            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            // Dapatkan Uri gambar yang dipilih
            val uri: Uri? = data?.data

            if (uri != null) {

                // Dapatkan gambar dari Uri
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    cameraDevice = p0
                    capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    var surface = Surface(textureView.surfaceTexture)
                    capReq.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface, imageReader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                cameraCaptureSession = p0
                                cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                            }

                        },
                        handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {
                    TODO("Not yet implemented")
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    TODO("Not yet implemented")
                }

            },
            handler
        )
    }

    fun getPermission() {
        var permissionList = mutableListOf<String>()

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.CAMERA
        )
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice.close()
        handleThread.quitSafely()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }
    }
}

