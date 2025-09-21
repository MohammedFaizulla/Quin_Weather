package com.quin.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quin.weather.databinding.ItemCityBinding

class CityAdapter(private val onClick: (CityItem) -> Unit) :
    ListAdapter<CityItem, CityAdapter.ViewHolder>(diffCallback) {

    inner class ViewHolder(private val binding: ItemCityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(city: CityItem) {
            binding.tvCity.text = "${city.name}, ${city.country}"
            binding.root.setOnClickListener { onClick(city) }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CityItem>() {
            override fun areItemsTheSame(
                oldItem: CityItem,
                newItem: CityItem
            ): Boolean = oldItem.lat == newItem.lat && oldItem.lon == newItem.lon

            override fun areContentsTheSame(
                oldItem: CityItem,
                newItem: CityItem
            ): Boolean = oldItem == newItem

        }
    }
}