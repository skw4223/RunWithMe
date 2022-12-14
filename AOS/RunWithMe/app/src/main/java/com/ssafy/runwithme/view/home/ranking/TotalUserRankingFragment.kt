package com.ssafy.runwithme.view.home.ranking

import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ssafy.runwithme.R
import com.ssafy.runwithme.base.BaseFragment
import com.ssafy.runwithme.databinding.FragmentTotalUserRankingBinding
import com.ssafy.runwithme.view.home.HomeViewModel

class TotalUserRankingFragment : BaseFragment<FragmentTotalUserRankingBinding>(R.layout.fragment_total_user_ranking) {

    private val homeViewModel by activityViewModels<HomeViewModel>()
    private lateinit var totalRankingAdapter: TotalRankingAdapter

    override fun init() {
        binding.apply {
            homeVM = homeViewModel
            totalRankingAdapter = TotalRankingAdapter(homeViewModel, totalUserRankingListener)
            recyclerTotalUserRanking.adapter = totalRankingAdapter

            spinnerTotalUserRanking.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) { // 1. 거리 , 2. 시간, 3. 포인트
                    var type = when(position) {
                        0 -> "distance"
                        1 -> "time"
                        2 -> "point"
                        else -> ""
                    }
                    homeViewModel.getTotalRanking(type, 100, 0)
                    homeViewModel.getMyRanking(type)
                    totalRankingAdapter.notifyDataSetChanged()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        initClickListener()
    }

    private fun initClickListener() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private val totalUserRankingListener: TotalUserRankingListener = object : TotalUserRankingListener {
        override fun onItemClick(userSeq: Int) {
            val action = TotalUserRankingFragmentDirections.actionTotalUserRankingFragmentToUserDetailFragment(userSeq)
            findNavController().navigate(action)
        }
    }
}