package com.example.movies

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView

class GenreAdapter (
    private var genres: List<String>,
    private var genreIds: List<Int>
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private lateinit var dbHelper: DatabaseHelper

    class GenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemGenre: EditText = view.findViewById(R.id.itemGenre)
        val editBtn: ImageButton = view.findViewById(R.id.editGenreBtn)

        val genreId: TextView = view.findViewById(R.id.genreID)
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
        val id = genreIds[position]

        var editGenreText = ""

        holder.itemGenre.setText(genre)
        holder.genreId.text = id.toString()
        holder.editBtn.visibility = View.GONE

        dbHelper = DatabaseHelper(holder.itemGenre.context)

        holder.itemGenre.addTextChangedListener{ text ->
            editGenreText = text.toString()
            holder.editBtn.visibility = View.VISIBLE
        }

        holder.editBtn.setOnClickListener {
            Log.d("EDIT_GENRE", "edited text: '$editGenreText'", null)
            dbHelper.editGenre(id, editGenreText)
            holder.editBtn.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int = genres.size
}