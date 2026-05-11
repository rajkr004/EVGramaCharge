package com.evgramacharge.app

import android.app.Application
import com.evgramacharge.app.data.repository.FirestoreRepository

class EVGramaChargeApplication : Application() {

    val repository: FirestoreRepository by lazy { FirestoreRepository() }
}
