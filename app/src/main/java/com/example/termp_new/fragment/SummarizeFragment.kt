package com.example.termp_new.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.termp_new.R

/**
 * 문서의 내용을 요약하여 화면에 출력
 */
class SummarizeFragment : Fragment() {

    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_summarize, container, false)
        textView = v.findViewById(R.id.textViewSummarize)
        return v
    }
}