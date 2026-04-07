package com.example.mid_termtest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(role: String, onLogout: () -> Unit) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid ?: "user_default"

    var notes by remember { mutableStateOf(listOf<Note>()) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") } // Ô nhập link ảnh mới

    var editingNote by remember { mutableStateOf<Note?>(null) }

    // Tự động load dữ liệu theo quyền Admin/User
    LaunchedEffect(role, currentUserId) {
        val query = if (role == "admin") db.collection("notes")
        else db.collection("notes").whereEqualTo("userId", currentUserId)

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

    // --- DIALOG XEM CHI TIẾT & SỬA LINK ẢNH ---
    if (editingNote != null) {
        var editTitle by remember { mutableStateOf(editingNote!!.title) }
        var editDesc by remember { mutableStateOf(editingNote!!.description) }
        var editImageUrl by remember { mutableStateOf(editingNote!!.imageUrl) }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("Chi tiết & Chỉnh sửa") },
            text = {
                Column {
                    if (editImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = editImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Tiêu đề") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = { Text("Link ảnh mới (URL)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("notes").document(editingNote!!.id).update(
                            "title", editTitle,
                            "description", editDesc,
                            "imageUrl", editImageUrl
                        )
                        editingNote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) { Text("Cập nhật", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) { Text("Đóng") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (role == "admin") "Admin Panel" else "My Notes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    }
                }
            )
        },
        containerColor = Color(0xFFF7F9FB)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {

            // --- KHU VỰC NHẬP LINK ẢNH VÀ LƯU ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("Tiêu đề ghi chú...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    TextField(
                        value = desc, onValueChange = { desc = it },
                        placeholder = { Text("Nội dung chi tiết...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )
                    // Ô NHẬP LINK ẢNH
                    TextField(
                        value = imageUrlInput, onValueChange = { imageUrlInput = it },
                        placeholder = { Text("Dán URL hình ảnh tại đây...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )

                    if (imageUrlInput.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrlInput,
                            contentDescription = "Preview",
                            modifier = Modifier.size(100.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                db.collection("notes").add(hashMapOf(
                                    "title" to title,
                                    "description" to desc,
                                    "imageUrl" to imageUrlInput,
                                    "userId" to currentUserId
                                ))
                                title = ""; desc = ""; imageUrlInput = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                    ) {
                        Text("Lưu Note", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DANH SÁCH HIỂN THỊ ---
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editingNote = note },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(note.description, color = Color.Gray, fontSize = 14.sp, maxLines = 2)
                                }
                                IconButton(onClick = { db.collection("notes").document(note.id).delete() }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}