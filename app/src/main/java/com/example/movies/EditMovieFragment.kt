package com.example.movies

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.movies.databinding.EditMovieFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditMovieFragment : Fragment() {

    private var _binding: EditMovieFragmentBinding? = null
    private val binding get() = _binding!!

    private val originalText = "Editovať film - "

    private val sharedViewModel: SharedMoviesViewModel by activityViewModels()

    private var selectedGenres = listOf<String>()

    private lateinit var genres: Array<String>
    private lateinit var names: Array<String>
    private lateinit var seenNamesBoolArray: BooleanArray
    private lateinit var selectedNames: List<String>
    private lateinit var checkedGenres: BooleanArray
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditMovieFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieTitle = arguments?.getString("movieTitle") ?: ""

        dbHelper = DatabaseHelper(requireContext())

        genres = dbHelper.getAllGenres().toTypedArray()
        checkedGenres = BooleanArray(genres.size)

        val movieToEdit = updatePageTitle(originalText, movieTitle)

        binding.editMovieBtn.setOnClickListener {

            val title = binding.editTitleText.text.toString()
            val director = binding.editDirectorText.text.toString()

            val year = binding.editYearText.text.toString()
                .toIntOrNull()

            val rating = binding.editRating.text.toString()
                .toDoubleOrNull()

            val priority = binding.editPriority.text.toString()
                .toIntOrNull()

            val ourRating = binding.editOurRating.text.toString()
                .toDoubleOrNull()

//            val seenSimi = binding.editSeenSimi.isChecked
//            val seenTerka = binding.editSeenTerka.isChecked
            val seenBoth = binding.editSeenBoth.isChecked
            val color = binding.editColor.isChecked

            if (year == null || rating == null || priority == null || ourRating == null) {
                Toast.makeText(
                    requireContext(),
                    "Skontroluj zadané hodnoty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val description = binding.descriptionText.text.toString()

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                val genreIds = selectedGenres.mapNotNull {
                    dbHelper.getGenreId(it)
                }

//                val success = dbHelper.editMovie(
//                    title = title,
//                    director = director,
//                    rating = rating,
//                    year = year,
//                    genreIds = genreIds,
////                    videlSimi = seenSimi,
////                    videlaTerka = seenTerka,
//                    videneSpolu = seenBoth,
//                    priority = priority,
//                    color = color,
//                    our_rating = ourRating,
//                    description = description,
//                    movieToEdit = movieToEdit
//                )

//                withContext(Dispatchers.Main) {
//                    if (success) {
//                        Log.d("EDIT_MOVIE", "Film edited")
//                        findNavController().popBackStack()
//                    } else {
//                        Log.e("EDIT_MOVIE", "Edit zlyhal")
//                    }
//                }
            }
        }

    }

    fun updatePageTitle(defaultTitle: String, movieTitle: String): MovieFull? {

        val matchedMovies = dbHelper.searchMovieByName(movieTitle, sharedViewModel.getCurrentMovies())

        names = dbHelper.getUsers().toTypedArray()
        seenNamesBoolArray = BooleanArray(names.size)

        if (matchedMovies.size != 1){
            val message = if (matchedMovies.isEmpty())
                "Film '$movieTitle' sa nenašiel"
            else
                "Nájdených viac filmov (${matchedMovies.size}) s názvom '$movieTitle', spresni výber"

            Log.e("EDIT_MOVIE", message)
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            findNavController().popBackStack()
            return null
        }

        val singleMatchedMovie: MovieFull = matchedMovies.first()

//        TODO: dokoncit
//        selectedNames = dbHelper.userSeen(singleMatchedMovie.id)

        val editFilmTitle = singleMatchedMovie.title
        val editFilmYear = singleMatchedMovie.year
        val editFilmDirector = singleMatchedMovie.director
        val editFilmRating = singleMatchedMovie.rating
        val editFilmPriority = singleMatchedMovie.priority
        val editFilmColor =  singleMatchedMovie.color
        val editFilmSeenBoth =  singleMatchedMovie.seen_both
        val editFilmOurRating =  singleMatchedMovie.our_rating
        val editFilmDescription = singleMatchedMovie.description

        val editFilGenres = singleMatchedMovie.genre
            .map { it.trim() }

        selectedGenres = editFilGenres

        val editGenresBoolArray = BooleanArray(genres.size) { index ->
            editFilGenres.contains(genres[index])
        }

        Log.d("EDIT_CHECKED_GENRES", editGenresBoolArray.contentToString())

        binding.editMovie.text = defaultTitle + editFilmTitle
        binding.editTitleText.setText(editFilmTitle)
        binding.editYearText.setText(editFilmYear.toString())
        binding.editDirectorText.setText(editFilmDirector)
        binding.editRating.setText(editFilmRating.toString())
        binding.editPriority.setText(editFilmPriority.toString())
        binding.editColor.isChecked = editFilmColor
        binding.editSeenBoth.isChecked = editFilmSeenBoth
        binding.editOurRating.setText(editFilmOurRating.toString())
        binding.descriptionText.setText(editFilmDescription)

        binding.seenBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Videl/a")
                .setMultiChoiceItems(names, seenNamesBoolArray) { _, which, isChecked ->
                    seenNamesBoolArray[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedNames = names.filterIndexed { index, _ ->
                        seenNamesBoolArray[index]
                    }

                    binding.seenBtn.text =
                        if (selectedGenres.isEmpty())
                            "Zadaj videnosť"
                        else
                            "✓"
                }
                .show()
        }

        binding.editGenres.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editovať žánre")
                .setMultiChoiceItems(genres, editGenresBoolArray) { _, which, isChecked ->
                    editGenresBoolArray[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedGenres = genres.filterIndexed { index, _ ->
                        editGenresBoolArray[index]
                    }

                    binding.editGenres.text =
                        if (selectedGenres.isEmpty())
                            "Editovať žánre"
                        else
                            selectedGenres.joinToString(",")
                }
                .show()
        }
        return singleMatchedMovie
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}