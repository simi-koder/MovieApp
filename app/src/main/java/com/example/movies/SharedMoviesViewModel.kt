package com.example.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class SharedMoviesViewModel : ViewModel() {

    private val _movies = MutableLiveData<List<MovieFull>>(emptyList())
    val movies: LiveData<List<MovieFull>> get() = _movies

    fun setMovies(newMovies: List<MovieFull>) {
        _movies.value = newMovies
    }

    fun getCurrentMovies(): List<MovieFull> {
        return _movies.value ?: emptyList()
    }
}