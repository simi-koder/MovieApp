package com.example.movies

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

//    private val sharedViewModel: SharedMoviesViewModel by activityViewModels()

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

        val movieId = arguments?.getInt("movieId") ?: 0

        dbHelper = DatabaseHelper(requireContext())

        genres = dbHelper.getAllGenres().toTypedArray()
        checkedGenres = BooleanArray(genres.size)

        val movieToEdit = updatePageTitle(originalText, movieId)

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

                val success = dbHelper.editMovie(
                    title = title,
                    director = director,
                    rating = rating,
                    year = year,
                    genreIds = genreIds,
                    allSaw = seenBoth,
                    priority = priority,
                    color = color,
                    our_rating = ourRating,
                    description = description,
                    movieToEdit = movieToEdit
                )

                dbHelper.delUserSeen(dbHelper.getUserIds(names.toList()), movieToEdit.id)
                dbHelper.addUserSeen(dbHelper.getUserIds(selectedNames.toList()), movieToEdit.id)

                withContext(Dispatchers.Main) {
                    if (success) {
//                        Log.d("EDIT_MOVIE", "Film edited")
                        Toast.makeText(requireContext(), "Film zmenený", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
//                        Log.e("EDIT_MOVIE", "Edit zlyhal")
                        Toast.makeText(requireContext(), "Edit zlyhal", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

    fun updatePageTitle(defaultTitle: String, movieId: Int): MovieFull {

        val matchedMovie = dbHelper.getMovieById(movieId)

        names = dbHelper.getUsers().toTypedArray()
        seenNamesBoolArray = BooleanArray(names.size)

//        if (matchedMovies.size != 1){
//            val message = if (matchedMovies.isEmpty())
//                "Film '$movieTitle' sa nenašiel"
//            else
//                "Nájdených viac filmov (${matchedMovies.size}) s názvom '$movieTitle', spresni výber"
//
////            Log.e("EDIT_MOVIE", message)
//            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
//
//            findNavController().popBackStack()
//        }

//        val singleMatchedMovie: MovieFull = matchedMovie

        selectedNames = dbHelper.getUserSeenMovie(matchedMovie.id)

        val editFilmTitle = matchedMovie.title
        val editFilmYear = matchedMovie.year
        val editFilmDirector = matchedMovie.director
        val editFilmRating = matchedMovie.rating
        val editFilmPriority = matchedMovie.priority
        val editFilmColor =  matchedMovie.color
        val editFilmSeenBoth =  matchedMovie.seen_both
        val editFilmOurRating =  matchedMovie.our_rating
        val editFilmDescription = matchedMovie.description

        val editFilGenres = matchedMovie.genre
            .map { it.trim() }

        selectedGenres = editFilGenres

        val editGenresBoolArray = BooleanArray(genres.size) { index ->
            editFilGenres.contains(genres[index])
        }

        val seenNamesBoolArray = BooleanArray(names.size) { index ->
            selectedNames.contains(names[index])
        }

        binding.seenBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vyber užívateľov čo film videli")
                .setMultiChoiceItems(names, seenNamesBoolArray) { _, which, isChecked ->
                    seenNamesBoolArray[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedNames = names.filterIndexed { index, _ ->
                        seenNamesBoolArray[index]
                    }
                    binding.seenBtn.text =
                        if (selectedNames.isEmpty())
                            "Zadaj videnosť"
                        else
                            "✓"
                }
                .show()
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
        return matchedMovie
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}