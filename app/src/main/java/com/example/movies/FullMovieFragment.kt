package com.example.movies

import android.os.Bundle
import android.os.Build
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.movies.databinding.FullMovieFragmentBinding

//TODO: zamenit simi terka za gone checkboxy yk
class FullMovieFragment : Fragment() {

    private var _binding: FullMovieFragmentBinding? = null

    private val binding get() = _binding!!

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FullMovieFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        val movie: MovieFull? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("movie", MovieFull::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("movie") as? MovieFull
        }

        binding.fullMovieTitle.setText(movie?.title)
        binding.fullMovieYear.setText(movie?.year.toString())
        binding.fullMovieDirector.setText(movie?.director)
        binding.fullMovieSB.isChecked = movie?.seen_both ?: false
        binding.fullMovieSB.isEnabled = false
        binding.fullMovieGenres.setText(movie?.genre?.joinToString(", "))
        binding.fullMovieRating.setText("Hodnotenie - " + movie?.rating.toString())
        binding.fullMovieOurRating.setText("Naše hodnotenie - " + movie?.our_rating.toString())
        binding.fullMoviePriority.setText("Priorita - " + movie?.priority.toString())
        binding.fullMovieColor.isChecked = movie?.color ?: false
        binding.fullMovieColor.isEnabled = false
        if (!movie?.description.isNullOrEmpty()) {
            binding.descriptionPlaceholder.visibility = View.VISIBLE
            binding.descriptionPlaceholder.setText(movie.description)
        } else {
            binding.descriptionPlaceholder.visibility = View.GONE
        }

        binding.editMovieBtnDetail.setOnClickListener {
            val bundle = Bundle().apply {
                putString("movieTitle", movie?.title)
            }

            navController.navigate(R.id.EditMovieFragment, bundle)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}