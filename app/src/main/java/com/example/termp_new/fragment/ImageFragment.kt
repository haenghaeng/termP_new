package com.example.termp_new.fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.termp_new.R


/**
 * 기존 이미지에서 문서 부분만 화면에 출력함
 */
class ImageFragment : Fragment() {
    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_image, container, false)
        imageView = v.findViewById(R.id.imageViewResult)
        return v
    }

    fun setImage(uri: Uri){
        if(::imageView.isInitialized)
            imageView.setImageURI(uri)
    }
}