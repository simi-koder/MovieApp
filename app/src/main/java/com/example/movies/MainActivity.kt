package com.example.movies

    import android.annotation.SuppressLint
    import android.content.Intent
    import android.os.Bundle
    import androidx.appcompat.app.AppCompatActivity
    import androidx.activity.enableEdgeToEdge
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.navigation.findNavController
    import androidx.navigation.fragment.NavHostFragment
    import androidx.navigation.ui.AppBarConfiguration
    import androidx.navigation.ui.navigateUp
    import androidx.navigation.ui.setupActionBarWithNavController
    import android.view.Menu
    import android.view.MenuItem
    import androidx.core.content.FileProvider
    import androidx.navigation.NavController
    import com.example.movies.databinding.ActivityMainBinding


    @SuppressLint("WrongViewCast")
    class MainActivity : AppCompatActivity() {

        private lateinit var appBarConfiguration: AppBarConfiguration
        private lateinit var binding: ActivityMainBinding

        private lateinit var navController: NavController

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            setSupportActionBar(binding.toolbar)

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            navController = navHostFragment.navController

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)

            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

            navGraph.setStartDestination(
                if (hasSeenOnboarding) R.id.MovieList else R.id.StartPage
            )

            navController.graph = navGraph

            appBarConfiguration = AppBarConfiguration(
                setOf(R.id.MovieList, R.id.StartPage)
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
        }

        private fun exportDatabase() {
            val dbFile = getDatabasePath("new2_database.db")
            val dbUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                dbFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, dbUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Zdieľať databázu"))
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_movie -> {
                navController.navigate(R.id.AddEditMovie)
                true
            }

            R.id.action_user -> {
                navController.navigate(R.id.AddUser)
                true
            }

            R.id.action_send_db -> {
                exportDatabase()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}