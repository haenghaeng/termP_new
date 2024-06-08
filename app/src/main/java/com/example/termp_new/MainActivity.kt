package com.example.termp_new

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var galleryBtn : ImageButton
    lateinit var cameraBtn : ImageButton
    lateinit var functionBtn : ImageButton
    lateinit var previewView: PreviewView
    
    lateinit var photoFile : File

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

    fun init(){
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

    fun gallery_btn_click(){
        // 갤러리로 이동, 사진 선택
        Toast.makeText(this, "gallery", Toast.LENGTH_SHORT).show()
    }
    fun camera_btn_click(){
        // 현재 화면 촬영

        /////// 임시 코드. 버퍼에 넣지 않고 Pictures/CameraX-Image에 바로 저장하게 설정함
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
                    // insert your code here.

                    // 촬영한 사진을 굳이 ScanActivity로 보낼 필요가 있을까?
                    // 버퍼에 있으니까 ScanActivity에서 버퍼에 있는 걸 꺼내 쓰면 안됨?
                    
                    // ScanActivity 실행
                    startActivity(Intent(this@MainActivity, ScanActivity::class.java))

//                    Toast.makeText(this@MainActivity, "사진 찍음요!", Toast.LENGTH_SHORT).show()
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



}