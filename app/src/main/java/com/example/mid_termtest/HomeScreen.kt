package com.example.mid_termtest

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(role: String) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val storage = Firebase.storage.reference
    val currentUserId = auth.currentUser?.uid ?: "user_default"

    var notes by remember { mutableStateOf(listOf<Note>()) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(role, currentUserId) {
        val query = if (role == "admin") {
            db.collection("notes")
        } else {
            db.collection("notes").whereEqualTo("userId", currentUserId)
        }

        query.addSnapshotListener { value, _ ->
            if (value != null) {
                notes = value.documents.map { doc ->
                    Note(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        userId = doc.getString("userId") ?: ""
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (role == "admin") {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (role == "admin") "Admin Panel" else "My Notes", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { auth.signOut() }) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    }
                }
            )
        },
        containerColor = Color(0xFFF7F9FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sửa lỗi TextField candidates bằng cách dùng thuộc tính chuẩn
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Tiêu đề ghi chú...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )

                    TextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("Nội dung chi tiết...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    selectedImageUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF5F6368))
                        }

                        Button(
                            onClick = {
                                if (title.isNotEmpty()) {
                                    isUploading = true
                                    val fileName = "notes/${UUID.randomUUID()}.jpg"
                                    if (selectedImageUri != null) {
                                        val fileRef = storage.child(fileName)
                                        fileRef.putFile(selectedImageUri!!).addOnSuccessListener {
                                            fileRef.downloadUrl.addOnSuccessListener { uri ->
                                                db.collection("notes").add(hashMapOf(
                                                    "title" to title,
                                                    "description" to desc,
                                                    "imageUrl" to uri.toString(),
                                                    "userId" to currentUserId
                                                ))
                                                title = ""; desc = ""; selectedImageUri = null; isUploading = false
                                            }
                                        }
                                    } else {
                                        db.collection("notes").add(hashMapOf(
                                            "title" to title, "description" to desc, "imageUrl" to "", "userId" to currentUserId
                                        ))
                                        title = ""; desc = ""; isUploading = false
                                    }
                                }
                            },
                            enabled = !isUploading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                        ) {
                            if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Lưu Note", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            if (note.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = note.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(note.description, color = Color.Gray, fontSize = 14.sp)
                                    if (role == "admin") {
                                        Text("Owner UID: ${note.userId.take(8)}...", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                                IconButton(onClick = { db.collection("notes").document(note.id).delete() }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}