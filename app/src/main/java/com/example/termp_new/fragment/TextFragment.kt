package com.example.termp_new.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.termp_new.R

/**
 * 기존 이미지에서 문서 부분을 추출하고 거기의 텍스트를 화면에 출력
 */
class TextFragment : Fragment() {
    lateinit var textView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_text, container, false)
        textView = v.findViewById(R.id.textView)
        return v
    }
}