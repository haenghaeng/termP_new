package com.example.termp_new

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//import org.opencv.android.NativeCameraView.TAG
//import org.opencv.android.OpenCVLoader
import java.io.File

class ScanActivity : AppCompatActivity() {

    lateinit var save_btn : Button
    lateinit var reset_btn : Button
    lateinit var imageView: ImageView

    lateinit var photoFile : File

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

    // cache 디렉토리에 있는 cacheImageTemrP.jpg를 불러옴
    fun getImageFromCache(){
        photoFile = File(cacheDir, "cacheImageTemrP.jpg")
        if(photoFile.exists()){
            imageView.setImageURI(photoFile.toUri())
        }
    }
}