package com.example.mid_termtest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mid_termtest.ui.theme.MidtermTestTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MidtermTestTheme {
                var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                var userRole by remember { mutableStateOf<String?>(null) }
                val db = Firebase.firestore

                // Lấy thông tin Role từ Firestore khi đăng nhập thành công
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        db.collection("users").document(currentUser!!.uid).get()
                            .addOnSuccessListener { userRole = it.getString("role") ?: "user" }
                    } else { userRole = null }
                }

                if (currentUser == null) {
                    AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
                } else {
                    HomeScreen(role = userRole ?: "user", onLogout = { currentUser = null })
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF2D3E50)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("HỆ THỐNG GHI CHÚ", color = Color(0xFFFFD54F), fontSize = 28.sp, fontWeight = FontWeight.Bold)

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFFFFD54F), divider = {}) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("EMAIL", modifier = Modifier.padding(16.dp)) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("PHONE", modifier = Modifier.padding(16.dp)) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedTab == 0) {
            AuthTextField(email, { email = it }, "Email", Icons.Default.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(password, { password = it }, "Mật khẩu", Icons.Default.Lock, true)
        } else {
            AuthTextField(phoneNumber, { phoneNumber = it }, "Số điện thoại", Icons.Default.Phone)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (selectedTab == 0 && email.isNotEmpty() && password.isNotEmpty()) {
                    if (isRegisterMode) {
                        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                            val newUser = User(uid = auth.currentUser!!.uid, email = email, role = "user")
                            db.collection("users").document(newUser.uid).set(newUser)
                            onAuthSuccess()
                        }.addOnFailureListener { Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                    } else {
                        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener { onAuthSuccess() }
                            .addOnFailureListener { Toast.makeText(context, "Sai tài khoản/mật khẩu", Toast.LENGTH_SHORT).show() }
                    }
                } else if (selectedTab == 1) {
                    Toast.makeText(context, "Tính năng Phone OTP cần cấu hình Firebase SMS", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isRegisterMode) "ĐĂNG KÝ" else "ĐĂNG NHẬP", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký tại đây", color = Color.White)
        }
    }
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFFFD54F)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFD54F), unfocusedBorderColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
}