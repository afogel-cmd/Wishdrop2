package com.example.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.format.EventDateUtils
import com.example.designsystem.components.PersonAvatarBadge
import com.example.designsystem.icon.WishDropIcons
import com.example.domain.model.FamilyMemberEvent
import com.example.domain.model.GiftRecipient
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyMemberProfileDialog(
  member: GiftRecipient?,
  isNewMember: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    budgetHuf: Int,
    interests: List<String>,
    avatarUrl: String?,
    events: List<FamilyMemberEvent>,
  ) -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }

  var name by rememberSaveable { mutableStateOf(member?.name ?: "") }
  var relationship by rememberSaveable { mutableStateOf(member?.relationship ?: "Partner") }
  var avatarUrl by rememberSaveable { mutableStateOf(member?.avatarUrl) }
  var budgetText by rememberSaveable { mutableStateOf((member?.budgetHuf ?: 35_000).toString()) }
  val accentKey = member?.accentKey ?: "coral"

  var selectedInterests by rememberSaveable {
    mutableStateOf(member?.interests ?: listOf("Könyvek", "Kávé"))
  }
  var customInterestInput by rememberSaveable { mutableStateOf("") }

  // Events list state
  var eventsList by remember {
    mutableStateOf(
      member?.events?.ifEmpty {
        val evtName = member.upcomingEventName.ifBlank { "Születésnap" }
        listOf(
          FamilyMemberEvent(
            id = "evt_init_1",
            title = evtName,
            dateMillis = System.currentTimeMillis() + member.daysUntilEvent.toLong() * 86_400_000L,
            eventType = EventDateUtils.determineEventType(evtName),
            isRecurringYearly = true,
          )
        )
      } ?: listOf(
        FamilyMemberEvent(
          id = "evt_init_new",
          title = "Születésnap",
          dateMillis = System.currentTimeMillis() + 30L * 86_400_000L,
          eventType = "Születésnap",
          isRecurringYearly = true,
        )
      )
    )
  }

  val context = androidx.compose.ui.platform.LocalContext.current
  var editingEvent by remember { mutableStateOf<FamilyMemberEvent?>(null) }
  var showAddEventDialog by rememberSaveable { mutableStateOf(false) }
  var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

  val photoPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      runCatching {
        val avatarDir = java.io.File(context.filesDir, "avatars").apply { mkdirs() }
        val avatarFile = java.io.File(avatarDir, "member_avatar_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
          avatarFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
        avatarUrl = Uri.fromFile(avatarFile).toString()
      }.onFailure {
        avatarUrl = uri.toString()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // --- HEADER ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(modifier = Modifier.clickable {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }) {
              PersonAvatarBadge(
                name = name.ifBlank { "Új" },
                accentKey = accentKey,
                imageUrl = avatarUrl,
                size = 52.dp,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isNewMember) "Új családtag profilja" else "${name.ifBlank { "Családtag" }} profilja",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "$relationship • ${eventsList.size} esemény rögzítve",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (!isNewMember && onDelete != null) {
            IconButton(
              onClick = { showDeleteConfirmDialog = true },
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Családtag törlése",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }

        // --- TABS ---
        TabRow(
          selectedTabIndex = selectedTab,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Események", maxLines = 1) },
            icon = { Icon(WishDropIcons.Cake, contentDescription = null, modifier = Modifier.size(18.dp)) },
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Profil & Hobbik", maxLines = 1) },
            icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
          )
        }

        // --- TAB CONTENTS ---
        Box(
          modifier = Modifier
            .weight(1f, fill = false)
            .heightIn(min = 280.dp, max = 400.dp),
        ) {
          if (selectedTab == 0) {
            // EVENTS & ANNIVERSARIES TAB
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Közelgő események",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                  onClick = { showAddEventDialog = true },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Új esemény",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }

              if (eventsList.isEmpty()) {
                OutlinedCard(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                  ) {
                    Text(
                      text = "Nincs még rögzített esemény.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddEventDialog = true }) {
                      Text("Esemény hozzáadása")
                    }
                  }
                }
              } else {
                val sortedEvents = eventsList.sortedBy {
                  EventDateUtils.calculateDaysUntilRecurringEvent(it.dateMillis)
                }

                sortedEvents.forEach { event ->
                  val daysRemaining = EventDateUtils.calculateDaysUntilRecurringEvent(event.dateMillis)
                  val formattedDate = EventDateUtils.formatEventDate(event.dateMillis)
                  val isUrgent = daysRemaining <= 14

                  OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                      containerColor = if (isUrgent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                      ) {
                        val iconVector = when (event.eventType) {
                          "Születésnap" -> WishDropIcons.Cake
                          "Évforduló" -> Icons.Default.Favorite
                          "Karácsony" -> WishDropIcons.Holiday
                          else -> WishDropIcons.GiftFilled
                        }
                        Box(
                          modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                              if (isUrgent) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.secondaryContainer
                            ),
                          contentAlignment = Alignment.Center,
                        ) {
                          Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (isUrgent) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                          )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            Text(
                              text = event.title,
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            if (event.isRecurringYearly) {
                              Text(
                                text = "• Évente",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                              )
                            }
                          }

                          Text(
                            text = "$formattedDate (${EventDateUtils.formatDaysRemaining(daysRemaining)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUrgent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                          )

                          if (event.note.isNotBlank()) {
                            Text(
                              text = event.note,
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                        }
                      }

                      Row {
                        IconButton(
                          onClick = { editingEvent = event },
                          modifier = Modifier.size(32.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Szerkesztés",
                            modifier = Modifier.size(18.dp),
                          )
                        }
                        IconButton(
                          onClick = {
                            eventsList = eventsList.filterNot { it.id == event.id }
                          },
                          modifier = Modifier.size(32.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Törlés",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            // PROFILE & INTERESTS TAB
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Név *") },
                placeholder = { Text("pl. Dalma, Ármin, Anya...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )

              // Relationship
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kapcsolat", style = MaterialTheme.typography.labelMedium)
                val relations = listOf("Partner", "Gyerek", "Szülő", "Testvér", "Barát", "Családtag")
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  relations.forEach { rel ->
                    FilterChip(
                      selected = relationship.equals(rel, ignoreCase = true),
                      onClick = { relationship = rel },
                      label = { Text(rel) },
                    )
                  }
                }
              }

              // Budget
              OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Tervezett ajándék büdzsé (Ft)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )

              // Interests & Hobbies
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  "Érdeklődési körök & Hobbik (AI ajánláshoz)",
                  style = MaterialTheme.typography.labelMedium,
                )

                // Currently active / added interests
                if (selectedInterests.isNotEmpty()) {
                  Text(
                    "Kiválasztott érdeklődési körök:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    selectedInterests.forEach { interest ->
                      FilterChip(
                        selected = true,
                        onClick = {
                          selectedInterests = selectedInterests.filterNot { it.equals(interest, ignoreCase = true) }
                        },
                        label = { Text(interest) },
                        trailingIcon = {
                          Icon(
                            imageVector = WishDropIcons.Clear,
                            contentDescription = "Törlés",
                            modifier = Modifier.size(14.dp),
                          )
                        },
                      )
                    }
                  }
                }

                // Suggestions from popular gift interests
                val availablePopular = EventDateUtils.POPULAR_GIFT_INTERESTS.filter { tag ->
                  selectedInterests.none { it.equals(tag, ignoreCase = true) }
                }
                if (availablePopular.isNotEmpty()) {
                  Text(
                    "Javasolt hobbik & témák:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    availablePopular.take(8).forEach { tag ->
                      FilterChip(
                        selected = false,
                        onClick = {
                          selectedInterests = selectedInterests + tag
                        },
                        label = { Text("+ $tag") },
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedTextField(
                    value = customInterestInput,
                    onValueChange = { customInterestInput = it },
                    placeholder = { Text("Egyéni érdeklődési kör...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                  )
                  FilledTonalButton(
                    onClick = {
                      val clean = customInterestInput.trim()
                      if (clean.isNotBlank() && selectedInterests.none { it.equals(clean, ignoreCase = true) }) {
                        selectedInterests = selectedInterests + clean
                        customInterestInput = ""
                      }
                    },
                    enabled = customInterestInput.isNotBlank(),
                  ) {
                    Icon(
                      imageVector = WishDropIcons.Add,
                      contentDescription = "Hozzáadás",
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hozzáad")
                  }
                }
              }
            }
          }
        }

        // --- FOOTER BUTTONS ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_cancel))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank()) {
                val primaryEvent = eventsList.minByOrNull {
                  EventDateUtils.calculateDaysUntilRecurringEvent(it.dateMillis)
                }
                val finalEventName = primaryEvent?.title ?: "Születésnap"
                val finalDays = primaryEvent?.let {
                  EventDateUtils.calculateDaysUntilRecurringEvent(it.dateMillis)
                } ?: 30
                val parsedBudget = budgetText.toIntOrNull() ?: 35_000

                onSave(
                  name.trim(),
                  relationship.trim().ifBlank { "Partner" },
                  finalEventName,
                  finalDays,
                  parsedBudget,
                  selectedInterests,
                  avatarUrl,
                  eventsList,
                )
              }
            },
            enabled = name.isNotBlank(),
          ) {
            Text(stringResource(R.string.action_save))
          }
        }
      }
    }
  }

  // --- SUB-DIALOG: ADD OR EDIT EVENT ---
  if (showAddEventDialog || editingEvent != null) {
    val isEdit = editingEvent != null
    val targetEvent = editingEvent

    EventEditDialog(
      initialTitle = targetEvent?.title ?: "Születésnap",
      initialDateMillis = targetEvent?.dateMillis ?: (System.currentTimeMillis() + 30L * 86_400_000L),
      initialEventType = targetEvent?.eventType ?: "Születésnap",
      initialIsRecurring = targetEvent?.isRecurringYearly ?: true,
      initialNote = targetEvent?.note ?: "",
      dialogTitle = if (isEdit) "Esemény szerkesztése" else "Új esemény hozzáadása",
      onDismiss = {
        showAddEventDialog = false
        editingEvent = null
      },
      onSave = { title, dateMillis, type, isRecurring, note ->
        if (isEdit && targetEvent != null) {
          eventsList = eventsList.map {
            if (it.id == targetEvent.id) {
              it.copy(
                title = title,
                dateMillis = dateMillis,
                eventType = type,
                isRecurringYearly = isRecurring,
                note = note,
              )
            } else it
          }
        } else {
          eventsList = eventsList + FamilyMemberEvent(
            id = "evt_${UUID.randomUUID().toString().take(6)}",
            title = title,
            dateMillis = dateMillis,
            eventType = type,
            isRecurringYearly = isRecurring,
            note = note,
          )
        }
        showAddEventDialog = false
        editingEvent = null
      },
    )
  }

  // --- SUB-DIALOG: DELETE CONFIRMATION ---
  if (showDeleteConfirmDialog && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Családtag profil törlése") },
      text = { Text("Biztosan törölni szeretnéd ${name} teljes profilját, évfordulóit és ajándékjavaslatait?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirmDialog = false
            onDelete()
          }
        ) {
          Text("Törlés", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = false }) {
          Text("Mégse")
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EventEditDialog(
  initialTitle: String,
  initialDateMillis: Long,
  initialEventType: String,
  initialIsRecurring: Boolean,
  initialNote: String,
  dialogTitle: String,
  onDismiss: () -> Unit,
  onSave: (title: String, dateMillis: Long, type: String, isRecurring: Boolean, note: String) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf(initialTitle) }
  var eventType by rememberSaveable { mutableStateOf(initialEventType) }
  var selectedDateMillis by rememberSaveable { mutableStateOf(initialDateMillis) }
  var isRecurring by rememberSaveable { mutableStateOf(initialIsRecurring) }
  var note by rememberSaveable { mutableStateOf(initialNote) }
  var showDatePicker by rememberSaveable { mutableStateOf(false) }

  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

  val eventTypes = listOf("Születésnap", "Évforduló", "Karácsony", "Névnap", "Egyéb")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(dialogTitle, style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Event Type selection
        Text("Esemény típusa", style = MaterialTheme.typography.labelMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          eventTypes.forEach { type ->
            FilterChip(
              selected = eventType.equals(type, ignoreCase = true),
              onClick = {
                eventType = type
                if (title.isBlank() || eventTypes.contains(title)) {
                  title = type
                }
              },
              label = { Text(type) },
            )
          }
        }

        // Title
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Esemény / Évforduló megnevezése *") },
          placeholder = { Text("pl. 30. Születésnap, Házassági évforduló") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Date Picker Trigger
        val daysUntil = EventDateUtils.calculateDaysUntilRecurringEvent(selectedDateMillis)
        val formattedDate = EventDateUtils.formatEventDate(selectedDateMillis)

        OutlinedCard(
          onClick = { showDatePicker = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Dátum kiválasztása",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = EventDateUtils.formatDaysRemaining(daysUntil),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              imageVector = WishDropIcons.Cake,
              contentDescription = "Dátum választása",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }

        // Recurring switch
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isRecurring = !isRecurring }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Évenként ismétlődő esemény", style = MaterialTheme.typography.bodyMedium)
            Text(
              "Minden évben automatikusan újraszámolja a hátralévő napokat",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = isRecurring,
            onCheckedChange = { isRecurring = it },
          )
        }

        // Note
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Megjegyzés / Ajándékötlet emlékeztető") },
          placeholder = { Text("pl. virág, wellness utazás, meglepetés buli") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onSave(title.trim(), selectedDateMillis, eventType, isRecurring, note.trim())
          }
        },
        enabled = title.isNotBlank(),
      ) {
        Text("Mentés")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Mégse")
      }
    }
  )

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let {
              selectedDateMillis = it
            }
            showDatePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Mégse")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
