package com.example.movies

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream
import java.text.Normalizer
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "new2_database.db"
        const val DB_VERSION = 1
    }

    private val dbPath = context.getDatabasePath(DB_NAME).path
    private val context = context

    fun copyDatabaseIfNeeded() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun getReadableDb(): SQLiteDatabase {
        copyDatabaseIfNeeded()
        Log.d("PATH", dbPath.toString())
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun parseYearFilter(input: String): Pair<String, Int>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val allowedOps = listOf(">=", "<=", ">", "<", "=")

        val op = allowedOps.firstOrNull { trimmed.startsWith(it) }

        return if (op != null) {
            val numberPart = trimmed.removePrefix(op).trim()
            val year = numberPart.toIntOrNull() ?: return null
            op to year
        } else {
            val year = trimmed.toIntOrNull() ?: return null
            "=" to year
        }
    }

    private fun parseRatingFilter(input: String): Pair<String, Double>? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val allowedOps = listOf(">=", "<=", ">", "<", "=")

        val op = allowedOps.firstOrNull { trimmed.startsWith(it) }

        return if (op != null) {
            val numberPart = trimmed.removePrefix(op).trim()
            val rating = numberPart.toDoubleOrNull() ?: return null
            op to rating
        } else {
            val rating = trimmed.toDoubleOrNull() ?: return null
            "=" to rating
        }
    }

    fun addUsers(
        name: List<String>
    ) {
        val db = getWritableDb()

        for (name in name) {
            val values = ContentValues().apply {
                put("meno", name)
            }

            val newUserId = db.insert("Uzivatelia", null, values)
        }

//        DEBUG
        val cursor = db.rawQuery(
            """
                SELECT * FROM Uzivatelia
            """.trimIndent(), null
        )

        cursor.use {
            while (it.moveToNext()) {
                Log.d("DEBUG_USER", it.getString(it.getColumnIndexOrThrow("meno")))
            }
        }
//        END DEBUG

        db.close()
    }

//    fun delUsersAll(){
//        val db = getWritableDb()
//        db.rawQuery(
//            """
//                DELETE FROM Uzivatelia;
//            """.trimIndent(), null
//        )
//        db.close()
//    }

    fun getUsers (): List<String> {
        val db = getReadableDb()

        val cursor = db.rawQuery(
            """
                SELECT id_user, meno FROM Uzivatelia
                ORDER BY id_user ASC
            """.trimIndent(), null
        )

        val names = mutableListOf<String>()

        cursor.use {
            while (it.moveToNext()) {
                names.add(it.getString(it.getColumnIndexOrThrow("meno")))
            }
        }
        Log.d("USER_NAMES", names.toString())

        db.close()
        return names
    }

