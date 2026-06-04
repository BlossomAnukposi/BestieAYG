package com.bayg.services.storage.sync

import com.bayg.services.storage.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

open class SyncRepository(
    protected val db: AppDatabase,
    protected val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    protected val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    protected val uid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    protected fun userDoc() = firestore.collection("users").document(uid)
}