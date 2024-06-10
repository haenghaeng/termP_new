package com.example.termp_new

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.termp_new.fragment.PreferenceFragment
import com.example.termp_new.openCV.ScanDoc
import com.example.termp_new.openCV.ScanDoc.Companion.getOutlineFromSrc
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    // 버튼, 프리뷰, 레이아웃, 커스텀뷰
    lateinit var galleryBtn : ImageButton
    lateinit var cameraBtn : ImageButton
    lateinit var functionBtn : ImageButton
    lateinit var previewView: PreviewView    
    lateinit var frameLayout : FrameLayout
    lateinit var customView : CustomView

    val GALLERY_IMAGE_REQUEST_CODE = 1000

    // camera 구성에 필요한 변수
    lateinit var preview : Preview
    lateinit var cameraSelector : CameraSelector
    lateinit var imageCapture : ImageCapture
    lateinit var camera : Camera

    // 화면 또는 사진에서 추출한 문서의 꼭지점
    lateinit var points : ArrayList<Point>
    // 추출된 문서의 bitmap
    lateinit var resultBitmap : Bitmap
    // 추출한 bitmap을 저장할 임시파일 : cacheDir
    lateinit var tempFile : File

    // 현재 설정창이 열려 있으면 true
    var isPref = false

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init(){
        // 버튼, 뷰, 레이아웃 연결
        galleryBtn = findViewById(R.id.galleryBtn)
        cameraBtn = findViewById(R.id.cameraBtn)
        functionBtn = findViewById(R.id.functionBtn)
        previewView = findViewById(R.id.previewView)
        frameLayout = findViewById(R.id.frameLayout)

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

        // 임시 파일의 경로와 이름 지정
        tempFile = File(applicationContext.cacheDir,"cacheImageTermP.jpg")

        // 카메라 실행
        startCamera()

        // 새로운 customView를 만들고 FrameLayout에 추가
        customView = CustomView(this)
        frameLayout.addView(customView)

        // 주기적으로 함수를 호출 하기 위한 핸들러 관련 변수
        val mHandler = android.os.Handler(Looper.getMainLooper())
        val mRunnable: Runnable?
        val mInterval: Long = 100 // 0.1초 간격(milisecond 단위)

        // 'mInterval = 0.1초' 간격 마다 외곽선을 그림
        mRunnable = object : Runnable {
            override fun run() {
                // previeView가 준비되면 아래의 함수가 계속 호출됨
                if(previewView.bitmap != null){
                    // 현재 화면에서 외곽선 추출을 시도함
                    points = getOutlineFromSrc(previewView.bitmap!!)

                    // 외곽선을 그림
                    drawOutline()
                }
                // 다음 호출 예약
                mHandler.postDelayed(this, mInterval)
            }
        }
        // 초기 호출
        mHandler.postDelayed(mRunnable as Runnable, mInterval)
    }

    /**
     * 갤러리로 이동하여 사진을 선택함
     */
    private fun galleryBtnClick(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE)
    }

    /**
     * 현재 preview에 있는 화면에서 문서를 추출할 수 있다면 추출하여 ResultActivity로 보냄
     */
    private fun cameraBtnClick(){  
        // 문서를 인식 했는지 확인
        if(points.isEmpty()){
            Toast.makeText(this, "네 모서리가 다 보이게 어두운 배경에서 촬영해주세요!", Toast.LENGTH_SHORT).show()
        }
        else{
            // 문서를 추출
            resultBitmap = previewView.bitmap?.let { ScanDoc.makeDocFromImage(it, points) }!!

            // resultBitmap을 ResultActivity로 보내기 위해 CacheDir에 저장
            // Bitmap은 크기가 커서 Intent에 담아서 보내면 안 좋다고 함
            saveBitmapAtCacheDir(resultBitmap)

            // ResultActivity 실행
            startActivity(Intent(this@MainActivity, ResultActivity::class.java))
        }
    }

    /**
     * 설정창을 열고 닫음
     */
    private fun functionBtnClick(){
        // 설정창이 열려있으면 닫음
        if(isPref){
            isPref = false
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                supportFragmentManager.beginTransaction()
                    .remove(it)
                    .commit()
            }
        }
        // 설정창이 닫혀있으면 열어줌
        else{
            isPref = true
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, PreferenceFragment())
                .addToBackStack(null)
                .commit()
        }
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

            // 갤러리에서 읽은 파일에서 bitmap을 가져옴
            val inputStream = data.data?.let { contentResolver.openInputStream(it) }
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // 갤러리에서 읽은 파일에서 외곽선 추출 시도
            points = getOutlineFromSrc(bitmap)
            
            // 외곽선 추출에 실패했는지 확인
            if(points.isEmpty()){
                Toast.makeText(this, "불러온 그림에서 문서를 인식할 수 없었습니다.", Toast.LENGTH_SHORT).show()
            }
            else{
                // resultBitmap을 만듬
                resultBitmap = bitmap?.let { ScanDoc.makeDocFromImage(it, points) }!!

                // resultBitmap을 ResultActivity로 보내기 위해 CacheDir에 저장
                // Bitmap은 크기가 커서 Intent에 담아서 보내면 안 좋다고 함
                saveBitmapAtCacheDir(resultBitmap)

                // ResultActivity 실행
                startActivity(Intent(this@MainActivity, ResultActivity::class.java))
            }
        }
    }

    /**
     * 외곽선 그리는 함수
     */
    fun drawOutline(){
        // FrameLayout에 있던 기존 CustomView는 제거
        if(customView != null){
            frameLayout.removeView(customView)
        }  

        // 새로운 customView 지정, points도 넣어줌
        customView = CustomView(this)
        customView.points = points
        
        // FrameLayout에 customView 추가
        frameLayout.addView(customView)
    }

    /**
     * previewView 바로 위에는 선을 그릴 수 없다고 함
     * 그래서 CustomView를 프리뷰 위에 덮어씌워서 CustomView에 선을 그림
     */
    class CustomView(context: Context?) : AppCompatImageView(context!!) {
        var paint = Paint()
        var path = Path()
        var points = ArrayList<Point>()
        init {
            // 선의 색, 스타일, 굵기 지정
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 외곽선 추출에 실패하였다면 선을 그리지 않고 리턴함
            if(points.isEmpty()) return
            
            // 화면에서 인식한 문서의 각 정점을 기준으로 path 설정
            path.moveTo(points[0].x.toFloat(), points[0].y.toFloat()) // 0 : 시작점
            path.lineTo(points[1].x.toFloat(), points[1].y.toFloat()) // 0-1
            path.lineTo(points[2].x.toFloat(), points[2].y.toFloat()) // 1-2
            path.lineTo(points[3].x.toFloat(), points[3].y.toFloat()) // 2-3
            path.lineTo(points[0].x.toFloat(), points[0].y.toFloat()) // 3-1 : 시작점으로 되돌아오는 선
            
            // path를 따라 선 그림
            canvas.drawPath(path, paint)
        }
    }

    /**
     * resultBitmap에 있는 비트맵을 캐시 디렉토리에 저장함
     */
    fun saveBitmapAtCacheDir(bitmap: Bitmap){
        val outputStream = FileOutputStream(tempFile)
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }
}