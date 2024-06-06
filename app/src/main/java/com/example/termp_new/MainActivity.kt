package com.example.termp_new

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            gallery_btn_click()
        }
        cameraBtn.setOnClickListener{
            camera_btn_click()
        }
        functionBtn.setOnClickListener{
            function_btn_click()
        }

        // 임시 디렉토리
        photoFile = File(applicationContext.cacheDir,"cacheImageTemrP.jpg")

        // 카메라 실행
        startCamera()
    }

    /**
     * 갤러리로 이동하여 사진을 선택함
     */
    private fun gallery_btn_click(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE)
    }

    /**
     * 현재 화면을 촬영함
     */
    private fun camera_btn_click(){
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
                    startActivity(Intent(this@MainActivity, ScanActivity::class.java))
                }
            })


    }
    fun function_btn_click(){
        // ?
        Toast.makeText(this, "function", Toast.LENGTH_SHORT).show()
    }


    /**
     * 카메라 실행 함수
     * https://developer.android.com/media/camera/camerax/preview?hl=ko
     */
    fun startCamera(){
        OpenCVLoader.initDebug()

        // 1. CameraProvider 요청
        // ProcessCameraProvider는 Camera의 생명주기를 LifeCycleOwner의 생명주기에 Binding 함
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // 2. CameraProvier 사용 가능 여부 확인
        cameraProviderFuture.addListener(Runnable {
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
            startActivity(Intent(this@MainActivity, ScanActivity::class.java))
        }
    }
}