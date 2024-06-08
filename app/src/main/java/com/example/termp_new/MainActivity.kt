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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    // 버튼, 프리뷰, 레이아웃, 커스텀뷰
    lateinit var galleryBtn : ImageButton
    lateinit var cameraBtn : ImageButton
    lateinit var functionBtn : ImageButton
    lateinit var previewView: PreviewView    
    lateinit var frameLayout : FrameLayout
    lateinit var customView : CustomView
    
    // 추출한 bitmap을 저장할 임시파일
    lateinit var tempFile : File

    val GALLERY_IMAGE_REQUEST_CODE = 1000

    // camera 구성에 필요한 변수
    lateinit var preview : Preview
    lateinit var cameraSelector : CameraSelector
    lateinit var imageCapture : ImageCapture
    lateinit var camera : Camera

    // 주기적으로 함수를 호출 하기 위한 핸들러 관련 변수
    var mHandler = android.os.Handler(Looper.getMainLooper())
    var mRunnable: Runnable? = null
    val mInterval: Long = 100 // 0.1초 간격(milisecond 단위)

    // CustomView class에서도 사용하므로 전역변수로 지정함
    companion object {
        // 외곽선을 만드는 데 성공했는지, 실패했는지 확인하는 변수        
        var failToMakeOutlines = true

        // 화면 또는 사진에서 추출한 문서의 꼭지점
        lateinit var points : ArrayList<Point>
    }
    
    // 외곽선 추출에 사용하는 변수
    lateinit var src : Mat // 추출 전 사진의 Mat
    lateinit var resultBitmap : Bitmap // 최종적으로 추출된 문서사진의 bitmap
    
    
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

        // 새로운 customView 지정
        customView = CustomView(this)

        // FrameLayout에 customView 추가
        frameLayout.addView(customView)

        // 'mInterval = 0.1초' 간격마다 run 내부의 함수를 호출함
        mRunnable = object : Runnable {
            override fun run() {
                // previeView가 준비되면 아래의 함수가 계속 호출됨
                if(previewView.bitmap != null){
                    // 현재 화면에서 외곽선 추출을 시도함
                    getOutlineFromSrc(previewView.bitmap!!)

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
        // 문서를 인식하였다면 진행
        if(!failToMakeOutlines){
            // 문서를 추출
            makeDocFromImage()

            // resultBitmap을 ResultActivity로 보내기 위해 CacheDir에 저장
            // Bitmap은 크기가 커서 Intent에 담아서 보내면 안 좋다고 함
            saveBitmapAtCacheDir()

            // ResultActivity 실행
            startActivity(Intent(this@MainActivity, ResultActivity::class.java))
        }
    }

    private fun functionBtnClick(){
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

            // 읽은 파일에서 bitmap을 가져옴
            val inputStream = data.data?.let { contentResolver.openInputStream(it) }
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // 갤러리에서 읽은 파일에서 외곽선 추출 시도
            getOutlineFromSrc(bitmap)
            
            // 외곽선 추출에 실패했는지 확인
            if(failToMakeOutlines){
                Toast.makeText(this, "불러온 그림에서 문서를 인식할 수 없었습니다.", Toast.LENGTH_SHORT).show()
            }
            else{
                // resultBitmap을 만듬
                makeDocFromImage()

                // resultBitmap을 ResultActivity로 보내기 위해 CacheDir에 저장
                // Bitmap은 크기가 커서 Intent에 담아서 보내면 안 좋다고 함
                saveBitmapAtCacheDir()

                // ResultActivity 실행
                startActivity(Intent(this@MainActivity, ResultActivity::class.java))
            }
        }
    }

    fun getOutlineFromSrc(srcBitmap : Bitmap){        
        // Bitmap을 OpenCV의 Mat으로 변환
        src = Mat()
        Utils.bitmapToMat(srcBitmap, src)

        // 흑백영상으로 전환
        val graySrc = Mat()
        Imgproc.cvtColor(src, graySrc, Imgproc.COLOR_BGR2GRAY)

        // 이진화
        val binarySrc = Mat()
        Imgproc.threshold(graySrc, binarySrc, 0.0, 255.0, Imgproc.THRESH_OTSU)

        // 윤곽선 찾기
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            binarySrc,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_NONE
        )

        // 가장 면적이 큰 윤곽선 찾기
        var biggestContour: MatOfPoint? = null
        var biggestContourArea = 0.0
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > biggestContourArea) {
                biggestContour = contour
                biggestContourArea = area
            }
        }

        failToMakeOutlines = false

        // 외곽선이 있는지 확인
        if (biggestContour == null) {
            failToMakeOutlines = true
            return
        }
        // 외곽선이 너무 작은지 확인
        if (biggestContourArea < 400) {
            failToMakeOutlines = true
            return
        }

        // 근사화 하여 꼭지점 만들기
        val candidate2f = MatOfPoint2f(*biggestContour.toArray())
        val approxCandidate = MatOfPoint2f()
        Imgproc.approxPolyDP(
            candidate2f,
            approxCandidate,
            Imgproc.arcLength(candidate2f, true) * 0.02,
            true
        )

        // 사각형인지 확인
        if (approxCandidate.rows() != 4) {
            failToMakeOutlines = true
            return
        }

        // 컨벡스(볼록한 도형)인지 판별
        if (!Imgproc.isContourConvex(MatOfPoint(*approxCandidate.toArray()))) {
            failToMakeOutlines = true
            return
        }

        // 좌상단부터 시계 반대 방향으로 정점을 정렬
        points = arrayListOf(
            Point(approxCandidate.get(0, 0)[0], approxCandidate.get(0, 0)[1]),
            Point(approxCandidate.get(1, 0)[0], approxCandidate.get(1, 0)[1]),
            Point(approxCandidate.get(2, 0)[0], approxCandidate.get(2, 0)[1]),
            Point(approxCandidate.get(3, 0)[0], approxCandidate.get(3, 0)[1]),
        )
        points.sortBy { it.x } // x좌표 기준으로 먼저 정렬

        if (points[0].y > points[1].y) {
            val temp = points[0]
            points[0] = points[1]
            points[1] = temp
        }

        if (points[2].y < points[3].y) {
            val temp = points[2]
            points[2] = points[3]
            points[3] = temp
        }
    }

    fun drawOutline(){
        // FrameLayout에 있던 기존 CustomView는 제거
        if(customView != null){
            frameLayout.removeView(customView)
        }  

        // 새로운 customView 지정
        customView = CustomView(this)
        
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
        init {
            // 선의 색, 스타일, 굵기 지정
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 외곽선 추출에 실패하였다면 선을 그리지 않고 리턴함
            if(failToMakeOutlines) return
            
            // 화면에서 인식한 문서의 각 정점을 기준으로 path 설정
            path.moveTo(points.get(0).x.toFloat(), points.get(0).y.toFloat()) // 0 : 시작점
            path.lineTo(points.get(1).x.toFloat(), points.get(1).y.toFloat()) // 0-1
            path.lineTo(points.get(2).x.toFloat(), points.get(2).y.toFloat()) // 1-2
            path.lineTo(points.get(3).x.toFloat(), points.get(3).y.toFloat()) // 2-3
            path.lineTo(points.get(0).x.toFloat(), points.get(0).y.toFloat()) // 3-1 : 시작점으로 되돌아오는 선
            
            // path를 따라 선 그림
            canvas.drawPath(path, paint)
        }
    }

    /**
     * previewView에서 points에 저장된 정점을 기준으로 문서를 추출함
     */
    fun makeDocFromImage(){
        // 원본 영상 내 정점들
        val srcQuad = MatOfPoint2f().apply { fromList(points) }

        val maxSize = calculateMaxWidthHeight(
            tl = points[0],
            bl = points[1],
            br = points[2],
            tr = points[3]
        )
        val dw = maxSize.width
        val dh = dw * maxSize.height/maxSize.width
        val dstQuad = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(0.0, dh),
            Point(dw, dh),
            Point(dw, 0.0)
        )
        // 투시변환 매트릭스 구하기
        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcQuad, dstQuad)

        // 투시변환 된 결과 영상 얻기
        val dst = Mat()
        Imgproc.warpPerspective(src, dst, perspectiveTransform, Size(dw, dh))

        // resultBitmap를 dst와 크기가 같은 Bitmap 으로 설정
        resultBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);

        // 결과를 Mat에서 Bitmap으로 변경
        Utils.matToBitmap(dst, resultBitmap)
    }

    private fun calculateMaxWidthHeight(tl:Point, tr:Point, br:Point, bl:Point): Size {
        // Calculate width
        val widthA = sqrt((tl.x - tr.x) * (tl.x - tr.x) + (tl.y - tr.y) * (tl.y - tr.y))
        val widthB = sqrt((bl.x - br.x) * (bl.x - br.x) + (bl.y - br.y) * (bl.y - br.y))
        val maxWidth = max(widthA, widthB)
        // Calculate height
        val heightA = sqrt((tl.x - bl.x) * (tl.x - bl.x) + (tl.y - bl.y) * (tl.y - bl.y))
        val heightB = sqrt((tr.x - br.x) * (tr.x - br.x) + (tr.y - br.y) * (tr.y - br.y))
        val maxHeight = max(heightA, heightB)
        return Size(maxWidth, maxHeight)
    }

    /**
     * resultBitmap에 있는 비트맵을 캐시 디렉토리에 저장함
     */
    fun saveBitmapAtCacheDir(){
        val outputStream = FileOutputStream(tempFile)
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }
}