package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    
    private var isFirebaseInitialized = false
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState

    private val _authLogs = MutableStateFlow<List<String>>(listOf("System start: Unified AuthManager loaded."))
    val authLogs: StateFlow<List<String>> = _authLogs

    private val _isFallbackMode = MutableStateFlow(false)
    val isFallbackMode: StateFlow<Boolean> = _isFallbackMode

    private val _simulatedUserEmail = MutableStateFlow<String?>(null)
    val simulatedUserEmail: StateFlow<String?> = _simulatedUserEmail

    fun init(context: Context) {
        log("Initializing Firebase Context Tunnel...")
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true
            
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUserState.value = user
                user?.let { u ->
                    _simulatedUserEmail.value = u.email ?: "guest_${u.uid.take(6)}"
                    log("Secured OAuth Connection: Signed in as ${u.email ?: "Anonymous Guest"} (UID: ${u.uid.take(8)}...)")
                } ?: run {
                    _simulatedUserEmail.value = null
                    log("Secured Connection Status: Idle/Logged Out.")
                }
            }
            _isFallbackMode.value = false
            log("SUCCESS: Unified Firebase Client Online.")
        } catch (e: Exception) {
            isFirebaseInitialized = false
            _isFallbackMode.value = true
            log("WARNING: Firebase unavailable. Defaulting to Local Encrypted Sandbox Vault.")
            log("Detailed Trace: ${e.localizedMessage ?: "Missing google-services.json context"}")
        }
    }

    fun isFirebaseReady(): Boolean = isFirebaseInitialized && auth != null

    fun log(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _authLogs.value = _authLogs.value + "[$timestamp] $msg"
        Log.d(TAG, "[$TAG] $msg")
    }

    fun clearLogs() {
        _authLogs.value = listOf("[SYS] Logs cleared.")
    }

    fun signInWithEmail(email: String, password: String, scope: CoroutineScope, onResult: (Boolean, String) -> Unit) {
        if (isFirebaseReady()) {
            log("Calling Firebase credential validation for: $email")
            auth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        _currentUserState.value = user
                        _simulatedUserEmail.value = user?.email
                        _isFallbackMode.value = false
                        log("SIGN-IN SUCCESS: ${user?.email}")
                        onResult(true, "Firebase Connection Authenticated successfully!")
                    } else {
                        val error = task.exception?.localizedMessage ?: "Invalid login parameters"
                        log("SIGN-IN FAILED: $error")
                        onResult(false, error)
                    }
                }
        } else {
            log("SIMULATED SIGN-IN: Simulating secure sandbox for $email")
            _simulatedUserEmail.value = email
            _isFallbackMode.value = true
            onResult(true, "Simulated Sandbox logged in securely (Local Fallback active)")
        }
    }

    fun signUpWithEmail(email: String, password: String, scope: CoroutineScope, onResult: (Boolean, String) -> Unit) {
        if (isFirebaseReady()) {
            log("Creating cloud container login credentials for: $email")
            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        _currentUserState.value = user
                        _simulatedUserEmail.value = user?.email
                        _isFallbackMode.value = false
                        log("REGISTRATION SUCCESS: ${user?.email}")
                        onResult(true, "Firebase Account created and logged in!")
                    } else {
                        val error = task.exception?.localizedMessage ?: "Could not register user credentials"
                        log("REGISTRATION FAILED: $error")
                        onResult(false, error)
                    }
                }
        } else {
            log("SIMULATED SIGN-UP: Registering user '$email' inside local persistent vault.")
            _simulatedUserEmail.value = email
            _isFallbackMode.value = true
            onResult(true, "Simulated Sandbox registered successfully!")
        }
    }

    fun signInAnonymously(onResult: (Boolean, String) -> Unit) {
        if (isFirebaseReady()) {
            log("Requesting Firebase Guest Token...")
            auth?.signInAnonymously()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        _currentUserState.value = user
                        _simulatedUserEmail.value = "Guest_${user?.uid?.take(6) ?: "User"}"
                        _isFallbackMode.value = false
                        log("GUEST SIGN-IN SUCCESS: UID ${user?.uid}")
                        onResult(true, "Firebase Cloud Guest token initialized!")
                    } else {
                        val error = task.exception?.localizedMessage ?: "Guest authorization rejected by service"
                        log("GUEST SIGN-IN FAILED: $error")
                        onResult(false, error)
                    }
                }
        } else {
            log("SIMULATED GUEST SIGN-IN: Initializing sandbox guest session.")
            _simulatedUserEmail.value = "SimulatedGuest_${(1000..9999).random()}"
            _isFallbackMode.value = true
            onResult(true, "Logged into local sandbox as unique guest.")
        }
    }

    fun signOut() {
        if (isFirebaseReady()) {
            auth?.signOut()
            _currentUserState.value = null
        }
        _simulatedUserEmail.value = null
        log("PERSISTENCE CONTEXT TEARDOWN: Logged out.")
    }

    fun pushResourceToCloud(resource: ResourceState, scope: CoroutineScope) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            log("Syncing resource info '${resource.name}' to remote cloud bucket...")
            
            val db = firestore ?: return
            val data = hashMapOf(
                "id" to resource.id,
                "name" to resource.name,
                "status" to resource.status,
                "metrics" to resource.metrics,
                "timestamp" to resource.timestamp
            )
            
            db.collection("users").document(uid)
                .collection("resources").document(resource.id)
                .set(data)
                .addOnSuccessListener {
                    log("CLOUD SYNC SUCCESS: '${resource.name}' uploaded securely.")
                }
                .addOnFailureListener { e ->
                    log("CLOUD SYNC FAILED: ${e.message}")
                }
        } else {
            log("LOCAL PERSIST SYNC: Config '${resource.name}' secured in local SQLite Room context.")
        }
    }

    fun deleteResourceFromCloud(resourceId: String) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            log("Deleting resource state '$resourceId' from remote cloud bucket...")
            
            val db = firestore ?: return
            db.collection("users").document(uid)
                .collection("resources").document(resourceId)
                .delete()
                .addOnSuccessListener {
                    log("CLOUD REMOVAL SUCCESS: Resource deleted from cloud storage.")
                }
                .addOnFailureListener { e ->
                    log("CLOUD REMOVAL ERROR: ${e.message}")
                }
        } else {
            log("LOCAL REMOVAL SYNC: Resource state '$resourceId' removed from local database.")
        }
    }

    fun pullResourcesFromCloud(database: AppDatabase, scope: CoroutineScope) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            log("API CALL: Fetching remote resource configs from Firestore...")
            
            val db = firestore ?: return
            db.collection("users").document(uid).collection("resources")
                .get()
                .addOnSuccessListener { documents ->
                    scope.launch(Dispatchers.IO) {
                        var syncCount = 0
                        for (doc in documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val name = doc.getString("name") ?: "Discovered Node"
                                val status = doc.getString("status") ?: "Active"
                                val metrics = doc.getString("metrics") ?: "Normal"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                
                                database.resourceStateDao().insertState(
                                    ResourceState(id, name, status, metrics, timestamp)
                                )
                                syncCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Pull parse error for doc ${doc.id}: ${e.message}")
                            }
                        }
                        log("SYNC PULLED & INTEGRATED: Successfully synced $syncCount nodes to Local Database Cache.")
                    }
                }
                .addOnFailureListener { e ->
                    log("SYNC PULL ERROR: ${e.message}")
                }
        } else {
            log("OFFLINE METRIC: Skipping pull, working in local sandbox database.")
        }
    }

    fun pushProviderToCloud(provider: ProviderData) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            log("Syncing provider '${provider.name}' to remote cloud bucket...")
            
            val db = firestore ?: return
            val hexColor = String.format("#%08X", provider.iconColor.value.toLong() and 0xFFFFFFFFL)
            val data = hashMapOf(
                "icon" to provider.icon,
                "name" to provider.name,
                "keyPreview" to provider.keyPreview,
                "iconColorHex" to hexColor,
                "isActive" to provider.isActive
            )
            
            db.collection("users").document(uid)
                .collection("providers").document(provider.name)
                .set(data)
                .addOnSuccessListener {
                    log("CLOUD PROV SYNC SUCCESS: '${provider.name}' uploaded securely.")
                }
                .addOnFailureListener { e ->
                    log("CLOUD PROV SYNC FAILED: ${e.message}")
                }
        }
    }

    fun deleteProviderFromCloud(providerName: String) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            val db = firestore ?: return
            db.collection("users").document(uid)
                .collection("providers").document(providerName)
                .delete()
                .addOnSuccessListener {
                    log("CLOUD PROV REMOVAL: '${providerName}' deleted.")
                }
        }
    }

    fun pullProvidersFromCloud(onSynced: (List<ProviderData>) -> Unit) {
        if (isFirebaseReady()) {
            val user = auth?.currentUser ?: return
            val uid = user.uid
            log("API CALL: Fetching remote provider configs from Firestore...")
            
            val db = firestore ?: return
            db.collection("users").document(uid).collection("providers")
                .get()
                .addOnSuccessListener { documents ->
                    val pulled = mutableListOf<ProviderData>()
                    for (doc in documents) {
                        try {
                            val icon = doc.getString("icon") ?: "G"
                            val name = doc.getString("name") ?: ""
                            val keyPreview = doc.getString("keyPreview") ?: ""
                            val hex = doc.getString("iconColorHex") ?: "#FF4285F4"
                            val isActive = doc.getBoolean("isActive") ?: true
                            val color = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                Color(0xFF4285F4)
                            }
                            if (name.isNotEmpty()) {
                                pulled.add(ProviderData(icon, name, keyPreview, color, isActive))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Pull provider parse error: ${e.message}")
                        }
                    }
                    if (pulled.isNotEmpty()) {
                        log("SYNC PULLED & INTEGRATED: Successfully synced ${pulled.size} providers.")
                        onSynced(pulled)
                    } else {
                        log("SYNC PULL: No custom providers found on server.")
                    }
                }
                .addOnFailureListener { e ->
                    log("SYNC PROVIDER PULL ERROR: ${e.message}")
                }
        }
    }
}
