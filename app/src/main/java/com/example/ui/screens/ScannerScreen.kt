package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.LabelOcrEngine
import com.example.data.OcrExtractionResult
import com.example.model.GroqConfigState
import com.example.model.HealthProfile
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ScorePillBadge
import com.example.ui.theme.LocalSemanticRiskColors
import com.example.viewmodel.ScannerUiState
import kotlinx.coroutines.launch
import java.io.File

private data class QuickScanSample(
    val chipLabel: String,
    val productName: String,
    val ingredients: String
)

private val QuickSamples = listOf(
    QuickScanSample(
        chipLabel = "Instant Noodles",
        productName = "Spicy Masala Instant Noodles",
        ingredients = "Refined wheat flour (Maida), Refined palm oil, Iodized salt (Sodium 980mg/100g), Sugar, Hydrolysed groundnut protein, Flavor enhancers (INS 621 Monosodium Glutamate, INS 635), Acidity regulators (INS 500, INS 451), Caramel color (INS 150d), Preservative (INS 211 Sodium Benzoate). Calories: 455 kcal, Sugar: 5g, Protein: 8g, Fiber: 2g."
    ),
    QuickScanSample(
        chipLabel = "NPK Fertilizer",
        productName = "SuperGrow NPK 20-20-20",
        ingredients = "Ammonium Nitrate (20% Nitrogen), Monoammonium Phosphate (20% Phosphorus), Potassium Chloride (20% Potash), Synthetic EDTA chelates, Trace Copper Sulfate, Sodium Borate, Anti-caking chemical dye."
    ),
    QuickScanSample(
        chipLabel = "Plastic Packaging",
        productName = "Clear Food Takeout Container (#7 PC)",
        ingredients = "Polycarbonate Resin Code #7, Bisphenol-A (BPA) monomer, Di(2-ethylhexyl) phthalate (DEHP) plasticizer, Petroleum-based non-biodegradable polymer coating, Synthetic PFAS grease-resistant liner."
    )
)

