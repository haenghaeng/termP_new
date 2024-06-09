package com.example.termp_new.fragment

import android.graphics.Color
import com.example.termp_new.R
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_layout, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView의 배경색을 변경합니다.
        view.setBackgroundColor(Color.BLACK)
    }
}