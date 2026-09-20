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

    /**
     * Get DB functions -------------------------------------------------------------------------------
     */
    /**
     * Function to load readable DB
     * @return SQLiteDatabase
     */
    fun getReadableDb(): SQLiteDatabase {
        copyDatabaseIfNeeded()
        Log.d("PATH", dbPath.toString())
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    /**
     * Function to load writeable/readable DB
     * @return SQLiteDatabase
     */
    fun getWritableDb(): SQLiteDatabase {
        copyDatabaseIfNeeded()
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    /**
     * String manipulation functions -------------------------------------------------------------------------------
     */

    /**
     * Function parses year text with comparison symbols
     * @return Pair<String, Int> where String is the symbol and Int is the year value
     ###### TODO: add span to viable string option
     */
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

    /**
     * Function parses rating text with comparison symbols
     * @return Pair<String, Int> where String is the symbol and Int is the rating
    ###### TODO: add span to viable string option
     */
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

    /**
     * Function strips diacritics from string
     * @return stripped string
     */
    fun removeDiacritics(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    /**
     * Genre functions -------------------------------------------------------------------------------
     */

    /**
     * Retrieves one genre id from Zaner table
     * @param genreName type of genre
     * @return ID of found genre or null otherwise
     */
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

    /**
     * Retrieves all genre ids from Zaner table
     * @return List of IDs of all genres
     */
    fun getAllGenreIds(): List<Int> {
        val db = getReadableDb()
        val ids = mutableListOf<Int>()

        val cursor = db.rawQuery("""
            SELECT id_zaner FROM Zaner
        """.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()) {
                ids.add(it.getInt(it.getColumnIndexOrThrow("id_zaner")))
            }
        }
        db.close()
        return ids
    }

    /**
     * Edits the name of one genre in Zaner table
     * @param id id of genre (retrieved by eg: getGenreId())
     * @param name edited type of genre
     */
    fun editGenre(
        id: Int,
        name: String
    ) {
        val db = getWritableDb()
//        Log.d("EDIT_GENRE","id: '$id'")

        val contVals = ContentValues().apply {
            put("typ", name)
        }

        db.update("Zaner", contVals, "id_zaner = $id", null)

        db.close()
    }

    /**
     * Adds new genre to Zaner table
     * @param type type of genre
     * @return False if already exists, true if inserted
     */
    fun addNewGenre(
        type: String
    ): Boolean {
        val db = getWritableDb()

        val cursor = db.rawQuery("SELECT 1 FROM Zaner WHERE typ = ?", arrayOf(type))
        val alreadyExists = cursor.moveToFirst()
        cursor.close()

        if (alreadyExists) {
            db.close()
            return false
        }

        val values = ContentValues().apply {
            put("typ", type)
        }

        val result = db.insert("Zaner", null, values)
        db.close()
        return result > 0
    }

    /**
     * Deletes genre from Zaner table and all mentions in Film_zaner_spoj table
     * @param type type of genre
     * @return True if number of deleted Zaner and Film_zaner_spoj lines were not 0
     */
    fun delGenre(
        type: String
    ) : Boolean {
        val db = getWritableDb()

        val cursor = db.rawQuery(
            "SELECT id_zaner FROM Zaner WHERE typ = ?",
            arrayOf(type)
        )

        val zanerIds = mutableListOf<Int>()

        cursor.use {
            while (it.moveToNext()) {
                zanerIds.add(it.getInt(it.getColumnIndexOrThrow("id_zaner")))
            }
        }

        if (zanerIds.isEmpty()) {
            db.close()
            return false
        }

        val placeholders = zanerIds.joinToString(",") { "?" }
        val zanerIdsArgs = zanerIds.map { it.toString() }.toTypedArray()

        val successFZJ = db.delete("Film_zaner_spoj", "id_zaner IN ($placeholders)", zanerIdsArgs)
        val successZ = db.delete("Zaner", "typ = ?", arrayOf(type))

        val cursorDebug = db.rawQuery("SELECT * FROM Zaner", null)

        cursorDebug.use {
            val columnNames = it.columnNames
            Log.d("DB_DEBUG", "Stĺpce: ${columnNames.joinToString(", ")}")

            while (it.moveToNext()) {
                val row = columnNames.joinToString(" | ") { colName ->
                    "$colName=${it.getString(it.getColumnIndexOrThrow(colName))}"
                }
                Log.d("DB_DEBUG", row)
            }
        }

        db.close()
        return successFZJ >= 0 && successZ > 0
    }

    /**
     * Retrieves all genre types
     * @return List of genre types
     */
    fun getAllGenres(): List<String>{
        val db = getReadableDb()
        val finalList = mutableListOf<String>()

        val cursor = db.rawQuery(
            """
                SELECT *
                FROM Zaner
            """.trimIndent(),
            null
        )

        cursor.use{
            while (it.moveToNext()) {

                val genre = it.getString(
                    it.getColumnIndexOrThrow("typ")
                )
//                Log.d("DB_GENRE_DEBUG", "id: '${it.getString(it.getColumnIndexOrThrow("id_zaner"))}' | '$genre' len=${genre.length} codes=${genre.map { c -> c.code }}")
                finalList.add(
                    genre
                )

            }
        }
        db.close()
        return finalList
    }

    /**
     * User functions -------------------------------------------------------------------------------
     */

    /**
     * Adds new user
     * @param name name of new user
     * @return True if insert successful
     */
    fun addNewUser(
        name: String
    ): Boolean {
        val db = getWritableDb()

        val values = ContentValues().apply {
            put("meno", name)
        }

        return (db.insert("Uzivatelia", null, values)) >= 0
    }

    /**
     * Adds list of users - used in StartPage fragment for needing confirmation
     * @param name list of usernames
     */
    fun addUsers(
        name: List<String>
    ) {
        val db = getWritableDb()

        for (name in name) {
            val values = ContentValues().apply {
                put("meno", name)
            }

            db.insert("Uzivatelia", null, values)
        }

//        DEBUG
//        val cursor = db.rawQuery(
//            """
//                SELECT * FROM Uzivatelia
//            """.trimIndent(), null
//        )
//
//        cursor.use {
//            while (it.moveToNext()) {
//                Log.d("DEBUG_USER", it.getString(it.getColumnIndexOrThrow("meno")))
//            }
//        }
//        END DEBUG

        db.close()
    }

    /**
     * Retrieves all users
     * @return List of usernames
     */
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
//        Log.d("USER_NAMES", names.toString())

        db.close()
        return names
    }

    /**
     * Adds new lines to Videl table indicating users saw given movie
     * @param userIds List of user ids that saw a movie
     * @param movieId id of a movie users have seen
     */
    fun addUserSeen(
        userIds: List<Int>,
        movieId: Int
    ) {
        val db = getWritableDb()

        for (id in userIds) {
            val values = ContentValues().apply {
                put("id_user", id)
                put("id_film", movieId)
            }

            db.insert("Videl", null, values)
        }

        db.close()
    }

    /**
     * Helper function deletes users from Videl table
     * @param userIds List of user ids to delete from Videl (Saw) table
     * @param movieId
     */
    fun delUserSeen(
        userIds: List<Int>,
        movieId: Int?
    ) {
        if (userIds.isEmpty()) return
        movieId ?: return

        val db = getWritableDb()

        val placeholders = userIds.joinToString(",") { "?" }
        val args = (listOf(movieId.toString()) + userIds.map { it.toString() }).toTypedArray()

        db.delete(
            "Videl",
            "id_film = ? AND id_user IN ($placeholders)",
            args
        )

        db.close()
    }

    /**
     * Retrieves users that seen given movie
     * @param movieId current movie
     * @return List of usernames that saw given movie
     */
    fun getUserSeenMovie(
        movieId: Int
    ) : List<String> {
        val db = getReadableDb()

        val cursor = db.rawQuery(
            """
                SELECT u.meno FROM Uzivatelia u
                JOIN Videl v ON u.id_user = v.id_user
                WHERE v.id_film = $movieId
            """.trimIndent(), null
        )

        val userNamesSeenMovie = mutableListOf<String>()

        cursor.use {
            while (it.moveToNext()) {
                userNamesSeenMovie.add(it.getString(it.getColumnIndexOrThrow("meno")))
            }
        }

        db.close()
        return userNamesSeenMovie
    }

    /**
     * Deletes users from Uzivatelia and Videl
     * @param names List of usernames to delete
     */
    fun delUsers (
        names: List<String>
    ) {
        val db = getWritableDb()

        val placeholders = names.joinToString(",") { "?" }

        db.delete(
            "Uzivatelia",
            "meno IN ($placeholders)",
            names.toTypedArray()
        )
        db.delete(
            "Videl",
            "Videl.id_user IN (SELECT Uzivatelia.id_user FROM Uzivatelia WHERE Uzivatelia.meno IN ($placeholders))",
            names.toTypedArray()
        )

        db.close()
    }

    /**
     * Retrieves all user ids
     * @param names List of usernames
     * @return List of user ids
     */
    fun getUserIds (
        names: List<String>
    ): List<Int> {
        val db = getReadableDb()

        val placeholders = names.joinToString(",") { "?" }
        val userIds = mutableListOf<Int>()

        val cursor = db.rawQuery(
            """
                SELECT id_user FROM Uzivatelia
                WHERE meno IN ($placeholders)
            """.trimIndent(), names.toTypedArray()
        )

        cursor.use {
            while (it.moveToNext()) {
                userIds.add(it.getInt(it.getColumnIndexOrThrow("id_user")))
                Log.d("GET_USER_ID", it.getString(it.getColumnIndexOrThrow("id_user")))
            }
        }
        db.close()
        return userIds
    }

    /**
     * Movie functions -------------------------------------------------------------------------------
     */

    /**
     * Retrieves movies from database with LIKE title
     * Loads movies to MovieFull class
     * @param title of a movie
     * @return List of movies with similar titles
     */
    fun getMovieByTitle(
        title: String
    ): List<MovieFull> {
        val db = getReadableDb()
        val movies = mutableListOf<MovieFull>()

        val cursor = db.rawQuery("""
            SELECT f.*, GROUP_CONCAT(z.typ, ', ') AS genre_concat
            FROM Filmy f
            JOIN Film_zaner_spoj fz ON fz.id_film = f.id_film
            JOIN Zaner z ON z.id_zaner = fz.id_zaner
            WHERE f.nazov LIKE '%$title%'
            GROUP BY f.id_film
        """.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()){
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

    /**
     * Retrieves movie from database by ID
     * Used in FullMovie fragment where movie by given id must be found
     * @param id of a movie
     * @return MovieFull
     */
    fun getMovieById(
        id: Int
    ): MovieFull {
        val db = getReadableDb()
//        Log.d("MOVIE_ID", "id: $id")
        lateinit var movie: MovieFull

        val cursor = db.rawQuery(
            """
                SELECT f.*, GROUP_CONCAT(z.typ, ', ') AS genre_concat
                FROM Filmy f
                JOIN Film_zaner_spoj fz ON fz.id_film = f.id_film
                JOIN Zaner z ON z.id_zaner = fz.id_zaner
                WHERE f.id_film = ?
                """.trimIndent(), arrayOf(id.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                movie = MovieFull(
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
            }
        }
        db.close()
        return movie
    }

    /**
     * Retrieves all movies from database
     * @return List of all movies loaded in MovieFull classes
     */
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

    /**
     * Filters existing movie list by director
     * @param director Name of the director, filtered by containing
     * @param moviesList List of current movie list
     * @return Filtered movie list containing only movies with similar director names
     */
    fun filterByDirector(director: String, moviesList: List<MovieFull>): List<MovieFull> {
        var i = 0
        val movies = mutableListOf<MovieFull>()
        while (i < moviesList.size) {
            if (removeDiacritics(moviesList[i].director.lowercase()).contains(removeDiacritics(director.lowercase()))) movies.add(moviesList[i])
            i += 1
        }
        return movies
    }

    /**
     * Filters database
     * @param genreListRaw list of genre types, movie must include at least one
     * @param seenUsers list of user ids that seen the movie
     * @param allSaw check if all users have seen the move
     * @param year of publication with comparison symbols
     * @param rating with comparison symbols
     * @param color boolean is movie colored?
     * @param grayscale boolean is movie grayscale?
     * @param director name
     * @return List of filtered movies
     */
    fun getMoviesByFilters(
        genreListRaw: List<String>,
        seenUsers: List<Int> = emptyList(),
        allSaw: Boolean = false,
        year: String = "",
        rating: String = "",
        color: Boolean = false,
        grayscale: Boolean = false,
        director: String = ""
    ): List<MovieFull> {

        val genreList = genreListRaw.map { it.trim() }.distinct()
        val db = getReadableDb()
        val movieList = mutableListOf<MovieFull>()

        val extraArgs = mutableListOf<String>()

        val extraConditions = StringBuilder()

        if (seenUsers.isNotEmpty()) {
            val placeholders = seenUsers.joinToString(",") { "?" }
            extraConditions.append("""
                AND f.id_film IN (
                    SELECT Videl.id_film
                    FROM Videl
                    WHERE Videl.id_user IN ($placeholders)
                    GROUP BY Videl.id_film
                    HAVING COUNT(DISTINCT Videl.id_user) = ${seenUsers.size}
                )
            """.trimIndent())
            extraArgs.addAll(seenUsers.map { it.toString() })
        } else {
            extraConditions.append("""
                AND f.id_film NOT IN (
                    SELECT Videl.id_film
                    FROM Videl
                )
            """.trimIndent())
        }

        if (allSaw) extraConditions.append(" AND f.videne_spolu = 1") else extraConditions.append(" AND f.videne_spolu = 0")
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
        ORDER BY f.priorita DESC
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

    /**
     * Searches for similar movie names in current movie list
     * @param title of the movie
     * @param moviesList current movie list
     * @return Filtered movie list
     */
    fun searchMovieByName(title: String, moviesList: List<MovieFull>): List<MovieFull> {
        var i = 0
        val movies = mutableListOf<MovieFull>()
        while(i < moviesList.size) {
            if (removeDiacritics(moviesList[i].title.lowercase()).contains(removeDiacritics(title.lowercase()))) movies.add(moviesList[i])
            i = i + 1
        }
        return movies
    }

    /**
     * Picks a random movie in current movie list
     * @param moviesList current movie list
     * @return One movie if movie list is not empty
     */
    fun getRandomMovie(moviesList: List<MovieFull>): MovieFull? {
        if (moviesList.isEmpty()) return null
        return moviesList.random()
    }

    /**
     * Adds a new movie, its genres and which users have seen it
     * @param title of the movie
     * @param director name
     * @param rating by IMDB
     * @param year of publication
     * @param color boolean - colorized?
     * @param genreIds list of genre ids
     * @param allSaw boolean - all users saw this movie?
     * @param priority Int null - 10
     * @param description optional string
     * @param seenArray list of user ids that saw the movie
     * @return id of newly added movie
     */
    fun addMovie(
        title: String,
        director: String,
        rating: Double,
        year: Int,
        color: Boolean = false,
        genreIds: List<Int>,
        allSaw: Boolean = false,
        priority: Int,
        description: String,
        seenArray: List<Int>
    ): Long {
        val db = getWritableDb()

        val values = ContentValues().apply {
            put("nazov", title)
            put("reziser", director)
            put("hodnotenie", rating)
            put("rok_vydania", year)
            put("videne_spolu", if (allSaw) 1 else 0)
            put("priorita", priority)
            put("farba", color)
            put("popis", description)
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
            for (id in seenArray) {
                val linkValuesNames = ContentValues().apply {
                    put("id_film", newMovieId)
                    put("id_user", id)
                }
                db.insert("Videl", null, linkValuesNames)
            }
        }

        db.close()
        return newMovieId
    }

    /**
     * Deletes movie by title
     * #### TODO change function to resolve title ambiguity
     */
    fun deleteMovie(title: String): Boolean {
        val db = getWritableDb()

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
            return false
        }

        db.delete("Videl", "id_film = ?", arrayOf(movieId.toString()))

        db.delete("Film_zaner_spoj", "id_film = ?", arrayOf(movieId.toString()))

        val rowsDeleted = db.delete("Filmy", "id_film = ?", arrayOf(movieId.toString()))

        db.close()
        return rowsDeleted > 0
    }

    /**
     * Edits an existing movie
     * @param title of the movie
     * @param director name
     * @param rating on IMDB
     * @param year of publication
     * @param genreIds new list of genre ids
     * @param allSaw boolean - all users saw the movie?
     * @param priority Int null - 10
     * @param color boolean - colorized?
     * @param our_rating Float 0 - 10
     * @param description optional text
     * @param movieToEdit MovieFull
     * @return true
     */
    fun editMovie(
        title: String,
        director: String,
        rating: Double,
        year: Int,
        genreIds: List<Int>,
        allSaw: Boolean,
        priority: Int,
        color: Boolean,
        our_rating: Double,
        description: String,
        movieToEdit: MovieFull
    ): Boolean {

        val db = getWritableDb()

        return try {

            val movieId = movieToEdit.id

            val values = ContentValues()

            if (title.isNotBlank()) {
                values.put("nazov", title)
            }

            if (director.isNotBlank()) {
                values.put("reziser", director)
            }

            if (rating != null) {
                values.put("hodnotenie", rating)
            }

            if (year != null) {
                values.put("rok_vydania", year)
            }

            if (allSaw != null) {
                values.put("videne_spolu", if (allSaw) 1 else 0)
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

            if (values.size() > 0) {
                db.update(
                    "Filmy",
                    values,
                    "id_film = ?",
                    arrayOf(movieId.toString())
                )
            }

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


