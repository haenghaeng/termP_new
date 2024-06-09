package com.example.termp_new

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.example.termp_new.fragment.ImageFragment
import com.example.termp_new.fragment.SummarizeFragment
import com.example.termp_new.fragment.TextFragment
import com.example.termp_new.openAi.Image_to_text
import com.example.termp_new.openAi.TextClassify
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.coroutines.resume

class ResultActivity : AppCompatActivity() {

    lateinit var saveBtn : Button
    lateinit var resetBtn : Button
    lateinit var viewPager: ViewPager2
    lateinit var adapter : MyPagerAdapter

    lateinit var tempFile : File

    var foldername = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/File"
    var filename= "file.txt"
    var filename_gpt="summarized_file.txt"

    lateinit var resultText : FirebaseVisionText
    lateinit var resultGPT : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 버튼, 뷰페이저 연결
        saveBtn = findViewById(R.id.saveBtn)
        resetBtn = findViewById(R.id.resetBtn)
        viewPager = findViewById(R.id.viewPager)

        // 버튼에 리스너 설정
        saveBtn.setOnClickListener{
            saveBtnClick()
        }
        resetBtn.setOnClickListener{
            resetBtnClick()
        }

        // 뷰페이저 어댑터 연결
        adapter = MyPagerAdapter(this)
        viewPager.adapter = adapter

        // 프래그먼트 지정
        val imageFragment = (adapter.fragments[0]) as ImageFragment
        val textFragment = (adapter.fragments[1]) as TextFragment
        val summarizeFragment = (adapter.fragments[2]) as SummarizeFragment

        // 임시파일 지정
        tempFile = File(cacheDir, "cacheImageTermP.jpg")

        // tempFile에서 비트맵 추출
        val inputStream = tempFile.toUri().let { contentResolver.openInputStream(it) }
        val bitmap = BitmapFactory.decodeStream(inputStream)

//        코루틴 1. coroutine_getText
//        텍스트 요약 시작
//        요약이 끝나면 그 텍스트를 resultText에 저장
//        resultText에 있는 내용을 GPT에 전달
//        GPT에게 결과를 받으면 그걸 ResultGPT에 저장
//
//        코루틴 2. coroutine_waitText
//        textFragment가 초기화 될 때까지 + resultText의 내용이 null이 아닐때까지 대기
//        두 조건을 만족하면 textFragment의 textView의 내용 갱신
//
//        코루틴 3. coroutine_waitGPT
//        SummarizeFragment가 초기화 될 때까지 + resultGPT의 내용이 null이 아닐때까지 대기
//        두 조건을 만족하면 SummarizeFragment의 textView의 내용 갱신

        val coroutine_getText = CoroutineScope(Dispatchers.Default)
        val coroutine_waitText = CoroutineScope(Dispatchers.Main) // UI 내용을 변경하는 작업은 Main에서 해야 함
        val coroutine_waitGPT = CoroutineScope(Dispatchers.Main)

        suspend fun waitForResultText() {
            return suspendCancellableCoroutine { continuation ->
                // 주기적으로 조건을 확인합니다.
                CoroutineScope(Dispatchers.Default).launch {
                    while (!::resultText.isInitialized) {
                        delay(100)  // 100ms 대기 후 다시 조건 확인
                    }
                    // 조건이 만족되면 코루틴을 재개합니다.
                    continuation.resume(Unit)
                }
            }
        }
        suspend fun waitForResultGPT() {
            return suspendCancellableCoroutine { continuation ->
                CoroutineScope(Dispatchers.Default).launch {
                    while (!::resultGPT.isInitialized) {
                        delay(100)
                    }
                    continuation.resume(Unit)
                }
            }
        }

        suspend fun waitForTextFragmentInit() {
            return suspendCancellableCoroutine { continuation ->
                CoroutineScope(Dispatchers.Default).launch {
                    while (!textFragment.isAdded) {
                        delay(100)
                    }
                    continuation.resume(Unit)
                }
            }
        }

        suspend fun waitForSummarizeFragmentInit() {
            return suspendCancellableCoroutine { continuation ->
                CoroutineScope(Dispatchers.Default).launch {
                    while (!summarizeFragment.isAdded) {
                        delay(100)
                    }
                    continuation.resume(Unit)
                }
            }
        }

        coroutine_getText.launch {
            // 텍스트 요약 시작
            Image_to_text.Write_Text(bitmap, this@ResultActivity)
            // 텍스트 추출이 끝날때까지 대기
            waitForResultText()
            // 추출이 끝났으므로 GPT에 보냄
            TextClassify.process_text_Gpt(resultText.text,this@ResultActivity)
        }

        coroutine_waitText.launch {
            // 텍스트 추출이 끝날때까지 대기
            waitForResultText()
            // TextFragment가 초기화 될 때까지 대기
            waitForTextFragmentInit()
            // textView에 텍스트 지정
            textFragment.textView.text = resultText.text
        }

        coroutine_waitGPT.launch {
            // 요약이 끝날때까지 대기
            waitForResultGPT()
            // SummarizeFragment가 초기화 될 때까지 대기
            waitForSummarizeFragmentInit()
            // textView에 텍스트 지정
            summarizeFragment.textView.text = resultGPT
        }

