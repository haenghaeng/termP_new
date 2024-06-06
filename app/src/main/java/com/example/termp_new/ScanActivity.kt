package com.example.termp_new

//import org.opencv.android.NativeCameraView.TAG
//import org.opencv.android.OpenCVLoader
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import java.io.File

class ScanActivity : AppCompatActivity() {

    lateinit var save_btn : Button
    lateinit var reset_btn : Button
    lateinit var imageView: ImageView

    lateinit var photoFile : File

    lateinit var photoBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        save_btn = findViewById(R.id.saveBtn)
        reset_btn = findViewById(R.id.resetBtn)
        imageView = findViewById(R.id.imageView)

        getImageFromCache()

//        if (!OpenCVLoader.initDebug()) {
//            Log.e(TAG, "OpenCV 초기화 실패!")
//        } else {
//            Log.d(TAG, "OpenCV 초기화 성공!!!!!")
//        }
    }

    // cache 디렉토리에 있는 cacheImageTemrP.jpg를 불러와 Bitmap에 저장
    fun getImageFromCache(){
        photoFile = File(cacheDir, "cacheImageTemrP.jpg")
        if(photoFile.exists()){
            imageView.setImageURI(photoFile.toUri())

            val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(contentResolver, photoFile.toUri())
            } else {
                TODO("VERSION.SDK_INT < P")
            }
            photoBitmap = ImageDecoder.decodeBitmap(src).copy(Bitmap.Config.ARGB_8888, true)
        }
    }
}