package com.example.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.movies.databinding.AddUserBinding

class AddUser : Fragment() {
    private var _binding: AddUserBinding? = null

    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()
        dbHelper = DatabaseHelper(requireContext())

        val names = listOf(binding.user1, binding.user2, binding.user3, binding.user4, binding.user5,
            binding.user6, binding.user7, binding.user8, binding.user9, binding.user10)

        dbHelper.getUsers().forEachIndexed { index, string ->
            names[index].visibility = View.VISIBLE
            names[index].text = string
        }

        binding.addUserBtn.setOnClickListener {
            if (binding.addUserName.text.isNullOrBlank()){
                binding.addUserBtn.error = "Zadaj meno"
                return@setOnClickListener
            }

            if (!dbHelper.addNewUser(binding.addUserName.text.toString())){
                binding.addUserBtn.error
                binding.addUserBtn.text= "Maximálny počet užívateľov dosiahnutý (10)"
                return@setOnClickListener
            }

            binding.addUserName.text.clear()

            parentFragmentManager.beginTransaction()
                .detach(this)
                .commitNow()

            parentFragmentManager.beginTransaction()
                .attach(this)
                .commit()
        }

        binding.delUserBtn.setOnClickListener {
            var namesToDel = mutableListOf<String>()
            for (name in names) {
                if (name.isChecked) namesToDel.add(name.text.toString())
                name.isChecked = false
            }
            dbHelper.delUsers(namesToDel)

            parentFragmentManager.beginTransaction()
                .detach(this)
                .commitNow()

            parentFragmentManager.beginTransaction()
                .attach(this)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}