        // viewpager에 리스너를 연결하여 TextFragment로 옮길 때 함수를 실행
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(position == 0 && imageFragment.imageView.drawable == null){
                    // 이미지 지정
                    imageFragment.setImage(tempFile.toUri())
                }
            }
        })

        // cacheDir에 있는 파일을 가져와 ImageFragment의 ImageView에 띄움
        // fragment가 초기화 된 이후 setImage를 해야하므로 post를 사용
        viewPager.post{
            imageFragment.setImage(tempFile.toUri())
        }
    }

    /**
     * 현재 위치한 fragment에 대응하는 파일을 저장합니다.
     */
    private fun saveBtnClick(){
        when(viewPager.currentItem){
            0 -> saveImg()
            1 -> saveText()
            2 -> saveSummarizedText()
        }
    }

    /**
     * 추출한 문서를 그림 형태로 저장
     */
    fun saveImg(){
        // 저장할 파일 경로 설정
        val dstDir = File("/storage/emulated/0/Pictures/cutImage") // 대상 디렉토리 경로

        // 설정에 저장된 값을 가져옴
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val saveToPNG = sharedPreferences.getBoolean("saveToPNG", false)
        val saveToBin = sharedPreferences.getBoolean("saveToBlack", false)

        // jpg또는 png로 저장
        var name = ""
        if(saveToPNG)
            name = "img.png" // 새로운 파일 이름
        else
            name = "img.jpg" // 새로운 파일 이름

        // 흑백 또는 원본으로 저장
        if(saveToBin){
            // temp에서 비트맵 추출
            val srcBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

            // 비트맵 OpenCV의 Mat으로 변환
            val src = Mat()
            Utils.bitmapToMat(srcBitmap, src)

            // 흑백영상으로 전환
            val graySrc = Mat()
            Imgproc.cvtColor(src, graySrc, Imgproc.COLOR_BGR2GRAY)

            // dstBitmap을 binarySrc와 크기가 같은 Bitmap 으로 설정
            val dstBitmap = Bitmap.createBitmap(graySrc.cols(), graySrc.rows(), Bitmap.Config.ARGB_8888);

            // 결과를 Mat에서 Bitmap으로 변경
            Utils.matToBitmap(graySrc, dstBitmap)

            // 변경한 Bitmap을 다시 temp에 저장...
            val outputStream = FileOutputStream(tempFile)
            dstBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        }


        if(!dstDir.exists()){
            dstDir.mkdirs()
        }

        val dstFile = File(dstDir, name)

        try{
            // 복사
            val inputStream: InputStream = FileInputStream(tempFile)
            try {
                val out: OutputStream = FileOutputStream(dstFile)
                try {
                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len: Int
                    while ((inputStream.read(buf).also { len = it }) > 0) {
                        out.write(buf, 0, len)
                    }
                } finally {
                    out.close()
                }
            } finally {
                inputStream.close()
            }

            // 메세지 띄움
            Toast.makeText(this, "저장되었습니다!", Toast.LENGTH_SHORT).show()

            // MainActivity 실행
            startActivity(Intent(this@ResultActivity, MainActivity::class.java))
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }


    /**
     * 문서에서 추출한 텍스트를 저장
     */
    fun saveText(){
        if(::resultText.isInitialized){
            val txt = resultText.text
            if (txt.isEmpty()) {
                Toast.makeText(this, "저장할 텍스트가 없습니다.", Toast.LENGTH_LONG).show()
            } else {
                //파일 작성시간을 추가하는 것. 필요에 따라 제거
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                val contents = "파일 내용 :\n$txt \n파일 작성 시간 : $now\n"
                val dir = File(foldername)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                val fos = FileOutputStream("$foldername/$filename")
                fos.write(contents.toByteArray())
                fos.close()
                Toast.makeText(this, "txt파일이 생성되었습니다.", Toast.LENGTH_LONG).show()
            }
        }
        else{
            Toast.makeText(this, "아직 추출이 끝나지 않았습니다.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 문서에서 추출한 텍스트의 요약본을 저장
     */
    fun saveSummarizedText(){
        if(::resultGPT.isInitialized){
            if (resultGPT.isEmpty()) {
                Toast.makeText(this, "저장할 텍스트가 없습니다.", Toast.LENGTH_LONG).show()
            } else {
                val contents="요약된 내용 :\n$resultGPT\n"
                val dir = File(foldername)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                val fos = FileOutputStream("$foldername/$filename_gpt")
                fos.write(contents.toByteArray())
                fos.close()
                Toast.makeText(this, "txt파일이 생성되었습니다.", Toast.LENGTH_LONG).show()
            }
        }
        else{
            Toast.makeText(this, "아직 요약이 끝나지 않았습니다.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 현재 파일을 저장하지 않고 MainActivity로 돌아갑니다.
     */
    fun resetBtnClick(){
        // 임시파일 제거
        tempFile.delete()

        // MainActivity 실행
        startActivity(Intent(this@ResultActivity, MainActivity::class.java))
    }

}