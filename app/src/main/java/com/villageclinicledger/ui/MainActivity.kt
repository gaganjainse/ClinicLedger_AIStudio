package com.villageclinicledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.villageclinicledger.service.BackupService
import com.villageclinicledger.ui.compose.*
import com.villageclinicledger.ui.patientdetail.viewmodel.PatientDetailViewModel
import com.villageclinicledger.ui.search.viewmodel.SearchViewModel
import com.villageclinicledger.ui.analytics.viewmodel.AnalyticsViewModel
import com.villageclinicledger.ui.theme.VillageClinicLedgerTheme
import com.villageclinicledger.ui.util.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object PatientDetail : Screen("patient_detail/{patientId}?patientName={patientName}") {
        fun createRoute(patientId: Long, patientName: String? = null): String {
            val encodedName = if (patientName != null) android.net.Uri.encode(patientName) else ""
            return "patient_detail/$patientId?patientName=$encodedName"
        }
    }
    object AddPatient : Screen("add_patient")
}

class MainActivity : AppCompatActivity() {

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var detailViewModel: PatientDetailViewModel
    private lateinit var analyticsViewModel: AnalyticsViewModel

    private var showVoiceAssistantState = mutableStateOf(value = false)

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showVoiceAssistantState.value = true
        } else {
            Toast.makeText(this, "Voice entry requires audio permission", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleManager.applyLocaleLegacy(this)
        super.onCreate(savedInstanceState)

        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        detailViewModel = ViewModelProvider(this)[PatientDetailViewModel::class.java]
        analyticsViewModel = ViewModelProvider(this)[AnalyticsViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            com.villageclinicledger.data.util.DataSeeder.seedDatabaseIfNeeded(this@MainActivity)
        }
        BackupService.scheduleBackup(this)

        setContent {
            val isHindi = remember { LocaleManager.getSavedLocale(this) == "hi" }
            
            CompositionLocalProvider(LocaleManager.LocalIsHindi provides isHindi) {
                VillageClinicLedgerTheme {
                    val navController = rememberNavController()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Search.route,
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() }
                        ) {
                            composable(Screen.Search.route) {
                                SearchScreen(
                                    viewModel = searchViewModel,
                                    analyticsViewModel = analyticsViewModel,
                                    onNavigateToDetail = { id ->
                                        navController.navigate(Screen.PatientDetail.createRoute(id))
                                    },
                                    onNavigateToAddPatient = {
                                        navController.navigate(Screen.AddPatient.route)
                                    },
                                    onOpenVoiceSheet = {
                                        triggerVoiceAssistant()
                                    },
                                    onToggleLanguage = {
                                        val currentLang = LocaleManager.getSavedLocale(this@MainActivity)
                                        val nextLang = if (currentLang == "hi") "en" else "hi"
                                        setLocale(nextLang)
                                    }
                                )
                            }
                            composable(Screen.AddPatient.route) {
                                AddPatientScreen(
                                    viewModel = searchViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateHome = {
                                        navController.navigate(Screen.Search.route) {
                                            popUpTo(Screen.Search.route) { inclusive = true }
                                        }
                                    },
                                    onPatientAdded = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(
                                route = Screen.PatientDetail.route,
                                arguments = listOf(
                                    navArgument("patientId") { type = NavType.LongType },
                                    navArgument("patientName") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L

                                PatientDetailScreen(
                                    patientId = patientId,
                                    viewModel = detailViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateHome = {
                                        navController.navigate(Screen.Search.route) {
                                            popUpTo(Screen.Search.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateToPatientDetail = { id ->
                                        navController.navigate(Screen.PatientDetail.createRoute(id))
                                    }
                                )
                            }
                        }
                    }

                    if (showVoiceAssistantState.value) {
                        VoiceInputSheetCompose(
                            onDismiss = { showVoiceAssistantState.value = false },
                            onNavigateToPatientDetail = { id ->
                                navController.navigate(Screen.PatientDetail.createRoute(id))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerVoiceAssistant() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showVoiceAssistantState.value = true
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setLocale(lang: String) {
        LocaleManager.saveLocale(this, lang)
        LocaleManager.applyLocaleLegacy(this)
        recreate()
    }
}