//    TODO: pridat pridavanie do videl tabulky

    fun getAllMovies(): List<MovieFull> {
        val db = getReadableDb()
        val movies = mutableListOf<MovieFull>()

        val cursor = db.rawQuery(
            """
                SELECT 
                    f.*,
                    GROUP_CONCAT(z.typ, ', ') AS genre_concat
                FROM Filmy f
                JOIN Film_zaner_spoj fz ON fz.id_film = f.id_film
                JOIN Zaner z ON z.id_zaner = fz.id_zaner
                GROUP BY f.id_film
                ORDER BY f.priorita DESC
                """.trimIndent(),
            null
        )

        cursor.use {
            while (it.moveToNext()) {

                val genreString = it.getString(
                    it.getColumnIndexOrThrow("genre_concat")
                )

                movies.add(
                    MovieFull(
                        id = it.getInt(it.getColumnIndexOrThrow("id_film")),
                        title = it.getString(it.getColumnIndexOrThrow("nazov")),
                        director = it.getString(it.getColumnIndexOrThrow("reziser")),
                        year = it.getInt(it.getColumnIndexOrThrow("rok_vydania")),
                        rating = it.getDouble(it.getColumnIndexOrThrow("hodnotenie")),
                        genre = it.getString(it.getColumnIndexOrThrow("genre_concat"))
                            .split(",").map { g -> g.trim() }.toTypedArray(),
                        color = it.getInt(it.getColumnIndexOrThrow("farba")) == 1,
                        priority = it.getInt(it.getColumnIndexOrThrow("priorita")),
                        seen_both = it.getInt(it.getColumnIndexOrThrow("videne_spolu")) == 1,
                        our_rating = it.getDouble(it.getColumnIndexOrThrow("nase_hodnotenie")),
                        description = it.getString(it.getColumnIndexOrThrow("popis"))
                    )
                )
            }
        }
        db.close()
        return movies
    }

    fun getAllGenres(): List<String>{
        val db = getReadableDb()
        val finalList = mutableListOf<String>()

        val cursor = db.rawQuery(
            """
                SELECT typ
                FROM Zaner
            """.trimIndent(),
            null
        )

        cursor.use{
            while (it.moveToNext()) {

                val genre = it.getString(
                    it.getColumnIndexOrThrow("typ")
                )

                Log.d("DB_GENRE_DEBUG", "'$genre' len=${genre.length} codes=${genre.map { c -> c.code }}")

                finalList.add(
                    genre
                )

            }
        }
        db.close()
        return finalList
    }

    fun filterByDirector(director: String, moviesList: List<MovieFull>): List<MovieFull> {
        var i = 0
        val movies = mutableListOf<MovieFull>()
        while (i < moviesList.size) {
            if (removeDiacritics(moviesList[i].director.lowercase()).contains(removeDiacritics(director.lowercase()))) movies.add(moviesList[i])
            i = i + 1
        }
        return movies
    }

    fun getMoviesByFilters(
        genreListRaw: List<String>,
        videlSimi: Boolean = false,
        videlaTerka: Boolean = false,
        videneSpolu: Boolean = false,
        year: String = "",
        rating: String = "",
        color: Boolean = false,
        grayscale: Boolean = false,
        director: String = ""
    ): List<MovieFull> {

        val genreList = genreListRaw.map { it.trim() }.distinct()
        val db = getReadableDb()
        val movieList = mutableListOf<MovieFull>()

        // args pre WHERE 1=1 podmienky (poradie musí sedieť s poradím pridávania do SQL!)
        val extraArgs = mutableListOf<String>()

        val extraConditions = StringBuilder()
        if (videlSimi) extraConditions.append(" AND f.videl_simi = 1")
        if (videlaTerka) extraConditions.append(" AND f.videla_terka = 1")
        if (videneSpolu) extraConditions.append(" AND f.videne_spolu = 1")
        if (year.isNotBlank()) {
            val parsed = parseYearFilter(year)
            if (parsed != null) {
                val (op, yearValue) = parsed
                extraConditions.append(" AND f.rok_vydania $op ?")
                extraArgs.add(yearValue.toString())
            }
        }
        if (rating.isNotBlank()) {
            val parsedRating = parseRatingFilter(rating)
            if (parsedRating != null) {
                val (op1, ratingValue) = parsedRating
                extraConditions.append(" AND f.hodnotenie $op1 ?")
                extraArgs.add(ratingValue.toString())
            }
        }
        if (director.isNotBlank()) {

        }

        if (!(color && grayscale)) {
            if (color) extraConditions.append(" AND f.farba = 1")
            if (grayscale) extraConditions.append(" AND f.farba = 0")
        }
        val genreFilterSql: String
        val genreArgs = mutableListOf<String>()

        if (genreList.isNotEmpty()) {
            val placeholders = genreList.joinToString(",") { "?" }
            genreFilterSql = """
            AND f.id_film IN (
                SELECT fz2.id_film
                FROM Film_zaner_spoj fz2
                JOIN Zaner z2 ON z2.id_zaner = fz2.id_zaner
                WHERE z2.typ IN ($placeholders)
                GROUP BY fz2.id_film
                
            )
        """.trimIndent()
            genreArgs.addAll(genreList)
        } else {
            genreFilterSql = ""
        }

        val query = """
        SELECT
        f.*,
        GROUP_CONCAT(z.typ, ', ') AS genre_concat
        FROM Filmy f
        JOIN Film_zaner_spoj fz ON fz.id_film = f.id_film
        JOIN Zaner z ON z.id_zaner = fz.id_zaner
        WHERE 1=1
        $genreFilterSql
        $extraConditions
        GROUP BY f.id_film
    """.trimIndent()

        val args = (genreArgs + extraArgs).toTypedArray()

        Log.d("QUERY", query)
        Log.d("QUERY_ARGS", args.toList().toString())

        val cursor = db.rawQuery(query, args)

        cursor.use {
            while (it.moveToNext()) {
                movieList.add(
                    MovieFull(
                        id = it.getInt(it.getColumnIndexOrThrow("id_film")),
                        title = it.getString(it.getColumnIndexOrThrow("nazov")),
                        director = it.getString(it.getColumnIndexOrThrow("reziser")),
                        year = it.getInt(it.getColumnIndexOrThrow("rok_vydania")),
                        rating = it.getDouble(it.getColumnIndexOrThrow("hodnotenie")),
                        genre = it.getString(it.getColumnIndexOrThrow("genre_concat"))
                            .split(",").map { g -> g.trim() }.toTypedArray(),
                        color = it.getInt(it.getColumnIndexOrThrow("farba")) == 1,
                        priority = it.getInt(it.getColumnIndexOrThrow("priorita")),
                        seen_both = it.getInt(it.getColumnIndexOrThrow("videne_spolu")) == 1,
                        our_rating = it.getDouble(it.getColumnIndexOrThrow("nase_hodnotenie")),
                        description = it.getString(it.getColumnIndexOrThrow("popis"))
                    )
                )
            }
        }

        db.close()
        return if (director.isNotBlank()) {
            filterByDirector(director, movieList)
        } else {
            movieList
        }
    }

    fun removeDiacritics(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    fun searchMovieByName(title: String, moviesList: List<MovieFull>): List<MovieFull> {
        var i = 0
        val movies = mutableListOf<MovieFull>()
        while(i < moviesList.size) {
            if (removeDiacritics(moviesList[i].title.lowercase()).contains(removeDiacritics(title.lowercase()))) movies.add(moviesList[i])
            i = i + 1
        }
        return movies
    }

    fun getRandomMovie(moviesList: List<MovieFull>): MovieFull? {
        if (moviesList.isEmpty()) return null
        return moviesList.random()
    }
    fun getWritableDb(): SQLiteDatabase {
        copyDatabaseIfNeeded()
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun getGenreId(genreName: String): Int? {
        val db = getReadableDb()
        val cursor = db.rawQuery(
            "SELECT id_zaner FROM Zaner WHERE typ = ?",
            arrayOf(genreName)
        )
        var id: Int? = null
        cursor.use {
            if (it.moveToFirst()) {
                id = it.getInt(it.getColumnIndexOrThrow("id_zaner"))
            }
        }
        db.close()
        return id
    }

    fun addMovie(
        title: String,
        director: String,
        rating: Double,
        year: Int,
        color: Boolean = false,
        genreIds: List<Int>,
        videlSimi: Boolean = false,
        videlaTerka: Boolean = false,
        videneSpolu: Boolean = false,
        priority: Int
    ): Long {
        val db = getWritableDb()

        val values = ContentValues().apply {
            put("nazov", title)
            put("reziser", director)
            put("hodnotenie", rating)
            put("rok_vydania", year)
            put("videl_simi", if (videlSimi) 1 else 0)
            put("videla_terka", if (videlaTerka) 1 else 0)
            put("videne_spolu", if (videneSpolu) 1 else 0)
            put("priorita", priority)
            put("farba", color)
        }

        val newMovieId = db.insert("Filmy", null, values)

        if (newMovieId != -1L) {
            for (genreId in genreIds) {
                val linkValues = ContentValues().apply {
                    put("id_film", newMovieId)
                    put("id_zaner", genreId)
                }
                db.insert("Film_zaner_spoj", null, linkValues)
            }
        }

        db.close()
        return newMovieId
    }

    fun deleteMovie(title: String): Boolean {
        val db = getWritableDb()

        // najprv zisti id_film podľa názvu
        val cursor = db.rawQuery(
            "SELECT id_film FROM Filmy WHERE nazov = ?",
            arrayOf(title)
        )

        var movieId: Int? = null
        cursor.use {
            if (it.moveToFirst()) {
                movieId = it.getInt(it.getColumnIndexOrThrow("id_film"))
            }
        }

        if (movieId == null) {
            db.close()
            return false // film s daným názvom neexistuje
        }

        // vymaž väzby v spojovacej tabuľke
        db.delete("Film_zaner_spoj", "id_film = ?", arrayOf(movieId.toString()))

        // vymaž samotný film
        val rowsDeleted = db.delete("Filmy", "id_film = ?", arrayOf(movieId.toString()))

        db.close()
        return rowsDeleted > 0
    }

    fun editMovie(
        title: String,
        director: String,
        rating: Double,
        year: Int,
        genreIds: List<Int>,
        videlSimi: Boolean,
        videlaTerka: Boolean,
        videneSpolu: Boolean,
        priority: Int,
        color: Boolean,
        our_rating: Double,
        description: String,
        movieToEdit: MovieFull?
    ): Boolean {

        if (movieToEdit == null) {
            return false
        }

        val db = getWritableDb()

        return try {

            val movieId = movieToEdit.id

            // Aktualizujeme iba hodnoty, ktoré boli zadané
            val values = ContentValues()

            if (!title.isNullOrBlank()) {
                values.put("nazov", title)
            }

            if (!director.isNullOrBlank()) {
                values.put("reziser", director)
            }

            if (rating != null) {
                values.put("hodnotenie", rating)
            }

            if (year != null) {
                values.put("rok_vydania", year)
            }

            if (videlSimi != null) {
                values.put("videl_simi", if (videlSimi) 1 else 0)
            }

            if (videlaTerka != null) {
                values.put("videla_terka", if (videlaTerka) 1 else 0)
            }

            if (videneSpolu != null) {
                values.put("videne_spolu", if (videneSpolu) 1 else 0)
            }

            if (priority != null) {
                values.put("priorita", priority)
            }

            if (color != null) {
                values.put("farba", if (color) 1 else 0)
            }

            if (our_rating != null) {
                values.put("nase_hodnotenie", our_rating)
            }


            values.put("popis", description)


            // Aktualizuj film iba ak máme čo meniť
            if (values.size() > 0) {
                db.update(
                    "Filmy",
                    values,
                    "id_film = ?",
                    arrayOf(movieId.toString())
                )
            }

            // Žánre zmeň iba vtedy, ak ich používateľ zadal
            if (genreIds != null) {

                db.delete(
                    "Film_zaner_spoj",
                    "id_film = ?",
                    arrayOf(movieId.toString())
                )

                for (genreId in genreIds) {

                    val linkValues = ContentValues().apply {
                        put("id_film", movieId)
                        put("id_zaner", genreId)
                    }

                    db.insert(
                        "Film_zaner_spoj",
                        null,
                        linkValues
                    )
                }
            }

            true

        } finally {
            db.close()
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}


