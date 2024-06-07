package com.example.termp_new

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity() {
    lateinit var galleryBtn : ImageButton
    lateinit var cameraBtn : ImageButton
    lateinit var functionBtn : ImageButton
    lateinit var previewView: PreviewView
    
    lateinit var photoFile : File

    val GALLERY_IMAGE_REQUEST_CODE = 1000

    // camera 구성에 필요한 변수
    lateinit var preview : Preview
    lateinit var cameraSelector : CameraSelector
    lateinit var imageCapture : ImageCapture
    lateinit var camera : Camera


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    /**
     * 초기화
     */
    private fun init(){
        // 버튼, 뷰 연결
        galleryBtn = findViewById(R.id.galleryBtn)
        cameraBtn = findViewById(R.id.cameraBtn)
        functionBtn = findViewById(R.id.functionBtn)
        previewView = findViewById(R.id.previewView)

        // 버튼에 리스너 설정
        galleryBtn.setOnClickListener{
            galleryBtnClick()
        }
        cameraBtn.setOnClickListener{
            cameraBtnClick()
        }
        functionBtn.setOnClickListener{
            functionBtnClick()
        }

        // 임시 디렉토리
        photoFile = File(applicationContext.cacheDir,"cacheImageTemrP.jpg")

        // 카메라 실행
        startCamera()

//        drawPolylines()
    }

    /**
     * 갤러리로 이동하여 사진을 선택함
     */
    private fun galleryBtnClick(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE)
    }

    /**
     * 현재 화면을 촬영함
     */
    private fun cameraBtnClick(){
//        /////// 임시 코드. 버퍼에 넣지 않고 Pictures/CameraX-Image에 바로 저장하게 설정함
//        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
//            .format(System.currentTimeMillis())
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        val outputFileOptions = ImageCapture.OutputFileOptions
//            .Builder(contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues)
//            .build()
        ///// 임시 코드
        
        // 기존 버퍼에 있던 이미지 제거
        photoFile.delete()

        // 촬영한 사진을 버퍼에 저장하게 설정
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException)
                {
                    // insert your code here.
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // ScanActivity 실행
                    startActivity(Intent(this@MainActivity, ResultActivity::class.java))
                }
            })


    }
    fun functionBtnClick(){
        // ?
        Toast.makeText(this, "function", Toast.LENGTH_SHORT).show()
    }


    /**
     * 카메라 실행 함수
     * https://developer.android.com/media/camera/camerax/preview?hl=ko
     */
    fun startCamera(){
        OpenCVLoader.initDebug()

        // 1. CameraProvider 지정
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // 2. CameraProvier에 리스너 추가
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }



    fun bindPreview(cameraProvider : ProcessCameraProvider) {
        preview = Preview.Builder()
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()

        preview.setSurfaceProvider(previewView.getSurfaceProvider())

        camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageCapture, preview)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == GALLERY_IMAGE_REQUEST_CODE) {
            if (data == null) return

            // 갤러리에서 읽은 파일을 cacheDir에 저장
            val selectedImage = data.data
            val contentResolver: ContentResolver = contentResolver
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                // Input stream from the URI
                inputStream = selectedImage?.let { contentResolver.openInputStream(it) }

                if (inputStream == null) {
                    throw IOException("Unable to open input stream from URI")
                }

                // File in the desired directory
                outputStream = FileOutputStream(photoFile)

                // Buffer for data transfer
                val buffer = ByteArray(1024)
                var bytesRead: Int

                // Transfer data from the input stream to the output stream
                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
            } finally {
                // Close streams to avoid memory leaks
                inputStream?.close()
                outputStream?.close()
            }

            // ScanActivity 실행
            startActivity(Intent(this@MainActivity, ResultActivity::class.java))
        }
    }

    fun drawPolylines(){
        // FrameLayout 위에 다각형을 그리는 CustomView 추가
        val customView: CustomView = CustomView(this)
        val frameLayout = findViewById<FrameLayout>(R.id.frameLayout)
        frameLayout.addView(customView)
    }

    // CustomView 클래스 정의
    private class CustomView(context: Context?) : AppCompatImageView(context!!) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 다각형 그리기
            val paint = Paint()
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f

            val path = Path()
            path.moveTo(100f, 100f) // 시작점
            path.lineTo(200f, 100f) // 두 번째 점
            path.lineTo(150f, 200f) // 세 번째 점
            path.lineTo(100f, 100f) // 시작점으로 되돌아오는 선(닫힌 다각형을 만들기 위해)

            canvas.drawPath(path, paint)
        }
    }

}