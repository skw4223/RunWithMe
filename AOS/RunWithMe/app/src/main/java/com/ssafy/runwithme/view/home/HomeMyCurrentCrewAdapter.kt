package com.ssafy.runwithme.view.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.runwithme.databinding.ItemMyCrewHorizonBinding
import com.ssafy.runwithme.model.response.MyCurrentCrewResponse

class HomeMyCurrentCrewAdapter
    : ListAdapter<MyCurrentCrewResponse, HomeMyCurrentCrewAdapter.ViewHolder>(diffUtil){

    inner class ViewHolder(private val binding: ItemMyCrewHorizonBinding): RecyclerView.ViewHolder(binding.root){

        // TODO : 클릭리스너
        init {

        }
        fun bind(myCurrentCrew: MyCurrentCrewResponse){
            binding.myCurrentCrewResponse = myCurrentCrew
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyCrewHorizonBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object{
        val diffUtil = object : DiffUtil.ItemCallback<MyCurrentCrewResponse>(){
            override fun areItemsTheSame(oldItem: MyCurrentCrewResponse, newItem: MyCurrentCrewResponse): Boolean {
                return oldItem.crewId == newItem.crewId
            }

            override fun areContentsTheSame(oldItem: MyCurrentCrewResponse, newItem: MyCurrentCrewResponse): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}