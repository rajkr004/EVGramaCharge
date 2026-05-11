package com.evgramacharge.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.evgramacharge.app.R
import com.evgramacharge.app.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val topLevel = setOf(
            R.id.mapFragment,
            R.id.bookingsFragment,
            R.id.batteryCalculatorFragment,
            R.id.hostsFragment,
        )
        val appBarConfiguration = AppBarConfiguration(topLevel)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = destination.label
        }

        lifecycleScope.launch {
            ensureAnonymousAuth()
        }
    }

    private suspend fun ensureAnonymousAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) return
        runCatching {
            auth.signInAnonymously().await()
        }.onFailure {
            Snackbar.make(
                binding.root,
                getString(R.string.error_generic),
                Snackbar.LENGTH_LONG,
            ).show()
        }
    }
}
