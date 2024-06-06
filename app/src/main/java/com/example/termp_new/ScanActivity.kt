package com.example.termp_new

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

class ScanActivity : AppCompatActivity() {

    lateinit var saveBtn : Button
    lateinit var resetBtn : Button
    lateinit var imageView: ImageView

    lateinit var photoFile : File

    lateinit var photoBitmap: Bitmap
    lateinit var photoBitmapResult : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // 버튼, 뷰 연결
        saveBtn = findViewById(R.id.saveBtn)
        resetBtn = findViewById(R.id.resetBtn)
        imageView = findViewById(R.id.imageView)
        
        // 버튼에 리스너 설정
        saveBtn.setOnClickListener{
            saveBtnClick()
        }
        resetBtn.setOnClickListener{
            resetBtnClick()
        }

        // CacheDir에서 이미지를 가져옴
        getImageFromCache()

//        if (!OpenCVLoader.initDebug()) {
//            Log.e(TAG, "OpenCV 초기화 실패!")
//        } else {
//            Log.d(TAG, "OpenCV 초기화 성공!!!!!")
//        }
    }

    /**
     * cache 디렉토리에 있는 cacheImageTemrP.jpg를 불러와 Bitmap에 저장
     */
    private fun getImageFromCache(){
        photoFile = File(cacheDir, "cacheImageTemrP.jpg")
        if(photoFile.exists()){
            imageView.setImageURI(photoFile.toUri())

            val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(contentResolver, photoFile.toUri())
            } else {
                TODO("VERSION.SDK_INT < P")
            }
            photoBitmap = ImageDecoder.decodeBitmap(src).copy(Bitmap.Config.ARGB_8888, true)
            
            // 이미지에서 문서만 추출
            getDocFromImage()
        }
    }

    /**
    이미지에서 문서를 추출함
     */
    private fun getDocFromImage(){
        // Bitmap을 OpenCV의 Mat으로 변환
        val src = Mat()
        val bmp32: Bitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32, src)

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

        if (biggestContour == null) {
            // 오류를 내면 안되고 toast 메세지를 띄운 뒤 다시 MainActivity로 돌아가야 함
            throw IllegalArgumentException("No Contour")
        }
        // 너무 작아도 안됨
        if (biggestContourArea < 400) {
            // 오류를 내면 안되고 toast 메세지를 띄운 뒤 다시 MainActivity로 돌아가야 함
            throw IllegalArgumentException("too small")
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

        // 사각형 판별
        if (approxCandidate.rows() != 4) {
            // 오류를 내면 안되고 toast 메세지를 띄운 뒤 다시 MainActivity로 돌아가야 함
            throw java.lang.IllegalArgumentException("It's not rectangle %d".format(approxCandidate.rows()))
        }

        // 컨벡스(볼록한 도형)인지 판별
        if (!Imgproc.isContourConvex(MatOfPoint(*approxCandidate.toArray()))) {
            // 오류를 내면 안되고 toast 메세지를 띄운 뒤 다시 MainActivity로 돌아가야 함
            throw java.lang.IllegalArgumentException("It's not convex")
        }

        // 좌상단부터 시계 반대 방향으로 정점을 정렬한다.
        val points = arrayListOf(
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

        // dst와 크기가 같은 Bitmap 생성
        photoBitmapResult = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);

        // 결과를 Mat에서 Bitmap으로 변경
        Utils.matToBitmap(dst, photoBitmapResult)

        // ImageView에 결과를 띄움
        imageView.setImageBitmap(photoBitmapResult)

        Toast.makeText(this@ScanActivity, "으헝 됬다 됬어", Toast.LENGTH_SHORT).show()
    }

    /**
    * 사각형 꼭짓점 정보로 사각형 최대 사이즈 구하기
    * 평면상 두 점 사이의 거리는 직각삼각형의 빗변길이 구하기와 동일
    */
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
     * 현재 ImageView에 띄워진 이미지를 저장합니다.
     */
    private fun saveBtnClick(){

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        var outputStream: OutputStream? = null

        try {
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    photoBitmapResult.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
            }

            // 메세지 띄움
            Toast.makeText(this, "저장되었습니다!", Toast.LENGTH_SHORT).show()

            // MainActivity 실행
            startActivity(Intent(this@ScanActivity, MainActivity::class.java))
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            outputStream?.close()
        }
    }

    /**
     * 현재 ImageView에 있는 사진을 저장하지 않고 MainActivity로 돌아갑니다.
     */
    fun resetBtnClick(){
        // 메세지 띄움
        Toast.makeText(this, "저장하지 않고 돌아왔습니다!", Toast.LENGTH_SHORT).show()

        // MainActivity 실행
        startActivity(Intent(this@ScanActivity, MainActivity::class.java))
    }

}