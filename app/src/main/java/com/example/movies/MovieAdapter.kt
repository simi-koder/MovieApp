package com.example.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieAdapter(
    private var movies: List<MovieFull>,
    private val sharedViewModel: SharedMoviesViewModel
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val yearText: TextView = view.findViewById(R.id.yearText)
        val ratingText: TextView = view.findViewById(R.id.ratingText)
        val directorText: TextView = view.findViewById(R.id.directorText)
        val genreText: TextView = view.findViewById(R.id.genreText)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.titleText.text = movie.title
        holder.directorText.text = movie.director
        holder.yearText.text = movie.year.toString()
        holder.ratingText.text = movie.rating.toString()
        holder.genreText.text = movie.genre.joinToString(", ")

        holder.yearText.setOnClickListener {
            val dbHelper = DatabaseHelper(holder.itemView.context)
            val selectedYear = holder.yearText.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val filteredMovies = dbHelper.getMoviesByFilters(year = selectedYear, genreListRaw = emptyList())

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(filteredMovies)
                }
            }
        }

        holder.directorText.setOnClickListener {
            val dbHelper = DatabaseHelper(holder.itemView.context)
            val selectedDirector = holder.directorText.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val filteredMovies = dbHelper.getMoviesByFilters(director = selectedDirector, genreListRaw = emptyList())

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(filteredMovies)
                }
            }
        }

        holder.ratingText.setOnClickListener {
            val dbHelper = DatabaseHelper(holder.itemView.context)
            val selectedRating = holder.ratingText.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val filteredMovies = dbHelper.getMoviesByFilters(rating = selectedRating, genreListRaw = emptyList())

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(filteredMovies)
                }
            }
        }

        holder.genreText.setOnClickListener {
            val dbHelper = DatabaseHelper(holder.itemView.context)
            val selectedGenres = holder.genreText.text.toString().split(",").map { it.trim() }

            CoroutineScope(Dispatchers.IO).launch {
                val filteredMovies = dbHelper.getMoviesByFilters(genreListRaw = selectedGenres)

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(filteredMovies)
                }
            }
        }

        holder.titleText.setOnClickListener {
            val movie = movies[position]

            val bundle = Bundle().apply {
                putSerializable("movie", movie)
            }

            val navController = findNavController(holder.itemView)
            navController.navigate(R.id.FullMovieFragment, bundle)
        }

    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<MovieFull>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}