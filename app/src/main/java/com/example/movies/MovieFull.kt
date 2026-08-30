package com.example.movies


import java.io.Serializable

data class MovieFull(
    val id: Int,
    val title: String,
    val director: String,
    val year: Int,
    val genre: Array<String>,
    val rating: Double,
    val color: Boolean,
    val priority: Int,
    val seen_both: Boolean,
    val our_rating: Double,
    val description: String
) : Serializable