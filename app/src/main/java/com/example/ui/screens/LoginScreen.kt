package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.RoleUtilisateur
import com.example.ui.theme.RepairFlowTheme
import com.example.ui.viewmodel.RepairFlowViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: RepairFlowViewModel
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var loginInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("admin123") }
    var nomInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf(RoleUtilisateur.TECHNICIEN.name) }
    var passwordVisible by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    val roles = listOf(
        RoleUtilisateur.ADMINISTRATEUR.name,
        RoleUtilisateur.RECEPTIONNISTE.name,
        RoleUtilisateur.TECHNICIEN.name
    )

    RepairFlowTheme(darkTheme = isDark) {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("login_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top language & theme toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("login_lang_btn")
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = lang.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.toggleDarkTheme() },
                            modifier = Modifier.testTag("login_theme_btn")
                        ) {
                            Icon(
                                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Thème"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Logo & App Name
                    Image(
                        painter = painterResource(id = R.drawable.repairflow_logo),
                        contentDescription = "Logo RepairFlow",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RepairFlow",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "RÉPARER  •  GÉRER  •  FACTURER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (lang == AppLanguage.FR) "Système de gestion et de facturation d'appareils" else "Electronic Device Management & Invoicing System",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isRegisterMode) {
                                    if (lang == AppLanguage.FR) "Créer un compte atelier" else "Create Workshop Account"
                                } else {
                                    if (lang == AppLanguage.FR) "Connexion Sécurisée" else "Secure Sign In"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (authError != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = authError ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = nomInput,
                                    onValueChange = { nomInput = it },
                                    label = { Text(if (lang == AppLanguage.FR) "Nom complet *" else "Full Name *") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenuBox(
                                    expanded = roleMenuExpanded,
                                    onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = roleInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (lang == AppLanguage.FR) "Rôle dans l'atelier" else "Role") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = roleMenuExpanded,
                                        onDismissRequest = { roleMenuExpanded = false }
                                    ) {
                                        roles.forEach { r ->
                                            DropdownMenuItem(
                                                text = { Text(r) },
                                                onClick = {
                                                    roleInput = r
                                                    roleMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = loginInput,
                                onValueChange = {
                                    loginInput = it
                                    viewModel.clearAuthError()
                                },
                                label = { Text(if (lang == AppLanguage.FR) "Identifiant ou Email *" else "Login or Email *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_identifier_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    viewModel.clearAuthError()
                                },
                                label = { Text(if (lang == AppLanguage.FR) "Mot de passe *" else "Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Afficher mot de passe"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (isRegisterMode) {
                                            viewModel.register(nomInput, loginInput, passwordInput, roleInput)
                                        } else {
                                            viewModel.login(loginInput, passwordInput)
                                        }
                                    }
                                )
                            )

                            Button(
                                onClick = {
                                    if (isRegisterMode) {
                                        viewModel.register(nomInput, loginInput, passwordInput, roleInput)
                                    } else {
                                        viewModel.login(loginInput, passwordInput)
                                    }
                                },
                                enabled = !isAuthLoading && loginInput.isNotBlank() && (!isRegisterMode || nomInput.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_submit_button")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRegisterMode) {
                                            if (lang == AppLanguage.FR) "S'inscrire" else "Sign Up"
                                        } else {
                                            if (lang == AppLanguage.FR) "Se connecter" else "Sign In"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        isRegisterMode = !isRegisterMode
                                        viewModel.clearAuthError()
                                    }
                                ) {
                                    Text(
                                        text = if (isRegisterMode) {
                                            if (lang == AppLanguage.FR) "Déjà un compte ? Se connecter" else "Already have an account? Sign In"
                                        } else {
                                            if (lang == AppLanguage.FR) "Nouveau collaborateur ? S'inscrire" else "New staff member? Register"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick access demo accounts
                    Text(
                        text = if (lang == AppLanguage.FR) "Comptes démo de l'atelier (accès rapide) :" else "Workshop demo accounts (1-click access):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Admin quick login
                        DemoAccountCard(
                            roleTitle = "Admin",
                            userName = "Alexandre",
                            icon = Icons.Default.AdminPanelSettings,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                loginInput = "admin"
                                passwordInput = "admin123"
                                viewModel.login("admin", "admin123")
                            }
                        )

                        // Reception quick login
                        DemoAccountCard(
                            roleTitle = "Accueil",
                            userName = "Sophie",
                            icon = Icons.Default.SupportAgent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                loginInput = "reception"
                                passwordInput = "rec123"
                                viewModel.login("reception", "rec123")
                            }
                        )

                        // Technician quick login
                        DemoAccountCard(
                            roleTitle = "Technicien",
                            userName = "Thomas",
                            icon = Icons.Default.Build,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                loginInput = "thomas"
                                passwordInput = "tech123"
                                viewModel.login("thomas", "tech123")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DemoAccountCard(
    roleTitle: String,
    userName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = roleTitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
