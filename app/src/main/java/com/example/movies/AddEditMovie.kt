package com.example.movies

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.movies.databinding.AddEditMovieBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditMovie : Fragment() {

    private var _binding: AddEditMovieBinding? = null
    private val binding get() = _binding!!

    private var selectedGenres = listOf<String>()

    private lateinit var names: Array<String>
    private lateinit var checkedNames: BooleanArray
    private var selectedNames = listOf<String>()

    private var titleInputText: String = ""
    private var yearInputText: Int = 0
    private var ratingInputText: Double = 0.0
    private var directorInputText: String = ""
    private var priorityInputText: Int = 0
    private var descriptionText: String = ""

    private lateinit var genres: Array<String>
    private lateinit var checkedGenres: BooleanArray

    private var deleteMovieTitle: String = ""

    private var editMovieInputText: String = ""
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddEditMovieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()
        dbHelper = DatabaseHelper(requireContext())

        genres = dbHelper.getAllGenres().toTypedArray()
        checkedGenres = BooleanArray(genres.size)

        names = dbHelper.getUsers().toTypedArray()
        checkedNames = BooleanArray(names.size)

        binding.newGenres.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vyber žánre")
                .setMultiChoiceItems(genres, checkedGenres) { _, which, isChecked ->
                    checkedGenres[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedGenres = genres.filterIndexed { index, _ ->
                        checkedGenres[index]
                    }

                    binding.newGenres.text =
                        if (selectedGenres.isEmpty())
                            "Vyber žánre"
                        else
                            selectedGenres.joinToString(", ")
                }
                .show()
        }

        binding.newTitleText.addTextChangedListener { text ->
            titleInputText = text.toString()
        }

        binding.chooseSeen.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vyber užívateľov čo film videli")
                .setMultiChoiceItems(names, checkedNames) { _, which, isChecked ->
                    checkedNames[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    selectedNames = names.filterIndexed { index, _ ->
                        checkedNames[index]
                    }
                    binding.chooseSeen.text =
                        if (selectedNames.isEmpty())
                            "Vyber videnosť"
                        else
                            "✓"
                }
                .show()
        }

        binding.newYearText.addTextChangedListener { text ->
            yearInputText = text.toString().toIntOrNull() ?: 0
        }

        binding.newDirectorText.addTextChangedListener { text ->
            directorInputText = text.toString()
        }

        binding.newRating.addTextChangedListener { text ->
            ratingInputText = text.toString().toDoubleOrNull() ?: 0.0
        }

        binding.newPriority.addTextChangedListener { text ->
            priorityInputText = text.toString().toIntOrNull() ?: 0
        }

        binding.descriptionText.addTextChangedListener { text ->
            descriptionText = text.toString()
        }

        binding.addMovieBtn.setOnClickListener {

            val seenUserIds = dbHelper.getUserIds(selectedNames)
            val seenBoth = binding.newSeenBoth.isChecked
            val clr = binding.newColor.isChecked

            if (titleInputText.isBlank()) {
                Toast.makeText(requireContext(), "Zadaj názov filmu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                val genreIds = selectedGenres.mapNotNull { dbHelper.getGenreId(it) }

                val newId = dbHelper.addMovie(
                    title = titleInputText,
                    director = directorInputText,
                    rating = ratingInputText,
                    year = yearInputText,
                    genreIds = genreIds,
                    videneSpolu = seenBoth,
                    priority = priorityInputText,
                    color = clr,
                    description = descriptionText,
                    seenArray = seenUserIds
                )

                withContext(Dispatchers.Main) {
                    if (newId != -1L) {
//                        Log.d("ADD_MOVIE", "Film pridaný, id=$newId")
                        Toast.makeText(requireContext(), "Film pridaný", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    } else {
//                        Log.e("ADD_MOVIE", "Insert zlyhal")
                        Toast.makeText(requireContext(), "Pridanie zlyhalo", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.deleteMovieName.addTextChangedListener{ text ->
            deleteMovieTitle = text.toString()
        }

        binding.deleteMovieBtn.setOnClickListener {

            if (deleteMovieTitle.isBlank()){
                Toast.makeText(requireContext(), "Zadaj názov filmu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tempBool = dbHelper.deleteMovie(deleteMovieTitle)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    if (tempBool) {
//                        Log.d("DEL_MOVIE", "Film vymazany")
                        Toast.makeText(requireContext(), "Film vymazaný", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    } else {
//                        Log.e("DEL_MOVIE", "Delete zlyhal")
                        Toast.makeText(requireContext(), "Vymazanie zlyhalo", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.editMovieTitle.addTextChangedListener{ text ->
            editMovieInputText = text.toString()
        }

        binding.editMovieBtn.setOnClickListener {
            if (editMovieInputText.isBlank()) {
//                binding.editMovieTitle.error = "Zadaj názov filmu"
                Toast.makeText(requireContext(), "Zadaj názov filmu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("movieTitle", editMovieInputText)
            }

            navController.navigate(R.id.EditMovieFragment, bundle)

        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}