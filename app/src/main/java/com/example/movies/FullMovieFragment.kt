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

class FullMovieFragment : Fragment() {

    private var _binding: FullMovieFragmentBinding? = null

    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var dbHelper: DatabaseHelper

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
        dbHelper = DatabaseHelper(requireContext())

        val names = listOf(binding.videl1, binding.videl2, binding.videl3, binding.videl4, binding.videl5,
            binding.videl6, binding.videl7, binding.videl8, binding.videl9, binding.videl10)

        val movie: MovieFull? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("movie", MovieFull::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("movie") as? MovieFull
        }

        binding.fullMovieTitle.text = movie?.title
        binding.fullMovieYear.text = movie?.year.toString()
        binding.fullMovieDirector.text = movie?.director
        val allUsers = dbHelper.getUsers()

        val usersThatSeen = if (movie != null) dbHelper.getUserSeenMovie(movie.id) else emptyList()

        allUsers.forEachIndexed { index, string ->
            val curr = names[index]
            curr.visibility = View.VISIBLE
            curr.text = string
            curr.isEnabled = false
            curr.isChecked = curr.text in usersThatSeen
        }

        if (allUsers.size >= 2) {
            binding.videneSpoluCheck.isChecked = movie?.seen_both ?: false
            binding.videneSpoluCheck.visibility = View.VISIBLE
            binding.videneSpoluCheck.isEnabled = false
        }

        binding.fullMovieGenres.text = movie?.genre?.joinToString(", ")
        binding.fullMovieRating.text = "Hodnotenie - " + movie?.rating.toString()
        binding.fullMovieOurRating.text = "Naše hodnotenie - " + movie?.our_rating.toString()
        binding.fullMoviePriority.text = "Priorita - " + movie?.priority.toString()
        binding.fullMovieColor.isChecked = movie?.color ?: false
        binding.fullMovieColor.isEnabled = false
        if (!movie?.description.isNullOrEmpty()) {
            binding.descriptionPlaceholder.visibility = View.VISIBLE
            binding.descriptionPlaceholder.text = movie.description
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