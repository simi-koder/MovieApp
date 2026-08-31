package com.example.movies

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movies.databinding.MovieListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieList : Fragment() {

    private var _binding: MovieListBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedMoviesViewModel by activityViewModels()

    private var selectedGenres = listOf<String>()
    private var yearInputText = String()
    private var ratingInputText = String()

    private var directorInputText = String()

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var genres: Array<String>
    private lateinit var checkedGenres: BooleanArray
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movieAdapter = MovieAdapter(emptyList(), sharedViewModel)
        binding.movieRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.movieRecyclerView.adapter = movieAdapter

        dbHelper = DatabaseHelper(requireContext())

        genres = dbHelper.getAllGenres().toTypedArray()
        checkedGenres = BooleanArray(genres.size)

        // sleduj zmeny v shared movies a automaticky aktualizuj RecyclerView
        sharedViewModel.movies.observe(viewLifecycleOwner) { updatedMovies ->
            movieAdapter.updateMovies(updatedMovies)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val loadedMovies = dbHelper.getAllMovies()

            withContext(Dispatchers.Main) {
                sharedViewModel.setMovies(loadedMovies)
            }
        }

        binding.textViewFilter.setOnClickListener {
            binding.filterOptions.visibility =
                if (binding.filterOptions.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    val userNames = dbHelper.getUsers()
                    val seenChecks = listOf(
                        binding.videl1, binding.videl2, binding.videl3, binding.videl4, binding.videl5,
                        binding.videl6, binding.videl7, binding.videl8, binding.videl9, binding.videl10
                    )

//                    var lastCheckId = R.id.videl1

                    userNames.forEachIndexed { i, name ->
                        seenChecks[i].apply {
                            visibility = View.VISIBLE
                            text = name
                        }
                        Log.d("NAME_FOR_EACH", name)
//                        lastCheckId = seenChecks[i].id
                    }

//                    TODO: pridat poslednemu cloveku constraint

                    if (userNames.size >= 2) {
                        binding.videneSpoluCheck.visibility = View.VISIBLE
//
//                        ConstraintSet().apply {
//                            clone(binding.filterOptions)
//                            connect(R.id.videneSpoluCheck, ConstraintSet.START, lastCheckId, ConstraintSet.END)
//                            connect(R.id.videneSpoluCheck, ConstraintSet.TOP, lastCheckId, ConstraintSet.TOP)
//                            connect(R.id.videneSpoluCheck, ConstraintSet.BOTTOM, lastCheckId, ConstraintSet.BOTTOM)
//                            connect(R.id.videneSpoluCheck, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
//                            applyTo(binding.filterOptions)
//                        }
                    }
                    View.VISIBLE
                }
        }


        binding.deleteFilterBtn.setOnClickListener {

            // vyresetuj textové polia
            binding.yearInput.text?.clear()
            binding.ratingInput.text?.clear()
            binding.directorText.text?.clear()
            binding.searchBarText.text?.clear()

//            // vyresetuj checkboxy
//            binding.videlSimiCheck.isChecked = false
//            binding.videlaTerkaCheck.isChecked = false
            binding.videneSpoluCheck.isChecked = false
            binding.colorCheck.isChecked = false
            binding.grayscaleCheck.isChecked = false

            // vyresetuj žánre
            selectedGenres = listOf()
            checkedGenres = BooleanArray(genres.size)
            binding.vyberZanre.text = "Vyber žánre"

            // vyresetuj premenné pre text watchers
            yearInputText = ""
            ratingInputText = ""
            directorInputText = ""

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val allMovies = dbHelper.getAllMovies()

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(allMovies)
                }
            }
        }

        binding.searchButton.setOnClickListener {
            val title = binding.searchBarText.text.toString()

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val currentMovies = sharedViewModel.getCurrentMovies()

                val moviesTemp = if (!(title.isBlank())) {
                    dbHelper.searchMovieByName(title, currentMovies)
                } else {
                    currentMovies
                }

                withContext(Dispatchers.Main) {
                    movieAdapter.updateMovies(moviesTemp)
                    // poznámka: tu NEupravujeme sharedViewModel, lebo ide len o dočasné zobrazenie výsledkov hľadania
                }
            }
        }

        binding.vyberZanre.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vyber žánre")
                .setMultiChoiceItems(genres, checkedGenres) { _, which, isChecked ->
                    checkedGenres[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedGenres = genres.filterIndexed { index, _ -> checkedGenres[index] }
                    binding.vyberZanre.text =
                        if (selectedGenres.isEmpty()) "Vyber žánre"
                        else selectedGenres.joinToString(",")
                }
                .show()
        }

        binding.yearInput.addTextChangedListener { text -> yearInputText = text.toString() }
        binding.ratingInput.addTextChangedListener { text -> ratingInputText = text.toString() }
        binding.directorText.addTextChangedListener { text -> directorInputText = text.toString() }

        binding.searchFilterBtn.setOnClickListener {
//            TODO: dokoncit filtrovanuie podla videnosti
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                val videlSimi = binding.videlSimiCheck.isChecked
//                val videlaTerka = binding.videlaTerkaCheck.isChecked
                val videneSpolu = binding.videneSpoluCheck.isChecked
                val color = binding.colorCheck.isChecked
                val grayscale = binding.grayscaleCheck.isChecked


                val filteredMovies = dbHelper.getMoviesByFilters(
                    genreListRaw = selectedGenres,
//                    videlSimi = videlSimi,
//                    videlaTerka = videlaTerka,
                    videneSpolu = videneSpolu,
                    year = yearInputText,
                    rating = ratingInputText,
                    color = color,
                    grayscale = grayscale,
                    director = directorInputText
                )

                withContext(Dispatchers.Main) {
                    sharedViewModel.setMovies(filteredMovies)
                }
            }
        }

        binding.randomMovie.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val movie = dbHelper.getRandomMovie(moviesList = sharedViewModel.getCurrentMovies())

                withContext(Dispatchers.Main) {
                    if (movie != null) {
                        movieAdapter.updateMovies(listOf(movie))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}