package com.bayg.data.settings

/**
 * Public profile information for the signed-in user.
 *
 * Firestore path: `users/{uid}/profile/main`.
 *
 * [displayName] is the free-form "name" from the sign-up wireframe, not a
 * unique handle. Two users may share the same display name. If unique
 * handles are needed later, add a `/usernames/{handle}` collection with a
 * transactional uniqueness check.
 *
 * All fields have defaults so Firestore can deserialise documents written
 * by older clients.
 */
data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val consent: ConsentRecord = ConsentRecord(),
) {
    companion object {
        const val COLLECTION = "profile"
        const val DOCUMENT_ID = "main"
    }
}

/** Last recorded acceptance of the privacy policy. */
data class ConsentRecord(
    val privacyPolicyVersion: String = "",
    val acceptedAt: Long = 0L,
    val analyticsOptIn: Boolean = false,
)
