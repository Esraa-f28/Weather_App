package com.example.myapplication.ui.favourites.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FavoriteItemLayoutBinding
import com.example.myapplication.model.local.FavoritePlace

class FavAdapter(
    private val onDeleteClick: (FavoritePlace) -> Unit,
    private val onItemClick: (FavoritePlace) -> Unit
) : ListAdapter<FavoritePlace, FavAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    class FavoriteViewHolder(
        private val binding: FavoriteItemLayoutBinding,
        private val onDeleteClick: (FavoritePlace) -> Unit,
        private val onItemClick: (FavoritePlace) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentPlace: FavoritePlace? = null

        init {
            binding.root.setOnClickListener {
                currentPlace?.let { place ->
                    onItemClick(place)
                }
            }
            binding.buttonDelete.setOnClickListener {
                currentPlace?.let { place ->
                    onDeleteClick(place)
                }
            }
        }

        fun bind(place: FavoritePlace) {
            currentPlace = place
            binding.textViewPlaceName.text = place.city ?: "Unknown City"
            binding.textViewCoordinates.text = "Lat: ${place.latitude}, Lon: ${place.longitude}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = FavoriteItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavoriteViewHolder(binding, onDeleteClick, onItemClick)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoritePlace>() {
        override fun areItemsTheSame(oldItem: FavoritePlace, newItem: FavoritePlace): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoritePlace, newItem: FavoritePlace): Boolean {
            return oldItem == newItem
        }
    }
}