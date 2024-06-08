package com.example.termp_new

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.termp_new.fragment.ImageFragment
import com.example.termp_new.fragment.TextFragment


class MyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    val fragments : List<Fragment> = listOf(ImageFragment(), TextFragment())

    override fun getItemCount(): Int {
        // 페이지 수 반환
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}