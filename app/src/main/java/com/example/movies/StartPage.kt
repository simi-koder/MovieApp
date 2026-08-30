package com.example.movies

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.transition.Visibility
import com.example.movies.databinding.StartPageBinding
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartPage : Fragment() {
    private var _binding: StartPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private val nameList: MutableList<String> = mutableListOf()

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StartPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        navController = findNavController()

        val userNames = listOf(binding.name1, binding.name2, binding.name3, binding.name4, binding.name5, binding.name6, binding.name7, binding.name8, binding.name9, binding.name10)

        binding.numOfUsersBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                nameList.clear()
                binding.numUsers.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                for (i in 0 until seekBar.progress) {
                    userNames[i].visibility = View.VISIBLE
                }
                for (i in seekBar.progress until 10) {
                    userNames[i].visibility = View.GONE
                }
            }
        })

        binding.doneBtn.setOnClickListener {
            if (binding.numOfUsersBar.progress == 0){
                binding.numUsers.text = "Počet užívateľov musí byť vačší ako 1"
                return@setOnClickListener
            }

            for (i in userNames) {
                if (i.isVisible) {
                    if (i.text.isNullOrBlank()){
                        i.error = "Zadaj meno"
                        return@setOnClickListener
                    }
                    nameList.add(i.text.toString())
                }
            }
            Log.d("NAMES", nameList.toString())

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Uložiť?")
                .setNegativeButton("Nie") { _, _ ->
                }
                .setPositiveButton("Áno") { _, _ ->
                    dbHelper.addUsers(nameList)
                    navController.navigate(R.id.MovieList)
                }
                .show()

//            TODO: check if all users are added everytime, undo back arrow in movie list
//            TODO: make welcome page one time only
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}