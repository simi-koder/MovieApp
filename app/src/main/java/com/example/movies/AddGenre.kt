package com.example.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movies.databinding.AddGenreBinding

class AddGenre : Fragment() {
    private var _binding: AddGenreBinding? = null

    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var navController: NavController

    private lateinit var genreAdapter: GenreAdapter

    private var newGenreType: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddGenreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()
        dbHelper = DatabaseHelper(requireContext())

        genreAdapter = GenreAdapter(dbHelper.getAllGenres())
        binding.recyclerViewGenre.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGenre.adapter = genreAdapter

        binding.genreType.addTextChangedListener{ text -> newGenreType = text.toString()}

        binding.addGenreBtn.setOnClickListener {
            if (newGenreType.isBlank()) {
                binding.genreType.error
                Toast.makeText(requireContext(), "Zadaj názov", Toast.LENGTH_LONG).show()
            }
            if (dbHelper.addNewGenre(newGenreType)){

                binding.genreType.text.clear()

                parentFragmentManager.beginTransaction()
                    .detach(this)
                    .commitNow()

                parentFragmentManager.beginTransaction()
                    .attach(this)
                    .commit()

                Toast.makeText(requireContext(), "Pridané", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.genreType.error
            Toast.makeText(requireContext(), "Žáner už existuje", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}