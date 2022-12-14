package com.ssafy.runwithme.view.home

import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.ssafy.runwithme.R
import com.ssafy.runwithme.base.BaseFragment
import com.ssafy.runwithme.base.BaseFragmentKeep
import com.ssafy.runwithme.databinding.FragmentHomeBinding
import com.ssafy.runwithme.view.home.tab.crew.TabCrewFragment
import com.ssafy.runwithme.view.home.tab.home.TabHomeFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragmentKeep<FragmentHomeBinding>(R.layout.fragment_home) {

    private lateinit var tabHomeFragment : TabHomeFragment
    private lateinit var tabCrewFragment : TabCrewFragment

    override fun init() {
        initTabLayout()
    }

    // 각 탭에 들어갈 프레그먼트 객체화
    private fun initTabLayout(){
        tabHomeFragment = TabHomeFragment()
        tabCrewFragment = TabCrewFragment()

        childFragmentManager.beginTransaction().replace(R.id.frame_layout_home, tabHomeFragment).commit()

        binding.tabLayoutHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab!!.position) {
                    0 -> replaceView(tabHomeFragment)
                    1 -> replaceView(tabCrewFragment)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // 화면 변경
    private fun replaceView(tab : Fragment){
        var selectedFragment = tab
        selectedFragment.let {
            childFragmentManager.beginTransaction().replace(R.id.frame_layout_home, it).commit()
        }
    }

}