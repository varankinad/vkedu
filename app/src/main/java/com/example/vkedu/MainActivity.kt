package com.example.vkedu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vkedu.ui.theme.VkeduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VkeduTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IntentsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun IntentsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    val hasText = text.isNotBlank()
    val hasValidPhone = isValidRuPhone(text)
    val showEmptyError = text.isNotEmpty() && !hasText
    val showPhoneError = text.isNotBlank() && !hasValidPhone && text.any { it.isDigit() }
    val isError = showEmptyError || showPhoneError

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.main_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.main_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.main_input_label)) },
            isError = isError,
            supportingText = when {
                showEmptyError -> {
                    { Text(stringResource(R.string.error_empty)) }
                }
                showPhoneError -> {
                    { Text(stringResource(R.string.error_phone)) }
                }
                else -> null
            }
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(context, SecondActivity::class.java)
                        .putExtra(SecondActivity.EXTRA_MESSAGE, text.trim())
                )
            },
            enabled = hasText,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_open_second))
        }
        Button(
            onClick = {
                runCatching { context.startActivity(dialIntent(text)) }
                    .onFailure {
                        Toast.makeText(
                            context,
                            R.string.error_dial_no_app,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            },
            enabled = hasValidPhone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_call))
        }
        Button(
            onClick = {
                runCatching { context.startActivity(shareTextIntent(text.trim())) }
                    .onFailure {
                        Toast.makeText(
                            context,
                            R.string.error_share_no_app,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            },
            enabled = hasText,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_share))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntentsScreenPreview() {
    VkeduTheme {
        IntentsScreen()
    }
}
