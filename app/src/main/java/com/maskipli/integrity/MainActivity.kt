package com.maskipli.integrity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.maskipli.integrity.ui.theme.AppIntegrityTheme
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppIntegrityTheme {

                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                var nonce by remember { mutableStateOf(TextFieldValue()) }
                var token by remember { mutableStateOf(TextFieldValue()) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item {
                        ClickableText(
                            text = AnnotatedString("DeviceId = " + getDeviceId()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = TextStyle.Default.copy(textAlign = TextAlign.Center),
                            onClick = {
                                context.copyToClipboard(getDeviceId())
                            }
                        )

                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp, color = Color.Black,
                                    RoundedCornerShape(size = 8.dp)
                                )
                                .padding(16.dp),
                            value = nonce,
                            onValueChange = {
                                nonce = it
                            },
                            singleLine = false
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    token = TextFieldValue(generateToken(nonce.text).orEmpty())
                                }
                            }
                        ) {
                            Text(text = getString(R.string.generate_token))
                        }

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )

                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp, color = Color.Black,
                                    RoundedCornerShape(size = 8.dp)
                                )
                                .padding(16.dp),
                            value = token,
                            onValueChange = {
                                token = it
                            },
                            singleLine = false
                        )

                        Button(
                            enabled = token.text.isNotBlank(),
                            onClick = {
                                context.copyToClipboard(token.annotatedString)
                            }
                        ) {
                            Text(text = getString(R.string.copy_token))
                        }
                    }
                }
            }
        }
    }

    private suspend fun generateToken(nonce: String): String? = suspendCoroutine { continuation ->
        val integrityManager = IntegrityManagerFactory.create(application)
        val task: Task<IntegrityTokenResponse> = integrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setCloudProjectNumber(0L) // todo please change gcp project id here
                .setNonce(nonce)
                .build()
        )
        task.addOnCompleteListener {
            if (it.isSuccessful) continuation.resume(it.result.token())
            else continuation.resume(null)
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun Context.copyToClipboard(text: CharSequence) {
        val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("label", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(this, "copied!", Toast.LENGTH_SHORT).show()
    }
}