@Composable
fun ScannerScreen(
    scannerUiState: ScannerUiState,
    groqConfig: GroqConfigState,
    healthProfile: HealthProfile?,
    recentScans: List<ScanRecord>,
    onAnalyze: (productNameHint: String, ingredientText: String, bitmap: Bitmap?) -> Unit,
    onDismissError: () -> Unit,
    onSelectScan: (ScanRecord) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenGroqSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    var productNameHint by remember { mutableStateOf("") }
    var ingredientText by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    // Automatic on-device OCR state
    var isRunningOcr by remember { mutableStateOf(false) }
    var lastOcrResult by remember { mutableStateOf<OcrExtractionResult?>(null) }
    var showingFullOcrText by remember { mutableStateOf(false) }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    var ocrIsError by remember { mutableStateOf(false) }

    fun triggerAutoOcr(uri: Uri?, bitmap: Bitmap?) {
        if (uri == null && bitmap == null) return
        coroutineScope.launch {
            isRunningOcr = true
            ocrIsError = false
            ocrStatusMessage = "Reading ingredients from photo (Auto OCR)..."
            val result = LabelOcrEngine.extractFromImage(
                context = context,
                imageUri = uri,
                fallbackBitmap = bitmap
            )
            result.fold(
                onSuccess = { extracted ->
                    lastOcrResult = extracted
                    showingFullOcrText = false
                    ingredientText = extracted.extractedIngredients
                    if (productNameHint.isBlank() && !extracted.detectedProductName.isNullOrBlank()) {
                        productNameHint = extracted.detectedProductName
                    }
                    ocrIsError = false
                    ocrStatusMessage = "Auto-OCR extracted ingredients (${extracted.blockCount} text blocks) and filled the field below."
                    onDismissError()
                },
                onFailure = { err ->
                    lastOcrResult = null
                    ocrIsError = true
                    ocrStatusMessage = err.localizedMessage
                        ?: "No clear text detected. Try a closer, sharper photo or type ingredients manually."
                }
            )
            isRunningOcr = false
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            val bmp = decodeBitmapFromUri(context, uri)
            capturedBitmap = bmp
            activeImageUri = uri
            onDismissError()
            triggerAutoOcr(uri, bmp)
        }
    }

    fun launchCameraCapture() {
        val uri = createTempImageUri(context)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionNotice = null
            launchCameraCapture()
        } else {
            permissionNotice = "Camera permission denied. You can still upload a label photo or type ingredients manually."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = decodeBitmapFromUri(context, uri)
            capturedBitmap = bmp
            activeImageUri = uri
            permissionNotice = null
            onDismissError()
            triggerAutoOcr(uri, bmp)
        }
    }

    val isAnalyzing = scannerUiState is ScannerUiState.Analyzing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Auto-Detect Category strip
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Smart Multi-Category Scanner",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
                Text(
                    text = "Auto-detects Packaged Foods, Agricultural Fertilizers, and Packaging Materials with WHO, FSSAI & EPA standards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCategory.entries.forEach { cat ->
                        CategoryBadge(category = cat)
                    }
                }
            }

            // Groq Free Tier AI Engine Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (groqConfig.isReadyForAi) semantic.excellentBg.copy(alpha = 0.7f) else colors.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (groqConfig.isReadyForAi) colors.primary.copy(alpha = 0.4f) else semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGroqSettings() }
                    .testTag("scanner_groq_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(21.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (groqConfig.isReadyForAi) {
                                "Groq Free AI: ${groqConfig.selectedModelId}"
                            } else {
                                "Connect Free Groq API Key (Optional)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (groqConfig.isReadyForAi) {
                                val m = groqConfig.selectedModel
                                "RPM ${m?.rpm} • RPD ${m?.rpd} • TPM ${m?.tpm} • TPD ${m?.tpd} — Tap to switch"
                            } else {
                                "Tap to enter your Groq key or get a free key at console.groq.com/keys"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Personal Health Profile Active Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.softEmeraldSurface),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile() }
                    .testTag("scanner_profile_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        val activeCount = (healthProfile?.allergies?.size ?: 0) +
                            (healthProfile?.medicalConditions?.size ?: 0) +
                            (healthProfile?.chemicalSensitivities?.size ?: 0)
                        Text(
                            text = if (activeCount > 0) "Personal Protection Active ($activeCount triggers)" else "Set Up Your Personal Health Profile",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onPrimaryContainer
                        )
                        Text(
                            text = if (activeCount > 0) {
                                "Every scan automatically flags conflicts with your saved profile."
                            } else {
                                "Tap to add allergies, diabetes, hypertension, or chemical sensitivities."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Label Photo Capture / Upload Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Scan Ingredient Label Photo (Auto OCR)",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Ingredients are automatically read from your photo and filled below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    if (capturedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, semantic.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Selected product label photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    capturedBitmap = null
                                    activeImageUri = null
                                    lastOcrResult = null
                                    ocrStatusMessage = null
                                    ocrIsError = false
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface.copy(alpha = 0.9f))
                                    .testTag("remove_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove photo",
                                    tint = colors.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    launchCameraCapture()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            enabled = !isAnalyzing && !isRunningOcr,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, colors.primary),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Take Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isAnalyzing && !isRunningOcr,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, semantic.cardBorder),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Upload Photo")
                        }
                    }

                    // Live Auto-OCR Progress / Result Banner
                    AnimatedVisibility(visible = isRunningOcr || ocrStatusMessage != null) {
                        val bg = when {
                            isRunningOcr -> semantic.softEmeraldSurface
                            ocrIsError -> semantic.moderateBg
                            else -> semantic.excellentBg
                        }
                        val fg = when {
                            isRunningOcr -> colors.primary
                            ocrIsError -> semantic.moderate
                            else -> semantic.excellent
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("ocr_status_banner"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isRunningOcr) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (ocrIsError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = fg,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = ocrStatusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!isRunningOcr && (lastOcrResult != null || capturedBitmap != null)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (lastOcrResult != null) {
                                        TextButton(
                                            onClick = {
                                                val ocr = lastOcrResult ?: return@TextButton
                                                showingFullOcrText = !showingFullOcrText
                                                ingredientText = if (showingFullOcrText) {
                                                    ocr.fullRawText
                                                } else {
                                                    ocr.extractedIngredients
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("ocr_toggle_full_text_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DocumentScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (showingFullOcrText) "Use Ingredients Only" else "Use Full Label Text",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { triggerAutoOcr(activeImageUri, capturedBitmap) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("ocr_rescan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Re-scan OCR", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    if (permissionNotice != null) {
                        Text(
                            text = permissionNotice.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.moderate
                        )
                    }
                }
            }

            // Manual Ingredient Entry Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Or Enter Product & Ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )

                    OutlinedTextField(
                        value = productNameHint,
                        onValueChange = { productNameHint = it },
                        label = { Text("Product Name / Brand (optional)") },
                        placeholder = { Text("e.g. Whole Wheat Biscuits, NPK 20-20-20") },
                        singleLine = true,
                        enabled = !isAnalyzing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input")
                    )

                    OutlinedTextField(
                        value = ingredientText,
                        onValueChange = {
                            ingredientText = it
                            onDismissError()
                        },
                        label = { Text("Ingredient List or Composition") },
                        placeholder = { Text("Paste ingredients, E-numbers/INS codes, nutrition values, or resin codes...") },
                        minLines = 3,
                        maxLines = 6,
                        enabled = !isAnalyzing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingredient_text_input")
                    )

                    // Quick-fill test examples across all 3 categories
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Try a sample label:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickSamples.forEach { sample ->
                                FilterChip(
                                    selected = productNameHint == sample.productName,
                                    onClick = {
                                        productNameHint = sample.productName
                                        ingredientText = sample.ingredients
                                        onDismissError()
                                    },
                                    label = { Text(sample.chipLabel) },
                                    modifier = Modifier.testTag("sample_chip_${sample.chipLabel.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }
            }

            // Error / Analyzing Status
            AnimatedVisibility(visible = scannerUiState is ScannerUiState.Error) {
                val errMsg = (scannerUiState as? ScannerUiState.Error)?.message.orEmpty()
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = semantic.badBg),
                    border = BorderStroke(1.dp, semantic.bad.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = semantic.bad
                        )
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.bad,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Primary Analyze Button
            Button(
                onClick = {
                    onAnalyze(productNameHint, ingredientText, capturedBitmap)
                },
                enabled = !isAnalyzing && !isRunningOcr && (ingredientText.isNotBlank() || capturedBitmap != null),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("analyze_product_button")
            ) {
                if (isAnalyzing || isRunningOcr) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = colors.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isRunningOcr) "Extracting Ingredients (OCR)..." else (scannerUiState as ScannerUiState.Analyzing).stepLabel,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (groqConfig.isReadyForAi) "Analyze with Groq Free AI" else "Analyze Ingredients",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Recent Scans Quick List
            if (recentScans.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Recent Scans",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground
                )
                recentScans.take(3).forEach { scan ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectScan(scan) }
                            .testTag("recent_scan_card_${scan.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scan.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${scan.brand.ifBlank { "Generic" }} • ${scan.parsedCategory().shortLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            ScorePillBadge(score = scan.score, ratingLabel = scan.ratingLabel)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "label_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
}
