package com.example.feature.addproduct

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.format.CurrencyFormatter
import com.example.core.preferences.ThemeMode
import com.example.designsystem.components.ExpressiveAnalyzingOrb
import com.example.designsystem.components.ExpressiveChip
import com.example.designsystem.components.ExpressiveSectionHeader
import com.example.designsystem.components.PixelBentoCard
import com.example.designsystem.components.WishDropErrorState
import com.example.designsystem.components.WishProductArtwork
import com.example.designsystem.icon.WishDropIcons
import com.example.designsystem.theme.WishDropDesign
import com.example.designsystem.theme.WishDropTheme
import com.example.domain.model.AddInputMode
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.GiftRecipient

@Composable
fun AddProductRouteScreen(
  initialMode: String,
  onNavigateBack: () -> Unit,
  onProductSaved: (String) -> Unit,
  viewModel: AddProductViewModel = hiltViewModel(),
) {
  LaunchedEffect(initialMode) { viewModel.setInitialMode(initialMode) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  AddProductScreenContent(
    state = state,
    onNavigateBack = onNavigateBack,
    onSelectMode = viewModel::selectMode,
    onUrlChanged = viewModel::updateUrl,
    onManualChanged = viewModel::updateManualFields,
    onBarcodeChanged = viewModel::updateBarcode,
    onAnalyze = viewModel::startExtraction,
    onUpdateDraft = viewModel::updateDraftField,
    onSelectRecipient = viewModel::selectDraftRecipient,
    onResetToInput = viewModel::resetToInput,
    onSaveDraft = { viewModel.saveReviewedDraft(onProductSaved) },
  )
}

@Composable
fun AddProductScreenContent(
  state: AddProductUiState,
  onNavigateBack: () -> Unit,
  onSelectMode: (AddInputMode) -> Unit,
  onUrlChanged: (String) -> Unit,
  onManualChanged: (String, String, String, String) -> Unit,
  onBarcodeChanged: (String) -> Unit,
  onAnalyze: () -> Unit,
  onUpdateDraft: ((ExtractedProductDraft) -> ExtractedProductDraft) -> Unit,
  onSelectRecipient: (GiftRecipient) -> Unit,
  onResetToInput: () -> Unit,
  onSaveDraft: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = WishDropDesign.spacing
  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = spacing.maxSheetWidth)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("add_back_button"),
            ) {
              Icon(
                imageVector = WishDropIcons.ArrowBack,
                contentDescription = stringResource(R.string.add_back),
              )
            }
          }
          Text(
            text = stringResource(R.string.add_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
          )
        }

        AnimatedContent(
          targetState = state.step,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "add_product_step_transition",
          modifier = Modifier.weight(1f),
        ) { step ->
          when (step) {
            AddFlowStep.Input -> {
              AddInputStepContent(
                state = state,
                onSelectMode = onSelectMode,
                onUrlChanged = onUrlChanged,
                onManualChanged = onManualChanged,
                onBarcodeChanged = onBarcodeChanged,
                onAnalyze = onAnalyze,
              )
            }
            is AddFlowStep.Processing -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ExpressiveAnalyzingOrb(
                  stepDescription = step.stepLabel,
                  progress = step.progress,
                )
              }
            }
            is AddFlowStep.Review -> {
              ReviewDraftContent(
                draft = step.draft,
                recipients = state.recipients,
                onUpdateDraft = onUpdateDraft,
                onSelectRecipient = onSelectRecipient,
                onStartOver = onResetToInput,
                onSave = onSaveDraft,
              )
            }
            is AddFlowStep.Error -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WishDropErrorState(
                  message = step.message,
                  onRetry = onAnalyze,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddInputStepContent(
  state: AddProductUiState,
  onSelectMode: (AddInputMode) -> Unit,
  onUrlChanged: (String) -> Unit,
  onManualChanged: (String, String, String, String) -> Unit,
  onBarcodeChanged: (String) -> Unit,
  onAnalyze: () -> Unit,
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(20.dp),
    contentPadding = PaddingValues(bottom = 32.dp),
  ) {
    item(key = "mode_selector", contentType = "mode_selector") {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val modes =
          listOf(
            Triple(AddInputMode.URL, stringResource(R.string.mode_url), WishDropIcons.AddLink),
            Triple(
              AddInputMode.MANUAL,
              stringResource(R.string.mode_manual),
              WishDropIcons.ManualEntry,
            ),
            Triple(
              AddInputMode.BARCODE,
              stringResource(R.string.mode_barcode),
              WishDropIcons.BarcodeScan,
            ),
          )
        modes.forEachIndexed { idx, (mode, label, icon) ->
          SegmentedButton(
            selected = state.mode == mode,
            onClick = { onSelectMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
            icon = {
              Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("add_mode_${mode.name.lowercase()}"),
          ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
          }
        }
      }
    }

    when (state.mode) {
      AddInputMode.URL -> {
        item(key = "url_card", contentType = "input_card") {
          PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              OutlinedTextField(
                value = state.urlInput,
                onValueChange = onUrlChanged,
                label = { Text(stringResource(R.string.url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_url_input"),
              )
              Text(
                text = stringResource(R.string.url_quick_samples),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                val samples =
                  listOf(
                    "LEGO Bonsai" to "https://www.lego.com/en-hu/product/bonsai-tree",
                    "Pixel Buds 2a" to "https://store.google.com/product/pixel_buds_2a",
                    "Nothing Ear" to "https://alza.hu/nothing-ear",
                  )
                samples.forEachIndexed { idx, (name, sampleUrl) ->
                  ExpressiveChip(
                    label = name,
                    colorIndex = idx,
                    onClick = { onUrlChanged(sampleUrl) },
                  )
                }
              }
            }
          }
        }
      }
      AddInputMode.MANUAL -> {
        item(key = "manual_card", contentType = "input_card") {
          PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OutlinedTextField(
                value = state.manualName,
                onValueChange = {
                  onManualChanged(it, state.manualStore, state.manualPrice, state.manualNotes)
                },
                label = { Text(stringResource(R.string.manual_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_manual_name_input"),
              )
              OutlinedTextField(
                value = state.manualStore,
                onValueChange = {
                  onManualChanged(state.manualName, it, state.manualPrice, state.manualNotes)
                },
                label = { Text(stringResource(R.string.manual_store_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_manual_store_input"),
              )
              OutlinedTextField(
                value = state.manualPrice,
                onValueChange = {
                  onManualChanged(state.manualName, state.manualStore, it, state.manualNotes)
                },
                label = { Text(stringResource(R.string.manual_price_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_manual_price_input"),
              )
              OutlinedTextField(
                value = state.manualNotes,
                onValueChange = {
                  onManualChanged(state.manualName, state.manualStore, state.manualPrice, it)
                },
                label = { Text(stringResource(R.string.manual_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
      AddInputMode.BARCODE -> {
        item(key = "barcode_card", contentType = "input_card") {
          BarcodeScannerInputCard(
            barcode = state.barcodeInput,
            onBarcodeChanged = onBarcodeChanged,
          )
        }
      }
    }

    item(key = "analyze_cta", contentType = "cta") {
      Button(
        onClick = onAnalyze,
        shape = WishDropDesign.radii.medium,
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .minimumInteractiveComponentSize()
            .testTag("add_analyze_button"),
      ) {
        Icon(imageVector = WishDropIcons.Sparkle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_analyze_ai),
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarcodeScannerInputCard(
  barcode: String,
  onBarcodeChanged: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraPermission by rememberSaveable {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.barcode_camera_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )

      if (hasCameraPermission) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(180.dp)
              .clip(WishDropDesign.radii.medium)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          AndroidView(
            factory = { ctx ->
              PreviewView(ctx).apply {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener(
                  {
                    runCatching {
                      val cameraProvider = cameraProviderFuture.get()
                      val preview =
                        CameraPreview.Builder().build().also {
                          it.surfaceProvider = surfaceProvider
                        }
                      cameraProvider.unbindAll()
                      cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                      )
                    }
                  },
                  ContextCompat.getMainExecutor(ctx),
                )
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.width(210.dp)
                .height(92.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, WishDropDesign.radii.small)
          )
        }
      } else {
        FilledTonalButton(
          onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("barcode_request_camera_button"),
        ) {
          Icon(imageVector = WishDropIcons.Camera, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.barcode_grant_camera))
        }
      }

      OutlinedTextField(
        value = barcode,
        onValueChange = onBarcodeChanged,
        label = { Text(stringResource(R.string.barcode_manual_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("add_barcode_input"),
      )

      Text(
        text = stringResource(R.string.barcode_presets_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val codes =
          listOf(
            "0840353900240 • Pixel Buds" to "0840353900240",
            "5702017153162 • LEGO Bonsai" to "5702017153162",
            "5060696460512 • Nothing Ear" to "5060696460512",
          )
        codes.forEachIndexed { idx, (label, ean) ->
          ExpressiveChip(
            label = label,
            colorIndex = idx,
            onClick = { onBarcodeChanged(ean) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewDraftContent(
  draft: ExtractedProductDraft,
  recipients: List<GiftRecipient>,
  onUpdateDraft: ((ExtractedProductDraft) -> ExtractedProductDraft) -> Unit,
  onSelectRecipient: (GiftRecipient) -> Unit,
  onStartOver: () -> Unit,
  onSave: () -> Unit,
) {
  var showAddTagDialog by rememberSaveable { mutableStateOf(false) }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(18.dp),
    contentPadding = PaddingValues(bottom = 32.dp),
  ) {
    item(key = "review_header", contentType = "header") {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.review_title),
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = stringResource(R.string.review_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item(key = "review_artwork", contentType = "artwork") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val galleryImages = remember(draft.imageKey, draft.additionalImages) {
          (listOf(draft.imageKey) + draft.additionalImages).distinct().filter { it.isNotBlank() }
        }
        var selectedImageIndex by remember { mutableStateOf(0) }
        val currentImage = galleryImages.getOrElse(selectedImageIndex) { draft.imageKey }

        WishProductArtwork(
          imageKey = currentImage,
          productTitle = draft.title,
          shape = WishDropDesign.radii.large,
          modifier = Modifier.fillMaxWidth().height(210.dp),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (galleryImages.size > 1) {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f, fill = false),
            ) {
              itemsIndexed(galleryImages) { idx, imgKey ->
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(WishDropDesign.radii.small)
                    .border(
                      width = if (idx == selectedImageIndex) 2.dp else 0.dp,
                      color = if (idx == selectedImageIndex) MaterialTheme.colorScheme.primary else Color.Transparent,
                      shape = WishDropDesign.radii.small,
                    )
                    .clickable {
                      selectedImageIndex = idx
                      onUpdateDraft { it.copy(imageKey = imgKey) }
                    }
                ) {
                  WishProductArtwork(
                    imageKey = imgKey,
                    productTitle = draft.title,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          } else {
            Spacer(modifier = Modifier.width(4.dp))
          }

          val photoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
          ) { uri ->
            if (uri != null) {
              val customUrl = uri.toString()
              onUpdateDraft { current ->
                current.copy(
                  imageKey = customUrl,
                  additionalImages = (listOf(customUrl) + current.additionalImages).distinct(),
                )
              }
            }
          }

          OutlinedButton(
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = WishDropIcons.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Fotó feltöltése")
          }
        }
      }
    }

    item(key = "review_fields", contentType = "fields") {
      PixelBentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = draft.title,
            onValueChange = { value -> onUpdateDraft { it.copy(title = value) } },
            label = { Text(stringResource(R.string.manual_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("review_title_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
              value = draft.store,
              onValueChange = { value -> onUpdateDraft { it.copy(store = value) } },
              label = { Text(stringResource(R.string.manual_store_label)) },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = draft.priceHuf.toString(),
              onValueChange = { value ->
                val digits = value.filter(Char::isDigit).toIntOrNull() ?: 0
                onUpdateDraft { it.copy(priceHuf = digits) }
              },
              label = { Text(stringResource(R.string.manual_price_label)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    item(key = "review_taxonomy", contentType = "taxonomy") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExpressiveSectionHeader(title = stringResource(R.string.review_categories))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          draft.categories.forEachIndexed { idx, cat ->
            ExpressiveChip(label = cat, colorIndex = idx)
          }
        }

        ExpressiveSectionHeader(
          title = stringResource(R.string.review_tags),
          actionLabel = stringResource(R.string.review_add_tag),
          onActionClick = { showAddTagDialog = true },
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          draft.tags.forEachIndexed { idx, tag ->
            ExpressiveChip(
              label = "#$tag",
              colorIndex = idx + 1,
              onClick = {
                onUpdateDraft { d -> d.copy(tags = d.tags.filterNot { it == tag }) }
              },
            )
          }
        }
      }
    }

    item(key = "review_recipient", contentType = "recipient") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExpressiveSectionHeader(title = stringResource(R.string.review_recipient))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          recipients.forEachIndexed { idx, person ->
            val selected = draft.recipientPrediction?.recipientId == person.id
            ExpressiveChip(
              label =
                if (selected) "${person.name} (${draft.recipientPrediction?.confidencePercent ?: 95}%)"
                else person.name,
              selected = selected,
              colorIndex = idx,
              onClick = { onSelectRecipient(person) },
            )
          }
        }
      }
    }

    if (draft.alternatives.isNotEmpty()) {
      item(key = "review_alternatives", contentType = "alternatives") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ExpressiveSectionHeader(title = stringResource(R.string.review_alternatives))
          draft.alternatives.forEach { alt ->
            PixelBentoCard(
              shape = WishDropDesign.radii.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = alt.title, style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = "${alt.store} • ${alt.highlight}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = CurrencyFormatter.formatHuf(alt.priceHuf),
                  style = WishDropDesign.numericTypography.priceMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }

    item(key = "review_actions", contentType = "actions") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onStartOver,
          modifier = Modifier.weight(1f).height(54.dp).minimumInteractiveComponentSize(),
        ) {
          Text(stringResource(R.string.review_start_over))
        }
        Button(
          onClick = onSave,
          modifier =
            Modifier.weight(1.5f)
              .height(54.dp)
              .minimumInteractiveComponentSize()
              .testTag("review_save_button"),
        ) {
          Icon(imageVector = WishDropIcons.Check, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.review_save_button))
        }
      }
    }
  }

  if (showAddTagDialog) {
    var newTag by rememberSaveable { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddTagDialog = false },
      title = { Text(stringResource(R.string.dialog_add_tag_title)) },
      text = {
        OutlinedTextField(
          value = newTag,
          onValueChange = { newTag = it },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            val clean = newTag.trim().removePrefix("#")
            if (clean.isNotEmpty()) {
              onUpdateDraft { d -> d.copy(tags = (d.tags + clean).distinct()) }
            }
            showAddTagDialog = false
          },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(stringResource(R.string.action_add))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showAddTagDialog = false },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Preview(showBackground = true, name = "Add Product — Light")
@Composable
private fun AddProductLightPreview() {
  WishDropTheme(themeMode = ThemeMode.LIGHT, useDynamicColor = false) {
    AddProductScreenContent(
      state = AddProductUiState(),
      onNavigateBack = {},
      onSelectMode = {},
      onUrlChanged = {},
      onManualChanged = { _, _, _, _ -> },
      onBarcodeChanged = {},
      onAnalyze = {},
      onUpdateDraft = {},
      onSelectRecipient = {},
      onResetToInput = {},
      onSaveDraft = {},
    )
  }
}

@Preview(
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Add Product — Dark",
)
@Composable
private fun AddProductDarkPreview() {
  WishDropTheme(themeMode = ThemeMode.DARK, useDynamicColor = false) {
    AddProductScreenContent(
      state = AddProductUiState(mode = AddInputMode.BARCODE),
      onNavigateBack = {},
      onSelectMode = {},
      onUrlChanged = {},
      onManualChanged = { _, _, _, _ -> },
      onBarcodeChanged = {},
      onAnalyze = {},
      onUpdateDraft = {},
      onSelectRecipient = {},
      onResetToInput = {},
      onSaveDraft = {},
    )
  }
}
