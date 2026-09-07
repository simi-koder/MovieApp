package com.example.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GenreAdapter (
    private var genres: List<String>
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    class GenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemGenre: TextView = view.findViewById(R.id.itemGenre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: GenreViewHolder,
        position: Int
    ) {
        val genre = genres[position]
        holder.itemGenre.text = genre
    }

    override fun getItemCount(): Int = genres.size
}