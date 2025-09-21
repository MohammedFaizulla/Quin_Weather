package com.quin.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quin.weather.databinding.ItemCityBinding
import com.quin.weather.databinding.ItemSavedCityBinding

class SavedCityAdapter(private val onClick: (SavedCity) -> Unit) :
    ListAdapter<SavedCity, SavedCityAdapter.ViewHolder>(diffCallback) {

    inner class ViewHolder(private val binding: ItemSavedCityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(city: SavedCity) {
            binding.tvCityName.text = city.cityName
            binding.tvDegree.text = city.degree
            binding.root.setOnClickListener { onClick(city) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ItemSavedCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<SavedCity>() {
            override fun areItemsTheSame(
                oldItem: SavedCity,
                newItem: SavedCity
            ): Boolean = oldItem.degree == newItem.degree && oldItem.cityName == newItem.cityName

            override fun areContentsTheSame(
                oldItem: SavedCity,
                newItem: SavedCity
            ): Boolean = oldItem == newItem

        }
    }


}