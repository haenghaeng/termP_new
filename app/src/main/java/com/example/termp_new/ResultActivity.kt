package com.example.termp_new

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.viewpager2.widget.ViewPager2
import com.example.termp_new.fragment.ImageFragment
import com.example.termp_new.fragment.SummarizeFragment
import com.example.termp_new.fragment.TextFragment
import com.example.termp_new.openAi.Image_to_text
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class ResultActivity : AppCompatActivity() {

    lateinit var saveBtn : Button
    lateinit var resetBtn : Button
    lateinit var viewPager: ViewPager2
    lateinit var adapter : MyPagerAdapter

    lateinit var tempFile : File

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

        // viewpager에 리스너를 연결하여 TextFragment로 옮길 때 함수를 실행
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(position == 0 && imageFragment.imageView.drawable == null){
                    // 이미지 지정
                    imageFragment.setImage(tempFile.toUri())
                }

                if(position == 1 && textFragment.textView.text == "문서에서 텍스트를 추출중입니다."){
                    // 추출한 비트맵에서 텍스트를 추출하여 textView에 출력
//                    TODO()
//                    Image_to_text.Write_Text(bitmap, this@ResultActivity, textFragment.textView)
                }

                if(position == 2 && summarizeFragment.textView.text == "내용을 요약중입니다.\n잠시만 기다려 주세요!"){
                    // 추출한 비트맵에서 텍스트를 추출하고 요약본을 textView에 출력
//                    TODO()
                    Image_to_text.Write_Text(bitmap, this@ResultActivity, summarizeFragment.textView)
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
        val name = "img.jpg" // 새로운 파일 이름

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
        TODO()
    }

    /**
     * 문서에서 추출한 텍스트의 요약본을 저장
     */
    fun saveSummarizedText(){
        TODO()
    }



    /**
     * 현재 파일을 저장하지 않고 MainActivity로 돌아갑니다.
     */
    fun resetBtnClick(){
        // 임시파일 제거
        tempFile.delete()
        
        // 메세지 띄움
        Toast.makeText(this, "저장하지 않고 돌아왔습니다!", Toast.LENGTH_SHORT).show()        

        // MainActivity 실행
        startActivity(Intent(this@ResultActivity, MainActivity::class.java))
    }

}