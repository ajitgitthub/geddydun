@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.personal.remindme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.abs
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.runtime.key
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JTextStyle

// ---------- navigation ----------

sealed interface Screen {
    data object Today : Screen
    data object Lists : Screen
    data object Settings : Screen
    /** shareTitle/shareNotes: text shared in from another app (Share → Geddydun). */
    data class Edit(val id: Long?, val sendBack: Boolean = false, val shareTitle: String? = null, val shareNotes: String = "") : Screen
    data class Changes(val taskId: String) : Screen
    data object History : Screen
    data class View(val id: Long) : Screen
    data class Paid(val id: Long) : Screen
    data object Digest : Screen
    data object Name : Screen
    data object Permissions : Screen
    data object AddPerson : Screen
    data object Search : Screen
    data object Feedback : Screen
    data object Restore : Screen
    data object Notes : Screen
    data class NoteView(val id: String) : Screen
    data class NoteEdit(val id: String?) : Screen
    /** Review an errand someone asked you to do (taskId), or change your reminder for one you accepted (localId). */
    data class Review(val taskId: String? = null, val localId: Long? = null) : Screen
}

val Screen.isTab: Boolean get() = this == Screen.Today || this == Screen.Lists || this == Screen.Notes || this == Screen.Settings

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)
    private val screen = mutableStateOf<Screen>(Screen.Today)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Scheduler.rescheduleAll(this)
        if (Store.userName.isBlank()) {
            screen.value = Screen.Name
        } else if (!Store.setupShown) {
            Store.setupShown = true
            screen.value = Screen.Permissions
        }
        if (savedInstanceState == null) handleShare(intent)
        handleIntent(intent)
        setContent { AppTheme { AppRoot(screen, resumeTick.intValue) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
        handleIntent(intent)
    }

    /** Text shared from WhatsApp, Chrome etc. opens the add screen with it filled in. */
    private fun handleShare(i: Intent?) {
        if (i?.action != Intent.ACTION_SEND || Store.userName.isBlank()) return
        val text = i.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subject = i.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        if (text.isBlank() && subject.isBlank()) return
        val (title, notes) = shareToTitle(subject, text)
        screen.value = Screen.Edit(null, shareTitle = title, shareNotes = notes)
    }

    private fun handleIntent(i: Intent?) {
        if (i == null || Store.userName.isBlank()) return
        if (i.getBooleanExtra("digest", false)) screen.value = Screen.Digest
        val open = i.getStringExtra("open") ?: return
        i.removeExtra("open") // don't jump again when the screen rotates
        screen.value = when {
            open.startsWith("review:") -> Screen.Review(taskId = open.removePrefix("review:"))
            open == "new" -> Screen.Edit(null)
            open.startsWith("changes:") -> Screen.Changes(open.removePrefix("changes:"))
            open.startsWith("view:") -> {
                val id = open.removePrefix("view:").toLongOrNull()
                if (id != null && Store.get(id) != null) Screen.View(id) else Screen.Today
            }
            open.startsWith("task:") -> {
                val id = open.removePrefix("task:")
                Store.reminders.value.find { it.taskId == id }?.let { Screen.View(it.id) } ?: Screen.Today
            }
            else -> Screen.Today
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        Updater.check(this)
        TodayWidget.refresh(this)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalShared = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnim = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** A card that grows into its full screen (and shrinks back) when opened. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCard(key: Any): Modifier {
    val shared = LocalShared.current ?: return this
    val anim = LocalAnim.current ?: return this
    val base = this
    return with(shared) {
        base.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = anim,
            enter = fadeIn(tween(260, delayMillis = 60)),
            exit = fadeOut(tween(160)),
            boundsTransform = { _, _ -> tween(420, easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)) },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(18.dp)),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppRoot(state: MutableState<Screen>, tick: Int) {
    val ctx = LocalContext.current
    var screen by state
    var lastTab by remember { mutableStateOf<Screen>(Screen.Today) }
    val current = screen
    SideEffect { if (current.isTab) lastTab = current }
    // How many reminders there were when something was shared in, to tell if it got saved.
    val shareStart = remember(current) { if (current is Screen.Edit && current.shareTitle != null) Store.reminders.value.size else -1 }
    val back: () -> Unit = {
        val act = ctx as? android.app.Activity
        if (current == Screen.Restore && Store.userName.isBlank()) {
            screen = Screen.Name
        } else if (current is Screen.Edit && current.shareTitle != null && act != null) {
            // Shared in from another app: saved or cancelled, go straight back there.
            if (Store.reminders.value.size > shareStart) {
                Toast.makeText(ctx.applicationContext, "Added to Geddydun", Toast.LENGTH_SHORT).show()
            }
            act.finish()
        } else {
            screen = if (screen.isTab) Screen.Today else lastTab
        }
    }
    BackHandler(enabled = current != Screen.Today && current != Screen.Name) { back() }

    Box(Modifier.fillMaxSize().glow()) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    val order = listOf(Screen.Today, Screen.Lists, Screen.Notes, Screen.Settings)
                    val from = order.indexOf(initialState)
                    val to = order.indexOf(targetState)
                    if (targetState is Screen.View || initialState is Screen.View) {
                        fadeIn(tween(260, delayMillis = 60)) togetherWith fadeOut(tween(200))
                    } else if (from >= 0 && to >= 0 && from != to) {
                        // Swipe in from the side of the tab you tapped.
                        val dir = if (to > from) 1 else -1
                        (slideInHorizontally(tween(380, easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f))) { it * dir }) togetherWith
                            (slideOutHorizontally(tween(320)) { -it * dir / 3 } + fadeOut(tween(320)))
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "screens",
            ) { scr ->
                CompositionLocalProvider(LocalShared provides this@SharedTransitionLayout, LocalAnim provides this) {
                    Box(Modifier.fillMaxSize()) {
                    when (scr) {
                        Screen.Today -> TodayScreen(tick) { screen = it }
                        Screen.Lists -> ListsScreen(tick) { screen = it }
                        Screen.Notes -> NotesScreen { screen = it }
                        is Screen.NoteView -> NoteViewScreen(scr.id, onEdit = { screen = Screen.NoteEdit(scr.id) }, onClose = back)
                        is Screen.NoteEdit -> NoteEditScreen(scr.id, onClose = {
                            screen = if (scr.id != null && com.personal.remindme.Notes.get(scr.id) != null) Screen.NoteView(scr.id) else Screen.Notes
                        }) { screen = Screen.NoteView(it) }
                        Screen.Settings -> SettingsScreen(tick) { screen = it }
                        is Screen.View -> ViewScreen(
                            scr.id, onEdit = { screen = Screen.Edit(scr.id) }, onClose = back,
                            onAddPerson = { screen = Screen.AddPerson },
                        )
                        is Screen.Edit -> {
                            val e = scr.id?.let { Store.get(it) }
                            val p = e?.taskId?.let { Cloud.proposals.value[it] }
                            when {
                                scr.shareTitle != null ->
                                    EditScreen(null, shareTitle = scr.shareTitle, shareNotes = scr.shareNotes, onClose = back)
                                e != null && e.assignedToMe && e.taskStatus != "accepted" -> ReviewScreen(null, scr.id, back)
                                e != null && p != null && p.byUid != Cloud.uid && !scr.sendBack ->
                                    ChangesScreen(e.taskId!!, back) { screen = it }
                                else -> EditScreen(scr.id, scr.sendBack, onClose = back)
                            }
                        }
                        is Screen.Changes -> ChangesScreen(scr.taskId, back) { screen = it }
                        Screen.History -> HistoryScreen(back)
                        is Screen.Review -> ReviewRoute(scr.taskId, scr.localId, back) { screen = it }
                        Screen.Search -> SearchScreen(back) { screen = it }
                        is Screen.Paid -> PaidScreen(scr.id, back)
                        Screen.Digest -> DigestScreen(back)
                        Screen.Permissions -> PermissionsScreen(tick, back)
                        Screen.AddPerson -> AddPersonScreen(back)
                        Screen.Feedback -> FeedbackScreen(back)
                        Screen.Restore -> RestoreScreen(back) {
                            // Fresh phone: walk through permissions next, then Today.
                            screen = if (!Store.setupShown) {
                                Store.setupShown = true; Screen.Permissions
                            } else Screen.Today
                        }
                        Screen.Name -> NameScreen(onRestore = { screen = Screen.Restore }) {
                            screen = if (!Store.setupShown) {
                                Store.setupShown = true; Screen.Permissions
                            } else Screen.Today
                        }
                    }
                    }
                }
            }
        }
        if (current.isTab) Dock(current, Modifier.align(Alignment.BottomCenter)) { screen = it }
    }
    val feedbackOpen by FeedbackBox.sheet.collectAsState()
    if (feedbackOpen) FeedbackSheet(
        current.label(),
        onSeeAll = { FeedbackBox.sheet.value = false; screen = Screen.Feedback },
        onDismiss = { FeedbackBox.sheet.value = false },
    )
    UpdateDialog()
}

// ---------- updates ----------

@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    val rel by Updater.available.collectAsState()
    val st by Updater.state.collectAsState()
    var later by rememberSaveable { mutableIntStateOf(0) }
    val r = rel ?: return
    if (later == r.code) return
    AlertDialog(
        onDismissRequest = { if (st !is Updater.State.Downloading) later = r.code },
        containerColor = SheetBg,
        title = {
            Column {
                Text("Update available", color = Muted, fontSize = 13.sp)
                BigTitle("Geddydun ${r.name}", 24)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text("WHAT'S NEW", fontSize = 11.sp, letterSpacing = 0.8.sp, color = Faint, fontFamily = Mono)
                Spacer(Modifier.height(8.dp))
                val lines = r.notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) Text("Fixes and improvements.", color = TextC, fontSize = 14.sp)
                lines.forEach { line ->
                    val bullet = line.startsWith("-") || line.startsWith("*") || line.startsWith("•")
                    val head = line.startsWith("#")
                    val text = line.trimStart('-', '*', '•', '#', ' ')
                    Row(Modifier.padding(vertical = 3.dp)) {
                        if (bullet) Text("•", color = Blue, fontSize = 14.sp, modifier = Modifier.width(14.dp))
                        Text(
                            text, color = if (head) Muted else TextC, fontSize = if (head) 12.sp else 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
                Text(
                    "Your reminders stay as they are. Android asks you to tap Install.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp),
                )
                when (val x = st) {
                    is Updater.State.Downloading -> {
                        Text("Downloading… ${x.percent}%", color = BlueText, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                        ProgressBar(x.percent, 100)
                    }
                    is Updater.State.Failed -> Text(x.message, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    else -> {}
                }
            }
        },
        confirmButton = {
            val downloading = st is Updater.State.Downloading
            Text(
                if (downloading) "Downloading…" else "Update", color = if (downloading) BlueText else OnAccent, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (downloading) BlueBg else Blue)
                    .clickable(enabled = !downloading) { Updater.install(ctx, r) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        },
        dismissButton = {
            if (st !is Updater.State.Downloading) TextButton(onClick = { later = r.code }) { Text("Later", color = Muted) }
        },
    )
}

@Composable
fun Dock(current: Screen, modifier: Modifier, go: (Screen) -> Unit) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val tabs = listOf(
            Triple(Screen.Today, TimelineIcon, "Today"),
            Triple(Screen.Lists, ChecklistIcon, "Lists"),
            Triple(Screen.Notes, NotesIcon, "Notes"),
            Triple(Screen.Settings, Icons.Outlined.Settings, "Settings"),
        )
        val index = tabs.indexOfFirst { it.first == current }.coerceAtLeast(0)
        var rowWidth by remember { mutableIntStateOf(0) }
        // The highlight slides to the tapped tab, with a little bounce.
        val slide by animateFloatAsState(
            index.toFloat(), spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow), label = "dock",
        )
        val density = LocalDensity.current
        Box(
            Modifier.weight(1f).clip(RoundedCornerShape(30.dp)).background(DockBg).padding(5.dp)
                .onSizeChanged { rowWidth = it.width },
        ) {
            val itemPx = rowWidth / tabs.size
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier.offset { IntOffset((slide * itemPx).roundToInt(), 0) }
                        .width(with(density) { itemPx.toDp() }).fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp)).background(Color(0xFF2A2A2E)),
                )
            }
            Row {
                tabs.forEach { (s, icon, label) ->
                    val on = current == s
                    val tint by animateColorAsState(if (on) TextC else Muted, tween(250), label = "tint")
                    val grow by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "grow")
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                            .clickable { go(s) }.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp).scale(grow))
                        Text(label, fontSize = 11.sp, color = tint)
                    }
                }
            }
        }
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(DockBg)
                .border(1.dp, Color(0xFF2E2E33), CircleShape)
                .clickable { go(if (current == Screen.Notes) Screen.NoteEdit(null) else Screen.Edit(null)) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Add, if (current == Screen.Notes) "Add note" else "Add reminder", tint = TextC) }
    }
}

// ---------- shared bits ----------

@Composable
fun TopRow(
    icon: ImageVector, desc: String, onClick: () -> Unit,
    feedback: Boolean = true, actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClick) { Icon(icon, desc, tint = Color(0xFFB8B8BE)) }
        Spacer(Modifier.weight(1f))
        actions()
        if (feedback) FeedbackButton(modifier = Modifier.padding(start = 6.dp, end = 4.dp))
    }
}

@Composable
fun TaskCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outline: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .then(if (outline) Modifier.border(1.dp, Line, shape) else Modifier.background(CardBg))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun SmallIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Faint, modifier = Modifier.size(18.dp)) }
}

@Composable
fun ProgressBar(done: Int, total: Int) {
    Box(
        Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp)
            .clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A2A2E)),
    ) {
        Box(
            Modifier.fillMaxWidth(if (total == 0) 0f else done / total.toFloat())
                .fillMaxHeight().background(Blue),
        )
    }
}

/** Unticked items as small boxes drifting right to left. */
@Composable
fun ItemTicker(items: List<Item>, modifier: Modifier = Modifier) {
    val pending = items.filter { !it.done }
    if (pending.isEmpty()) return
    Row(
        modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 0, initialDelayMillis = 600, velocity = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        pending.forEach {
            Text(
                it.name, fontSize = 12.sp, color = TextC, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A2E))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(18.dp))
    }
}

@Composable
fun PinCard(r: Reminder, onClick: () -> Unit) {
    val left = r.items.count { !it.done }
    Column(
        Modifier.width(196.dp).clip(RoundedCornerShape(18.dp)).background(CardBg)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.title, fontSize = 15.sp, color = TextC, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(PinIcon, null, tint = Blue, modifier = Modifier.size(16.dp))
        }
        when {
            r.items.isEmpty() -> Text("No items yet · tap to add", color = Muted, fontSize = 12.sp)
            left == 0 -> {
                Text("All got", color = Green, fontSize = 12.sp)
                Text("Tap to add more", color = Muted, fontSize = 12.sp)
            }
            else -> {
                Text("$left left", color = Muted, fontSize = 12.sp)
                ItemTicker(r.items)
            }
        }
    }
}

@Composable
fun FieldButton(text: String, modifier: Modifier = Modifier, mono: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(54.dp).clip(FieldShape).background(CardBg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, fontSize = 15.sp, color = TextC, fontFamily = if (mono) Mono else FontFamily.Default) }
}

@Composable
fun DayCircles(isOn: (Int) -> Boolean, onTap: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, l ->
            val d = i + 1
            val on = isOn(d)
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (on) BlueBg else CardBg).clickable { onTap(d) },
                contentAlignment = Alignment.Center,
            ) { Text(l, color = if (on) BlueText else TextC, fontSize = 13.sp) }
        }
    }
}

@Composable
fun NumberRow(before: String, value: String, after: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(before, color = TextC, fontSize = 14.sp)
        OutlinedTextField(
            value = value, onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true, shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
        )
        Text(after, color = TextC, fontSize = 14.sp)
    }
}

@Composable
fun SetRow(
    title: String,
    sub: String? = null,
    value: String? = null,
    valueColor: Color = Muted,
    chevron: Boolean = false,
    titleColor: Color = TextC,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = titleColor)
            if (sub != null) Text(sub, color = Muted, fontSize = 12.sp)
        }
        if (value != null) Text(value, color = valueColor, fontSize = 13.sp, fontFamily = Mono)
        trailing?.invoke()
        if (chevron) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Faint)
    }
    HLine()
}

private val upperShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
fun fmtUpperDate(ms: Long): String = toLdt(ms).format(upperShort).uppercase(Locale.ENGLISH)

fun whenLine(r: Reminder): String {
    if (r.type == Type.CHECK) return Digest.describe(r)
    if (r.completed) return "Done · " + repeatShort(r)
    return fmtWhen(r.nextTrigger) + " · " + repeatShort(r)
}

// ---------- first open ----------

@Composable
fun NameScreen(onRestore: () -> Unit, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    fun save() {
        if (name.isNotBlank()) {
            Store.userName = name.trim(); onDone()
        }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
    ) {
        Spacer(Modifier.height(80.dp))
        Text("Welcome to", color = Muted, fontSize = 16.sp)
        BigTitle("Geddydun", 42)
        Spacer(Modifier.height(44.dp))
        Text("What should I call you?", fontSize = 20.sp, color = TextC)
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(30) },
            placeholder = { Text("Your name") }, singleLine = true,
            shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
        Text("You can change it later in Settings.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.weight(1f))
        BigButton("Continue", if (name.isBlank()) CardBg else Blue, if (name.isBlank()) Muted else OnAccent) { save() }
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Line))
            Text("or", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(Line))
        }
        BigButton("Restore from backup", CardBg, BlueText) { onRestore() }
        Text(
            "Had Geddydun before? Bring everything back.", color = Muted, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

// ---------- today ----------

/** Today's picked date and scroll positions survive opening a card or switching tabs. */
object TodayState {
    val day = mutableStateOf(LocalDate.now())
    var openedOn: LocalDate = LocalDate.now()
    val strip = LazyListState(firstVisibleItemIndex = 5)
    val list = LazyListState()
}

@Composable
fun TodayScreen(tick: Int, open: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val all by Store.reminders.collectAsState()
    val today = remember(tick) { LocalDate.now() }
    var day by TodayState.day
    val scope = rememberCoroutineScope()
    LaunchedEffect(today) {
        // A new day: start from today again.
        if (TodayState.openedOn != today) {
            TodayState.openedOn = today
            day = today
            TodayState.strip.scrollToItem(5)
            TodayState.list.scrollToItem(0)
        }
    }
    val now = remember(tick, all) { System.currentTimeMillis() }
    val permsOk = remember(tick) { Perms.allOk(ctx) }
    val days = remember(today) { (-7..60).map { today.plusDays(it.toLong()) } }
    val busy = remember(all, today) {
        days.filter { d -> Timeline.slots(all, d, now).any { it.paid == null } }.toSet()
    }
    val slots = remember(all, day, now) { Timeline.slots(all, day, now) }
    val rows: List<Any> = remember(slots, day, today, now) {
        buildList<Any> {
            var placed = day != today
            for (s in slots) {
                if (!placed && s.at > now) {
                    add("now"); placed = true
                }
                add(s)
            }
            if (!placed && slots.isNotEmpty()) add("now")
        }
    }
    val upcoming = remember(all, now) {
        all.filter {
            it.type != Type.CHECK && !it.completed && !it.isTest && it.dueAt > 0 &&
                toLdt(it.nextTrigger).toLocalDate().isAfter(LocalDate.now())
        }.sortedBy { it.nextTrigger }.take(5)
    }
    val strip = TodayState.strip
    val pinned = all.filter { it.pinned && it.type == Type.ERRAND }.sortedBy { it.id }
    var mutedDay by remember { mutableStateOf(Store.mutedDay) }
    val muted = remember(tick, mutedDay) { Store.mutedToday }
    var muteSheet by remember { mutableStateOf(false) }
    val pendingInvites by Cloud.invites.collectAsState()
    val requests by Cloud.requests.collectAsState()
    val props by Cloud.proposals.collectAsState()
    val incoming = props.values.filter { it.byUid != Cloud.uid }
        .mapNotNull { p -> all.find { it.taskId == p.taskId }?.let { p to it } }

    // Swipe the timeline (below the dates) for the next or previous day.
    var dragX by remember { mutableFloatStateOf(0f) }
    val settleX = remember { Animatable(0f) }
    var stripBottom by remember { mutableFloatStateOf(0f) }
    var listTop by remember { mutableFloatStateOf(0f) }
    val dens = LocalDensity.current
    val widthPx = with(dens) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val minSwipe = with(dens) { 56.dp.toPx() }
    val flingSpeed = with(dens) { 450.dp.toPx() }
    val slide = Modifier.graphicsLayer { translationX = dragX + settleX.value }

    fun settle(dx: Float, vx: Float) = scope.launch {
        val cur = TodayState.day.value
        val forward = if (abs(dx) >= minSwipe) dx < 0 else vx < 0
        val target = cur.plusDays(if (forward) 1 else -1)
        val go = (abs(dx) >= minSwipe || abs(vx) >= flingSpeed) && target in days
        settleX.snapTo(dx)
        dragX = 0f
        if (!go) {
            settleX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
            return@launch
        }
        val dir = if (forward) 1f else -1f
        settleX.animateTo(-dir * widthPx * 0.6f, tween(120, easing = LinearEasing))
        day = target
        settleX.snapTo(dir * widthPx * 0.6f)
        launch { strip.animateScrollToItem((days.indexOf(target) - 3).coerceAtLeast(0)) }
        settleX.animateTo(0f, spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow))
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding()
            .onGloballyPositioned { listTop = it.positionInRoot().y }
            .pointerInput(days) {
                val slop = viewConfiguration.touchSlop * 0.8f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.position.y + listTop <= stripBottom) return@awaitEachGesture
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var total = Offset.Zero
                    var sideways = false
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val delta = ch.positionChange()
                        tracker.addPosition(ch.uptimeMillis, ch.position)
                        if (!sideways) {
                            total += delta
                            // Mostly sideways (even at an angle) = change day; mostly up/down = scroll.
                            if (abs(total.x) > slop && abs(total.x) > abs(total.y) * 0.75f) {
                                sideways = true
                                ch.consume()
                            } else if (abs(total.y) > slop * 1.5f) {
                                return@awaitEachGesture
                            }
                        } else {
                            ch.consume()
                            // A little resistance past the first or last day.
                            val edge = (delta.x < 0 && TodayState.day.value == days.last()) ||
                                (delta.x > 0 && TodayState.day.value == days.first())
                            dragX += if (edge) delta.x * 0.3f else delta.x
                        }
                    }
                    if (sideways) settle(dragX, tracker.calculateVelocity().x)
                }
            },
        state = TodayState.list,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
    ) {
        item {
            Text("What's up, ${Store.userName}", color = Muted, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigTitle(day.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH), 54)
                Text(
                    "%02d".format(day.dayOfMonth), fontSize = 54.sp, lineHeight = 60.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = (-0.5).sp, color = Faint,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                Box(
                    Modifier.padding(end = 8.dp).size(44.dp).clip(CircleShape)
                        .background(CardBg).clickable { open(Screen.Search) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Search, "Search", tint = Color(0xFFC9C9CE), modifier = Modifier.size(22.dp)) }
                FeedbackButton(big = true, modifier = Modifier.padding(end = if (day == today) 8.dp else 0.dp))
                if (day == today) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(if (muted) Color(0xFF3A1F1F) else CardBg)
                            .clickable { muteSheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (muted) BellOffIcon else BellIcon,
                            if (muted) "Alarms off today" else "Turn off today's alarms",
                            tint = if (muted) Red else Color(0xFFC9C9CE), modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            if (day != today) {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val gap = java.time.temporal.ChronoUnit.DAYS.between(today, day)
                    val rel = when {
                        gap == 1L -> "tomorrow"
                        gap == -1L -> "yesterday"
                        gap > 0 -> "in $gap days"
                        else -> "${-gap} days ago"
                    }
                    Text(
                        day.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) + " · " + rel,
                        color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f),
                    )
                    Row(
                        Modifier.clip(RoundedCornerShape(18.dp)).background(BlueBg).clickable {
                            day = today
                            scope.launch { strip.animateScrollToItem(5) }
                        }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = BlueText, modifier = Modifier.size(16.dp))
                        Text("Today", color = BlueText, fontSize = 14.sp)
                    }
                }
            }
        }
        if (muted && day == today) item {
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A1416)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(BellOffIcon, null, tint = Red, modifier = Modifier.size(18.dp))
                Text("Alarms are silent today", color = Color(0xFFF7C1C1), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    "Turn back on", color = Red, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                        Store.mutedDay = ""; Store.mutedAt = 0L; mutedDay = ""
                    }.padding(4.dp),
                )
            }
        }
        items(pendingInvites, key = { "inv_" + it.id }) { inv -> InviteCard(inv) }
        items(requests, key = { "req_" + it.id }) { req ->
            val mine = props[req.id]?.takeIf { it.byUid == Cloud.uid }
            if (mine != null) WaitingCard(req, mine) else RequestCard(req) { open(Screen.Review(taskId = req.id)) }
        }
        items(incoming, key = { "prop_" + it.first.taskId }) { (p, r) -> ChangeCard(p, r) { open(Screen.Changes(p.taskId)) } }
        if (pinned.isNotEmpty()) item {
            LazyRow(
                Modifier.padding(top = 14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(pinned, key = { it.id }) { r -> PinCard(r) { open(Screen.View(r.id)) } }
            }
        }
        item {
            LazyRow(
                state = strip,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth()
                    .onGloballyPositioned { stripBottom = it.positionInRoot().y + it.size.height },
            ) {
                items(days) { d ->
                    val sel = d == day
                    Column(
                        Modifier.width(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Color(0xFF1E1E22) else Color.Transparent)
                            .clickable { day = d }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d.dayOfMonth.toString(), fontFamily = Mono, fontSize = 16.sp, color = if (sel) TextC else Muted)
                        Text(
                            d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                            fontFamily = Mono, fontSize = 10.sp,
                            color = when {
                                sel -> Blue
                                d == today -> TextC
                                else -> Faint
                            },
                        )
                        Box(
                            Modifier.padding(top = 4.dp).size(4.dp).clip(CircleShape)
                                .background(if (d in busy && !sel) Faint else Color.Transparent),
                        )
                    }
                }
            }
            Box(Modifier.padding(top = 8.dp, bottom = 16.dp)) { HLine() }
        }
        if (!permsOk) item {
            Row(
                Modifier.padding(bottom = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF3A1F1F)).clickable { open(Screen.Permissions) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Warning, null, tint = Red)
                Text("Alarms may not open full screen. Tap to allow permissions.", color = Color(0xFFF7C1C1), fontSize = 14.sp)
            }
        }
        if (slots.isEmpty()) item {
            Text(
                if (day == today) "Nothing for today. Tap + to add a reminder." else "Nothing on this day.",
                color = Muted, fontSize = 14.sp, modifier = slide.padding(vertical = 12.dp),
            )
        }
        items(rows) { row ->
            Box(slide) {
                if (row is Slot) SlotRow(row, now, open, silent = muted && day == today && Quiet.applies(row.r)) else NowLine(now)
            }
        }
        if (day == today && upcoming.isNotEmpty()) {
            item { Box(slide) { Caps("Coming up") } }
            items(upcoming, key = { it.id }) { r ->
                Row(
                    slide.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { open(Screen.View(r.id)) }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.title, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fmtUpperDate(r.nextTrigger), fontFamily = Mono, fontSize = 12.sp, color = Faint)
                }
            }
        }
    }
    if (muteSheet) MuteSheet(muted, slots, now, onDismiss = { muteSheet = false }, onDone = { on ->
        if (on) {
            Store.mutedDay = LocalDate.now().toString(); Store.mutedAt = System.currentTimeMillis()
            Toast.makeText(ctx, "Alarms silent for today", Toast.LENGTH_SHORT).show()
        } else {
            Store.mutedDay = ""; Store.mutedAt = 0L
        }
        mutedDay = Store.mutedDay
        muteSheet = false
    })
}

@Composable
fun MuteSheet(muted: Boolean, slots: List<Slot>, now: Long, onDone: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val upcoming = slots.filter { it.paid == null && it.r.type != Type.CHECK && it.at > now }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            if (muted) {
                BigTitle("Alarms are silent today", 22)
                Text(
                    "Turn them back on and the rest of today's alarms will ring as usual.",
                    color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
                )
                BigButton("Turn alarms back on", Blue, OnAccent) { onDone(false) }
            } else {
                BigTitle("Silence today's alarms?", 22)
                Text(
                    if (upcoming.isEmpty()) "Nothing else is set to ring today." else
                        "${upcoming.size} alarm${if (upcoming.size == 1) "" else "s"} left today will show a silent notification instead of ringing.",
                    color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
                upcoming.forEach { s ->
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmtTime(s.at), fontFamily = Mono, fontSize = 12.sp, color = Faint, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(70.dp).padding(end = 8.dp))
                        Text(s.r.title, color = TextC, fontSize = 15.sp)
                    }
                    HLine()
                }
                Text(
                    "Bills and one-off reminders move to tomorrow, repeating ones to their next date. " +
                        "Anything you add after this still rings. Back to normal tomorrow.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 18.dp),
                )
                BigButton("Silence today", Blue, OnAccent) { onDone(true) }
            }
        }
    }
}

@Composable
fun InviteCard(inv: Invite) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
            .border(1.dp, Blue, RoundedCornerShape(18.dp)).padding(14.dp),
    ) {
        Text("${inv.fromName} wants to share a list", color = Muted, fontSize = 13.sp)
        Text(inv.title, color = TextC, fontSize = 17.sp, modifier = Modifier.padding(top = 2.dp))
        Text(
            "${inv.count} item${if (inv.count == 1) "" else "s"} · you can add and tick items. No alarms.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Decline", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.decline(inv) }
            SoftButton(if (busy) "Joining…" else "Accept", Blue, OnAccent, Modifier.weight(1f), height = 46) {
                if (!busy) {
                    busy = true
                    Cloud.accept(inv) { err ->
                        busy = false
                        Toast.makeText(ctx, err ?: "Added to your lists", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(req: TaskRequest, onReview: () -> Unit) {
    if (req.both) return BothRequestCard(req, onReview)
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
            .border(1.dp, Blue, RoundedCornerShape(18.dp)).clickable(onClick = onReview).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(req.fromName)
            Column(Modifier.weight(1f)) {
                Text("${req.fromName} asked you", color = Muted, fontSize = 13.sp)
                Text(req.title, color = TextC, fontSize = 17.sp)
            }
        }
        Text(
            "Due ${dueLabel(req.dueBy)} · Reminder ${fmtTime(req.remindAt)} · " +
                repeatShort(req.toReminder(0)) + if (req.items.isNotEmpty()) " · ${req.items.size} items" else "",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Decline", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.declineTask(req) }
            SoftButton("Review", Blue, OnAccent, Modifier.weight(1f), height = 46, onClick = onReview)
        }
    }
}

/** A shared alarm someone set for both of us, waiting for my one OK. */
@Composable
fun BothRequestCard(req: TaskRequest, onReview: () -> Unit) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    val me = Store.userName.ifBlank { "Me" }
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
            .border(1.dp, Blue, RoundedCornerShape(18.dp)).clickable(onClick = onReview).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(req.fromName)
            Column(Modifier.weight(1f)) {
                Text("${req.fromName} set a shared alarm", color = Muted, fontSize = 13.sp)
                Text(req.title, color = TextC, fontSize = 17.sp)
            }
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = BlueText)) { append("$me + ${req.fromName}") }
                append(" · Rings ${fmtWhen(req.remindAt)} · " + repeatShort(req.toReminder(0)))
            },
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Decline", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.declineTask(req) }
            SoftButton(if (busy) "Adding…" else "Accept", Blue, OnAccent, Modifier.weight(1f), height = 46) {
                if (busy) return@SoftButton
                busy = true
                Cloud.acceptBoth(req) { err ->
                    busy = false
                    Toast.makeText(ctx, err ?: "Added. Rings ${fmtWhen(req.remindAt)}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/** A request I answered with changes, waiting for the other person's OK. */
@Composable
fun WaitingCard(req: TaskRequest, p: Proposal) {
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(req.fromName)
            Column(Modifier.weight(1f)) {
                Text("Waiting for ${req.fromName}'s OK", color = Muted, fontSize = 13.sp)
                Text(req.title, color = TextC, fontSize = 17.sp)
            }
        }
        proposalDiff(req.toReminder(0), p).forEach { (l, o, n) ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Muted)) { append("$l   ") }
                    append(diffText(o, n))
                },
                fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "No alarm until ${req.fromName} accepts.", color = Muted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
        SoftButton("Withdraw changes", Color(0xFF242428), TextC, Modifier.fillMaxWidth(), height = 46) { Cloud.withdraw(req.id) }
    }
}

@Composable
fun ChangeCard(p: Proposal, r: Reminder, onReview: () -> Unit) {
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
            .border(1.dp, Blue, RoundedCornerShape(18.dp)).clickable(onClick = onReview).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(p.byName)
            Column(Modifier.weight(1f)) {
                Text(
                    if (r.taskStatus == "pending") "${p.byName} will do it, with changes" else "${p.byName} suggested changes",
                    color = Muted, fontSize = 13.sp,
                )
                Text(r.title, color = TextC, fontSize = 17.sp)
            }
        }
        Text(
            proposalDiff(r, p).joinToString(" · ") { (l, o, n) ->
                when (l) {
                    "Notes" -> "notes changed"
                    "Added" -> "added $n"
                    "Removed" -> "removed $o"
                    "Renamed" -> "$o renamed to $n"
                    else -> "$l $o → $n"
                }
            },
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Decline", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.declineProposal(p.taskId) }
            SoftButton("Review", Blue, OnAccent, Modifier.weight(1f), height = 46, onClick = onReview)
        }
    }
}

/** Old value crossed out, then the new one. */
fun diffText(old: String, new: String): AnnotatedString = buildAnnotatedString {
    if (old.isNotEmpty()) withStyle(SpanStyle(color = Faint, textDecoration = TextDecoration.LineThrough)) { append(old) }
    if (old.isNotEmpty() && new.isNotEmpty()) withStyle(SpanStyle(color = Muted)) { append("  →  ") }
    if (new.isNotEmpty()) withStyle(SpanStyle(color = TextC)) { append(new) }
}

/** On an assigned errand: changes waiting for an OK, from either side. */
@Composable
fun ProposalCard(r: Reminder, onEdit: () -> Unit) {
    val props by Cloud.proposals.collectAsState()
    val p = r.taskId?.let { props[it] } ?: return
    val mine = p.byUid == Cloud.uid
    val diff = proposalDiff(r, p)
    Column(
        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(if (mine) r.taskPerson else p.byName)
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        mine -> "Waiting for ${r.taskPerson}'s OK"
                        r.taskStatus == "pending" -> "${p.byName} will do it, with changes"
                        else -> "${p.byName} suggested changes"
                    },
                    color = TextC, fontSize = 15.sp,
                )
                Text(
                    if (mine) "You suggested ${diff.size} change${if (diff.size == 1) "" else "s"}" else "Nothing changes until you accept",
                    color = Muted, fontSize = 12.sp,
                )
            }
        }
        diff.forEach { (l, o, n) ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Muted)) { append("$l   ") }
                    append(diffText(o, n))
                },
                fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mine) {
                SoftButton("Withdraw", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.withdraw(p.taskId) }
                SoftButton("Edit again", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46, onClick = onEdit)
            } else {
                SoftButton("Decline", Color(0xFF242428), TextC, Modifier.weight(1f), height = 46) { Cloud.declineProposal(p.taskId) }
                SoftButton("Review", Blue, OnAccent, Modifier.weight(1f), height = 46, onClick = onEdit)
            }
        }
    }
}

private val dueFmt = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH)
fun dueLabel(epochDay: Long): String {
    if (epochDay <= 0) return "anytime"
    val d = LocalDate.ofEpochDay(epochDay)
    return when (d) {
        LocalDate.now() -> "today"
        LocalDate.now().plusDays(1) -> "tomorrow"
        else -> d.format(dueFmt)
    }
}

/** "For Alex · due Sun 09" / "From Sam · due today", or null for normal reminders. */
fun taskLine(r: Reminder): String? {
    if (r.taskId == null) return null
    if (r.isBoth) return when (r.taskStatus) {
        "pending" -> "${r.bothNames} · waiting for ${r.taskPerson}"
        "declined" -> "${r.taskPerson} declined"
        "left" -> "${r.taskPerson} removed it"
        else -> r.bothNames
    }
    val due = "due " + dueLabel(r.dueBy)
    return if (r.assignedByMe) {
        val st = when (r.taskStatus) {
            "pending" -> "waiting for ${r.taskPerson}"
            "declined" -> "${r.taskPerson} declined"
            "done" -> "${r.taskPerson} did it"
            else -> "for ${r.taskPerson}"
        }
        st.replaceFirstChar { it.uppercase() } + " · " + due
    } else "From ${r.taskPerson} · $due"
}

fun taskOverdue(r: Reminder): Boolean =
    r.taskId != null && !r.completed && r.dueBy > 0 && LocalDate.ofEpochDay(r.dueBy).isBefore(LocalDate.now())

@Composable
fun NowLine(now: Long) {
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            fmtTime(now), fontFamily = Mono, fontSize = 11.sp, color = OnAccent,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Blue).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(Blue))
    }
}

@Composable
fun SlotRow(s: Slot, now: Long, open: (Screen) -> Unit, silent: Boolean = false) {
    val r = s.r
    val paid = s.paid
    val quiet = paid != null || r.type == Type.CHECK || s.moved
    Row(Modifier.padding(bottom = 12.dp)) {
        Text(
            fmtTime(s.at), fontFamily = Mono, fontSize = 11.sp,
            color = if (s.overdue) Red else Faint, textAlign = TextAlign.End, maxLines = 1,
            // Right-aligned so the colons and AM/PM line up whatever the hour.
            modifier = Modifier.width(66.dp).padding(top = 16.dp, end = 8.dp),
        )
        val shareMod = if (paid == null) Modifier.sharedCard("card_${r.id}") else Modifier
        TaskCard(onClick = { open(Screen.View(r.id)) }, modifier = Modifier.weight(1f).then(shareMod), outline = quiet) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.title, fontSize = 16.sp, color = if (quiet) Muted else TextC,
                    modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (paid == null) {
                    SmallIcon(EyeIcon, "View") { open(Screen.View(r.id)) }
                    SmallIcon(Icons.Outlined.Edit, "Edit") { open(Screen.Edit(r.id)) }
                }
            }
            val pending = r.items.filter { !it.done }
            val snooze = r.snoozeUntil
            val sub = when {
                s.moved && snooze != null -> "Moved to " + fmtWhen(snooze)
                paid != null -> "Paid · due " + fmtShortDate(paid.dueAt)
                r.type == Type.CHECK -> "Check later · " + Digest.describe(r)
                r.type == Type.ERRAND && r.items.isNotEmpty() ->
                    if (pending.isEmpty()) "Everything got" else "${pending.size} left"
                else -> repeatShort(r)
            }
            val snoozed = !s.moved && paid == null && snooze != null && snooze > now
            val lead = when {
                paid != null || s.moved -> ""
                silent && r.type != Type.CHECK && !s.overdue -> "Silent today · "
                s.overdue -> "Overdue ${shortSpan(now - s.at)} · "
                snoozed -> "Snoozed · rings ${fmtTime(snooze!!)} · "
                else -> ""
            }
            val tl = taskLine(r)
            Text(
                buildAnnotatedString {
                    if (tl != null && r.isBoth) {
                        withStyle(SpanStyle(color = BlueText)) { append(tl) }
                        append(" · ")
                    } else if (tl != null) append("$tl · ")
                    append(lead + sub)
                }, fontSize = 13.sp,
                color = when {
                    s.overdue || taskOverdue(r) -> Red
                    snoozed -> Amber
                    else -> Muted
                },
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (r.type == Type.ERRAND && r.items.isNotEmpty() && paid == null && !s.moved) {
                ItemTicker(r.items)
            }
        }
    }
}

// ---------- lists ----------

@Composable
fun ListsScreen(tick: Int, open: (Screen) -> Unit) {
    val all by Store.reminders.collectAsState()
    val now = remember(tick, all) { System.currentTimeMillis() }
    val visible = all.filter { !it.isTest }
    // Everything finished goes into one folded "Done" group at the bottom.
    fun isDone(r: Reminder) = r.completed || (r.type == Type.CHECK && Digest.isFinished(r))
    val active = visible.filter { !isDone(it) }
    val done = visible.filter { isDone(it) }.sortedByDescending { it.dueAt }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val sections = listOf(
        "Bills and recurring" to active.filter { it.type == Type.BILL }.sortedBy { it.nextTrigger },
        "Errands and tasks" to active.filter { it.type == Type.ERRAND && !it.isSharedMember }.sortedBy { it.nextTrigger },
        "Shared with me" to active.filter { it.isSharedMember }.sortedBy { it.title.lowercase() },
        "Check later" to active.filter { it.type == Type.CHECK }.sortedBy { it.id },
    )
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigTitle("Lists", 44, Modifier.weight(1f))
                FeedbackButton()
            }
            Text("Everything you've set up", color = Muted, fontSize = 14.sp)
        }
        if (visible.isEmpty()) item {
            Text("Nothing yet. Tap + to add your first reminder.", color = Muted, modifier = Modifier.padding(top = 16.dp))
        }
        sections.forEach { (label, list) ->
            if (list.isNotEmpty()) {
                item { Caps(label) }
                items(list, key = { it.id }) { r -> ListCard(r, now, open) }
            }
        }
        if (done.isNotEmpty()) {
            item(key = "done_header") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Line, RoundedCornerShape(18.dp)).clickable { showDone = !showDone }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                    Text("Done · ${done.size}", color = Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(if (showDone) "Hide" else "Show", color = BlueText, fontSize = 14.sp)
                }
            }
            if (showDone) items(done, key = { "done_" + it.id }) { r -> ListCard(r, now, open) }
        }
    }
}

@Composable
fun ListCard(r: Reminder, now: Long, open: (Screen) -> Unit) {
    val dim = r.completed || (r.type == Type.CHECK && Digest.isFinished(r))
    TaskCard(onClick = { open(Screen.View(r.id)) }, modifier = Modifier.sharedCard("card_${r.id}"), outline = r.type == Type.CHECK) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                r.title, fontSize = 16.sp, color = if (dim) Muted else TextC,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (r.sharedId != null || r.taskId != null) Icon(PeopleIcon, "Shared", tint = Blue, modifier = Modifier.size(16.dp).padding(end = 2.dp))
            if (r.pinned) Icon(PinIcon, "Pinned", tint = Blue, modifier = Modifier.size(16.dp))
            if (r.completed) Icon(Icons.Outlined.CheckCircle, "Done", tint = Green, modifier = Modifier.size(18.dp))
            SmallIcon(EyeIcon, "View") { open(Screen.View(r.id)) }
            if (!r.isSharedMember) SmallIcon(Icons.Outlined.Edit, "Edit") { open(Screen.Edit(r.id)) }
        }
        val overdue = r.type != Type.CHECK && !r.completed && r.dueAt in 1..now
        val sub = when {
            r.type == Type.CHECK -> Digest.describe(r)
            r.type == Type.ERRAND && r.items.isNotEmpty() -> {
                val left = r.items.count { !it.done }
                "$left left · ${statusText(r, now)}"
            }
            else -> "${repeatShort(r)} · ${statusText(r, now)}"
        }
        val tl = taskLine(r)
        Text(
            (tl?.let { "$it · " } ?: "") + sub, fontSize = 13.sp,
            color = if (overdue || taskOverdue(r)) Red else Muted,
        )
        if (r.type == Type.ERRAND && r.items.isNotEmpty()) {
            ItemTicker(r.items)
        }
    }
}

// ---------- view (read-only, except the list) ----------

@Composable
fun ViewScreen(id: Long, onEdit: () -> Unit, onClose: () -> Unit, onAddPerson: () -> Unit) {
    val ctx = LocalContext.current
    val live by Store.reminders.collectAsState()
    val r = live.find { it.id == id }
    if (r == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val items = remember(id) { mutableStateListOf<Item>().apply { addAll(r.items) } }
    LaunchedEffect(r.items) {
        if (r.items != items.toList()) {
            items.clear(); items.addAll(r.items)
        }
    }
    var share by remember { mutableStateOf(false) }
    var askReset by remember { mutableStateOf(false) }
    var people by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val member = r.isSharedMember

    Column(
        Modifier.fillMaxSize().sharedCard("card_$id").glow().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose) {
            if (r.type == Type.ERRAND) {
                IconButton(onClick = {
                    Store.get(id)?.let { Store.upsert(it.copy(pinned = !it.pinned)) }
                    Toast.makeText(ctx, if (r.pinned) "Unpinned" else "Pinned to Today", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(PinIcon, if (r.pinned) "Unpin" else "Pin to Today", tint = if (r.pinned) Blue else Color(0xFFB8B8BE))
                }
                IconButton(onClick = { share = true }) { Icon(Icons.Outlined.Share, "Send list as text", tint = Blue) }
                if (!member && r.taskId == null) IconButton(onClick = { people = true }) {
                    Icon(PeopleIcon, "Share live with people", tint = if (r.sharedId != null) Blue else Color(0xFFB8B8BE))
                }
            }
            if (!member) IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit reminder", tint = Color(0xFFB8B8BE)) }
        }
        BigTitle(r.title, 34, Modifier.padding(top = 4.dp))
        Text(
            ((taskLine(r)?.let { "$it · " } ?: "") +
                (if (member) "Shared by ${r.sharedBy}" + (if (r.pinned) " · pinned" else "")
                else whenLine(r) + if (r.pinned) " · pinned" else "")).uppercase(Locale.ENGLISH),
            fontFamily = Mono, fontSize = 12.sp, color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (r.sharedId != null) {
            val with = if (member) (listOf(r.sharedBy) + r.sharedWith.filter { it != r.sharedBy }).distinct() else r.sharedWith
            Row(
                Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = !member) { people = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(PeopleIcon, null, tint = Blue, modifier = Modifier.size(16.dp))
                Text(
                    if (with.isEmpty()) "Shared · waiting for someone to accept" else "Shared with ${with.joinToString(", ")} · live",
                    color = BlueText, fontSize = 13.sp,
                )
            }
        }
        if (r.notes.isNotBlank()) {
            Text(r.notes, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
        }
        if (r.assignedByMe) TaskStatusCard(r, onClose)
        if (r.taskId != null) ProposalCard(r, onEdit)
        if (r.assignedToMe && !r.completed) {
            Spacer(Modifier.height(14.dp))
            SoftButton("Make changes", CardBg, TextC, Modifier.fillMaxWidth(), height = 48, onClick = onEdit)
        }

        when (r.type) {
            Type.ERRAND -> {
                ItemsEditor(items, r.title, showLabel = false) {
                    Store.get(id)?.let { Store.upsert(it.copy(items = items.toList())) }
                }
                if (r.assignedByMe) {
                    // Their phone marks it done.
                } else if (member) {
                    Spacer(Modifier.height(24.dp))
                    SoftButton("Leave this list", CardBg, Red, Modifier.fillMaxWidth()) { confirmLeave = true }
                } else if (!r.completed || r.repeat != Repeat.NONE) {
                    Spacer(Modifier.height(24.dp))
                    BigButton(if (r.items.isEmpty()) "Mark as done" else "Done with this list", Blue, OnAccent) {
                        if (r.repeat != Repeat.NONE && r.items.isNotEmpty()) askReset = true
                        else finishList(ctx, r, null)
                    }
                }
            }
            Type.BILL -> {
                if (!r.completed) {
                    Spacer(Modifier.height(24.dp))
                    BigButton("Mark this cycle as paid", Green, GreenDark) { Actions.handle(ctx, r.id, Act.DONE) }
                }
                if (r.history.isNotEmpty()) {
                    Caps("Paid history")
                    r.history.forEach { PaidRow(it) }
                }
            }
            Type.CHECK -> {
                Spacer(Modifier.height(24.dp))
                BigButton("Checked, remove it", CardBg, TextC) {
                    Store.logDone(r); Store.delete(r.id); onClose()
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
    if (share) ShareSheet(r.title, items.toList()) { share = false }
    if (people) PeopleSheet(r, onAddPerson = { people = false; onAddPerson() }) { people = false }
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        containerColor = SheetBg,
        title = { Text("Leave ${r.title}?") },
        text = { Text("It'll be removed from your phone. ${r.sharedBy} keeps the list.", color = Muted) },
        confirmButton = {
            TextButton(onClick = { confirmLeave = false; Cloud.leave(r); onClose() }) { Text("Leave", color = Red) }
        },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel", color = Muted) } },
    )
    if (askReset) ResetDialog(r, onDismiss = { askReset = false }) { mode ->
        askReset = false
        finishList(ctx, r, mode)
    }
}

@Composable
fun TaskStatusCard(r: Reminder, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val who = r.taskPerson
    val (line, sub) = when (r.taskStatus) {
        "pending" -> "Waiting for $who to accept" to "You'll get a notification when they do."
        "accepted" -> "$who accepted" to "Their reminder: ${whenLine(r)}"
        "declined" -> "$who declined" to "Do it yourself, or delete it."
        "done" -> "$who did it" to (r.history.firstOrNull()?.let { "On ${fmtShortDate(it.paidAt)} at ${fmtTime(it.paidAt)}" } ?: "")
        else -> who to ""
    }
    Column(
        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(who)
            Column(Modifier.weight(1f)) {
                Text(line, color = TextC, fontSize = 15.sp)
                if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 12.sp)
            }
            when (r.taskStatus) {
                "accepted", "done" -> Icon(Icons.Outlined.CheckCircle, null, tint = Green, modifier = Modifier.size(20.dp))
                "declined" -> Icon(Icons.Outlined.Close, null, tint = Red, modifier = Modifier.size(20.dp))
                else -> {}
            }
        }
        if (r.taskStatus == "declined") {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("Delete", Color(0xFF242428), Red, Modifier.weight(1f), height = 46) {
                    Store.delete(r.id); onClose()
                }
                SoftButton("Do it myself", Blue, OnAccent, Modifier.weight(1f), height = 46) {
                    Cloud.takeBack(r)
                    Toast.makeText(ctx, "It's yours now, with an alarm", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * Done with a list. If this cycle is due (today or overdue) it completes the cycle;
 * if the alarm was already answered, it only tidies the list for next time.
 */
fun finishList(ctx: android.content.Context, r: Reminder, mode: ListMode?) {
    val dueNow = !toLdt(r.nextTrigger).toLocalDate().isAfter(LocalDate.now())
    if (!r.completed && (dueNow || r.repeat == Repeat.NONE)) {
        Actions.handle(ctx, r.id, Act.DONE, listMode = mode)
    } else if (mode != null) {
        Store.get(r.id)?.let { Store.upsert(it.copy(items = applyListMode(it.items, mode))) }
    }
    Toast.makeText(ctx, "Done. Next: ${Store.get(r.id)?.let { if (it.completed) "nothing scheduled" else fmtWhen(it.nextTrigger) } ?: ""}", Toast.LENGTH_SHORT).show()
}

@Composable
fun ResetDialog(r: Reminder, onDismiss: () -> Unit, onPick: (ListMode) -> Unit) {
    val got = r.items.count { it.done }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        title = { BigTitle("Next time's list", 20) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You've ticked $got of ${r.items.size}. What should the list look like next time?", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                SoftButton("Untick everything, same list", Blue, OnAccent, Modifier.fillMaxWidth(), height = 50) { onPick(ListMode.UNTICK_ALL) }
                SoftButton("Keep only what's not got", CardBg, TextC, Modifier.fillMaxWidth(), height = 50) { onPick(ListMode.KEEP_PENDING) }
                SoftButton("Start with an empty list", CardBg, TextC, Modifier.fillMaxWidth(), height = 50) { onPick(ListMode.EMPTY) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
    )
}

// ---------- live sharing ----------

@Composable
fun ActivityLog(listId: String) {
    val all by Cloud.logs.collectAsState()
    val entries = all[listId] ?: emptyList()
    var open by remember { mutableStateOf(false) }
    if (entries.isEmpty()) return
    Row(
        Modifier.padding(top = 18.dp).fillMaxWidth().clickable { open = !open }.padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("ACTIVITY", fontSize = 11.sp, letterSpacing = 0.8.sp, color = Faint, fontFamily = Mono, modifier = Modifier.weight(1f))
        Text(if (open) "Hide" else "Show ${entries.size}", color = Muted, fontSize = 12.sp)
        Icon(if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = Muted)
    }
    if (open) entries.forEach { e ->
        Row(Modifier.padding(vertical = 7.dp, horizontal = 2.dp)) {
            Text(
                (if (e.uid == Cloud.uid) "You" else e.who) + " " + e.text,
                color = TextC, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            Text(fmtLogTime(e.ts), color = Faint, fontSize = 11.sp, fontFamily = Mono)
        }
    }
}

fun fmtLogTime(ts: Long): String {
    if (ts <= 0) return ""
    val d = toLdt(ts).toLocalDate()
    return when (d) {
        LocalDate.now() -> fmtTime(ts)
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> fmtShortDate(ts)
    }
}

@Composable
fun PeopleSheet(r: Reminder, onAddPerson: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val people by Cloud.people.collectAsState()
    val ready by Cloud.ready.collectAsState()
    var picked by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            BigTitle("Share ${r.title} live", 22)
            Text(
                "They can add, tick and edit items. Alarms, time and repeat stay yours.",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
            if (!ready) Text("Connecting… check your internet.", color = Amber, fontSize = 13.sp)
            if (people.isEmpty()) {
                Text("You haven't added anyone yet.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            people.forEach { p ->
                val already = p.name in r.sharedWith
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !already) {
                        picked = if (p.uid in picked) picked - p.uid else picked + p.uid
                    }.padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(p.name)
                    Text(p.name, color = TextC, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (already) {
                        Text(
                            "Remove", color = Red, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Cloud.removeMember(r, p.uid) }.padding(6.dp),
                        )
                    } else {
                        Checkbox(
                            p.uid in picked, onCheckedChange = { picked = if (it) picked + p.uid else picked - p.uid },
                            colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Color(0xFF4A4A50)),
                        )
                    }
                }
                HLine()
            }
            Text(
                "+ Add a person", color = Blue, fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onAddPerson)
                    .padding(vertical = 12.dp, horizontal = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            if (picked.isNotEmpty()) {
                BigButton(if (busy) "Sending…" else "Send invite", Blue, OnAccent) {
                    if (busy) return@BigButton
                    busy = true
                    Cloud.share(r, people.filter { it.uid in picked }) { err ->
                        busy = false
                        Toast.makeText(ctx, err ?: "Invite sent. It shows up for them once they accept.", Toast.LENGTH_LONG).show()
                        if (err == null) onDismiss()
                    }
                }
            }
            if (r.sharedId != null) {
                Text(
                    "Stop sharing with everyone", color = Red, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { Cloud.stopSharing(r); onDismiss() }.padding(12.dp),
                )
            }
        }
    }
}

@Composable
fun Avatar(name: String) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(BlueBg),
        contentAlignment = Alignment.Center,
    ) { Text(name.take(1).uppercase(), color = BlueText, fontSize = 14.sp) }
}

@Composable
fun AddPersonScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val ready by Cloud.ready.collectAsState()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val mine = Store.myCode
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose)
        BigTitle("Add a person", 34)
        Text(
            "One of you sends a code, the other types it in. You only do this once, then you can share lists with each other.",
            color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp),
        )
        if (!ready) Text("Connecting… check your internet.", color = Amber, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Caps("Your code")
        Text(
            mine.ifBlank { "……" }, fontFamily = Mono, fontSize = 30.sp, letterSpacing = 4.sp, color = TextC,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(vertical = 18.dp),
        )
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Copy", CardBg, TextC, Modifier.weight(1f), height = 50) {
                clip.setText(AnnotatedString(mine))
                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }
            SoftButton("Send code", Blue, OnAccent, Modifier.weight(2f), height = 50) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                    Intent.EXTRA_TEXT,
                    "Add me on Geddydun so we can share lists. My code: $mine",
                )
                ctx.startActivity(Intent.createChooser(send, "Send your code"))
            }
        }
        Caps("Or enter their code")
        OutlinedTextField(
            value = code, onValueChange = { code = it.uppercase().take(8); err = null },
            singleLine = true, shape = FieldShape, colors = fieldColors(),
            placeholder = { Text("AB-CDEF") },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 20.sp, letterSpacing = 3.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        err?.let { Text(it, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(14.dp))
        BigButton(if (busy) "Connecting…" else "Connect", if (code.length >= 6) Blue else CardBg, if (code.length >= 6) OnAccent else Muted) {
            if (busy || code.length < 6) return@BigButton
            busy = true
            Cloud.connect(code) { e ->
                busy = false
                if (e == null) {
                    Toast.makeText(ctx, "Connected", Toast.LENGTH_SHORT).show(); onClose()
                } else err = e
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ---------- share ----------

fun listText(title: String, items: List<Item>, everything: Boolean): String = buildString {
    append(title)
    val list = if (everything) items.sortedBy { it.done } else items.filter { !it.done }
    if (list.isEmpty()) append("\n(nothing left to get)")
    list.forEach { append("\n").append(if (it.done) "☑ " else "☐ ").append(it.name) }
}

@Composable
fun ShareSheet(title: String, items: List<Item>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var everything by remember { mutableStateOf(false) }
    val pending = items.count { !it.done }
    val text = listText(title, items, everything)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            BigTitle("Share $title", 22)
            Text(
                text, fontFamily = Mono, fontSize = 13.sp, color = Muted, lineHeight = 21.sp,
                modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth().heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(12.dp)).background(CardBg)
                    .verticalScroll(rememberScrollState()).padding(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Remaining ($pending)", !everything) { everything = false }
                Pill("Everything (${items.size})", everything) { everything = true }
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("Copy", CardBg, TextC, Modifier.weight(1f)) {
                    clip.setText(AnnotatedString(text))
                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
                SoftButton("Share to app", Blue, OnAccent, Modifier.weight(2f)) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, title)
                        .putExtra(Intent.EXTRA_TEXT, text)
                    ctx.startActivity(Intent.createChooser(send, "Share $title"))
                    onDismiss()
                }
            }
        }
    }
}

// ---------- add / edit ----------

@Composable
fun EditScreen(id: Long?, sendBack: Boolean = false, shareTitle: String? = null, shareNotes: String = "", onClose: () -> Unit) {
    val ctx = LocalContext.current
    val existing = remember(id) { id?.let { Store.get(it) } }
    val live by Store.reminders.collectAsState()
    val current = live.find { it.id == id }
    // An accepted assigned errand: shared changes go to the other person for their OK.
    val review = existing?.taskId != null && existing.taskStatus == "accepted" && !existing.isBoth
    val other = existing?.taskPerson ?: ""
    // Start from my own waiting suggestion, or from theirs when changing it and sending it back.
    val startProp = remember(id) {
        existing?.taskId?.let { Cloud.proposals.value[it] }?.takeIf { it.byUid == Cloud.uid || sendBack }
    }
    val seed = remember(id) { if (existing != null && startProp != null) startProp.applyTo(existing) else existing }

    var type by remember { mutableStateOf(existing?.type ?: if (shareTitle != null) Type.ERRAND else Type.BILL) }
    var title by remember { mutableStateOf(seed?.title ?: shareTitle ?: "") }
    var notes by remember { mutableStateOf(seed?.notes ?: shareNotes) }
    val initDt = remember {
        seed?.takeIf { it.dueAt > 0 && it.type != Type.CHECK }?.let { toLdt(it.dueAt) }
            ?: LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
    var date by remember { mutableStateOf(initDt.toLocalDate()) }
    var time by remember { mutableStateOf(initDt.toLocalTime()) }
    var repeat by remember { mutableStateOf(seed?.repeat ?: Repeat.MONTHLY) }
    var everyDays by remember { mutableStateOf((seed?.everyDays ?: 2).toString()) }
    var fromDone by remember { mutableStateOf(seed?.fromDone ?: false) }
    var nth by remember { mutableIntStateOf(seed?.takeIf { it.repeat == Repeat.NTH_WEEKDAY }?.nth ?: minOf(4, (initDt.dayOfMonth - 1) / 7 + 1)) }
    var weekday by remember {
        mutableIntStateOf(seed?.takeIf { it.repeat == Repeat.NTH_WEEKDAY || it.repeat == Repeat.EVERY_N_WEEKS }?.weekday ?: initDt.dayOfWeek.value)
    }
    var weekMask by remember {
        mutableIntStateOf(seed?.takeIf { it.repeat == Repeat.WEEKDAYS }?.weekMask ?: (1 shl (initDt.dayOfWeek.value - 1)))
    }
    val items = remember { mutableStateListOf<Item>().apply { addAll(seed?.items ?: emptyList()) } }
    var error by remember { mutableStateOf<String?>(null) }
    val wasCheck = existing?.type == Type.CHECK
    var checkFreq by remember {
        mutableStateOf(existing?.repeat?.takeIf { wasCheck && it in listOf(Repeat.WEEKLY, Repeat.MONTHLY) } ?: Repeat.DAILY)
    }
    var maxNotifs by remember { mutableIntStateOf(if (wasCheck) existing!!.maxNotifs else 0) }
    var checkDue by remember {
        mutableStateOf(existing?.takeIf { wasCheck && it.dueAt > 0 }?.let { toLdt(it.dueAt).toLocalDate() })
    }
    val people by Cloud.people.collectAsState()
    var forUid by remember { mutableStateOf(existing?.takeIf { it.assignedByMe }?.taskPersonUid ?: "") }
    var dueBy by remember { mutableStateOf(seed?.dueByDate ?: LocalDate.now().plusDays(1)) }
    val assignLocked = existing?.assignedByMe == true
    val bothLocked = existing?.isBoth == true
    var both by remember { mutableStateOf(bothLocked) }
    val myLabel = Store.userName.ifBlank { "Me" }
    val assigning = type == Type.ERRAND && forUid.isNotBlank() && !both
    val forName = people.find { it.uid == forUid }?.name ?: existing?.taskPerson ?: ""
    val accepted = existing?.assignedByMe == true && existing.taskStatus in setOf("accepted", "done")
    val canAssign = existing?.sharedId == null && existing?.assignedToMe != true && (people.isNotEmpty() || assignLocked)

    fun persistItems() {
        if (existing != null) Store.get(existing.id)?.let { Store.upsert(it.copy(items = items.toList())) }
    }

    /** Accepted assigned errand: my reminder time applies now; everything else goes for an OK. */
    fun saveForReview(cur: Reminder) {
        val n = everyDays.toIntOrNull()?.coerceIn(1, 365) ?: 1
        if (repeat == Repeat.WEEKDAYS && weekMask == 0) {
            error = "Pick at least one day"; return
        }
        val d = dueBy
        if (d.toEpochDay() != cur.dueBy && d.isBefore(LocalDate.now())) {
            error = "Pick a due date from today onwards"; return
        }
        val edited = cur.copy(
            title = title.trim(), notes = notes.trim(), dueBy = d.toEpochDay(), repeat = repeat, everyDays = n,
            anchorDay = date.dayOfMonth, nth = nth, weekday = weekday, weekMask = weekMask,
        )
        // Notes change straight away; title, dates, time and repeat go for an OK.
        if (notes.trim() != cur.notes) Cloud.setNotes(cur, notes.trim())
        // The reminder time is part of the suggestion too, whoever changes it.
        var remindAt = 0L
        val picked = LocalDateTime.of(date, time)
        if (picked != toLdt(cur.dueAt).withSecond(0).withNano(0)) {
            val ms = fromLdt(Rules.firstOnOrAfter(edited, picked))
            if (ms <= System.currentTimeMillis()) {
                error = "Pick a date and time in the future"; return
            }
            remindAt = ms
        }
        val asProp = Proposal.of(cur.taskId!!, "", "", edited, remindAt)
        val sameAsStart = startProp != null && !sendBack && proposalDiff(startProp.applyTo(cur), asProp).isEmpty()
        if (proposalDiff(cur, asProp).isNotEmpty() && !sameAsStart) {
            Cloud.propose(cur, if (remindAt > 0) edited.copy(dueAt = remindAt) else edited) { err ->
                if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
            }
            Toast.makeText(ctx, "Sent to $other for their OK", Toast.LENGTH_SHORT).show()
        } else if (startProp != null && !sendBack && proposalDiff(cur, asProp).isEmpty()) {
            Cloud.withdraw(cur.taskId) // changed everything back
        }
        onClose()
    }

    fun save() {
        if (title.isBlank()) {
            error = "Enter a title"; return
        }
        if (review && existing != null) {
            saveForReview(Store.get(existing.id) ?: existing); return
        }
        val base = Store.get(id ?: -1) ?: Reminder(id = Store.newId(), type = type, title = "")
        var r = base.copy(
            type = type, title = title.trim(), notes = notes.trim(),
            items = if (type == Type.ERRAND) items.toList() else emptyList(),
        )
        if (type != Type.CHECK && accepted) {
            // They set their own reminder now; keep it as it is.
        } else if (type != Type.CHECK) {
            val n = everyDays.toIntOrNull()?.coerceIn(1, 365) ?: 1
            if (repeat == Repeat.WEEKDAYS && weekMask == 0) {
                error = "Pick at least one day"; return
            }
            val rule = r.copy(
                repeat = repeat, everyDays = n, anchorDay = date.dayOfMonth, nth = nth, weekday = weekday, weekMask = weekMask,
                fromDone = fromDone && repeat.canTrail && type != Type.BILL,
            )
            var first = Rules.firstOnOrAfter(rule, LocalDateTime.of(date, time))
            if (repeat.isCustom) {
                val nowL = LocalDateTime.now()
                var g = 0
                while (!first.isAfter(nowL) && g++ < 2000) first = Rules.step(rule, first)
            }
            val dueMs = fromLdt(first)
            val changed = existing == null || existing.type == Type.CHECK || existing.dueAt != dueMs ||
                existing.repeat != repeat || existing.everyDays != n || existing.completed ||
                existing.nth != nth || existing.weekday != weekday || existing.weekMask != weekMask
            if (changed && dueMs <= System.currentTimeMillis()) {
                error = "Pick a date and time in the future"; return
            }
            r = rule.copy(dueAt = dueMs)
            if (changed) r = r.copy(snoozeUntil = null, completed = false, rings = 0)
        } else {
            val due = checkDue
            if (due != null && due.isBefore(LocalDate.now())) {
                error = "Pick a due date from today onwards"; return
            }
            r = r.copy(
                repeat = checkFreq, maxNotifs = maxNotifs,
                dueAt = due?.let { fromLdt(it.atStartOfDay()) } ?: 0L,
                snoozeUntil = null, completed = false,
                notifCount = if (wasCheck) base.notifCount else 0,
            )
        }
        if (type == Type.ERRAND && both && type != Type.CHECK) {
            if (existing?.isBoth == true) {
                // A change to the time is shared; my snoozes aren't.
                if (r.dueAt != existing.dueAt || r.repeat != existing.repeat) r = r.copy(taskBaseAt = r.dueAt)
            } else {
                val p = people.find { it.uid == forUid }
                if (p == null) {
                    error = "Pick who it's with"; return
                }
                Cloud.assignBoth(r, p) { err -> if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_LONG).show() }
                Toast.makeText(ctx, "Set. Sent to ${p.name} to accept", Toast.LENGTH_SHORT).show()
                onClose()
                return
            }
        }
        if (assigning) {
            val d = dueBy
            if ((existing == null || existing.dueBy != d.toEpochDay()) && d.isBefore(LocalDate.now())) {
                error = "Pick a due date from today onwards"; return
            }
            r = r.copy(dueBy = d.toEpochDay())
            if (existing?.assignedByMe != true) {
                val p = people.find { it.uid == forUid }
                if (p == null) {
                    error = "Pick who it's for"; return
                }
                Cloud.assign(r, p) { err -> if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_LONG).show() }
                Toast.makeText(ctx, "Sent to ${p.name}", Toast.LENGTH_SHORT).show()
                onClose()
                return
            }
        }
        Store.upsert(r)
        Scheduler.schedule(ctx, r)
        onClose()
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.Outlined.Close, "Close", onClose)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigTitle(if (existing == null) "New reminder" else if (review) "Edit errand" else "Edit reminder", 28, Modifier.weight(1f))
            Text(
                if (review) "Send" else "Save", color = OnAccent, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Blue).clickable { save() }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            )
        }
        if (review) {
            Text(
                "CHANGES GO TO ${other.uppercase(Locale.ENGLISH)} FOR AN OK",
                fontFamily = Mono, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (existing?.taskId == null) {
            Caps("Type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(Type.BILL, "Bill", BillIcon), Triple(Type.ERRAND, "Errand", ErrandIcon),
                    Triple(Type.CHECK, "Check later", CheckLaterIcon),
                ).forEach { (t, l, ic) ->
                    Pill(l, type == t, ic) {
                        type = t
                        if (existing == null) repeat = if (t == Type.BILL) Repeat.MONTHLY else Repeat.NONE
                    }
                }
            }
        }
        Caps("Title")
        OutlinedTextField(
            value = title, onValueChange = { title = it; error = null },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            placeholder = {
                Text(
                    when (type) {
                        Type.BILL -> "Rent"
                        Type.ERRAND -> "Get groceries"
                        Type.CHECK -> "Is the blue kettle back in stock?"
                    },
                )
            },
        )

        if (type == Type.ERRAND && (canAssign || bothLocked)) {
            Caps("For")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (assignLocked) {
                    Pill(forName, true) {}
                } else if (bothLocked) {
                    Pill("Both", true) {}
                } else {
                    Pill(myLabel, forUid.isBlank() && !both) { forUid = ""; both = false }
                    people.forEach { p -> Pill(p.name, forUid == p.uid && !both) { forUid = p.uid; both = false; error = null } }
                    if (people.size == 1) {
                        Pill("Both", both) { forUid = people[0].uid; both = true; error = null }
                    } else people.forEach { p ->
                        Pill("$myLabel + ${p.name}", both && forUid == p.uid) { forUid = p.uid; both = true; error = null }
                    }
                }
            }
            if (both) {
                val with = if (bothLocked) existing!!.taskPerson else people.find { it.uid == forUid }?.name ?: ""
                Text(
                    "Rings for $myLabel and $with. Each of you snoozes or finishes it on your own." +
                        if (bothLocked) " Changes to the title, time or repeat reach $with straight away." else " $with accepts it once, then it rings on both phones.",
                    color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (assigning) {
                Caps("Due by")
                FieldButton(fmtDate(dueBy), Modifier.fillMaxWidth()) {
                    DatePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, y, m, d -> dueBy = LocalDate.of(y, m + 1, d); error = null },
                        dueBy.year, dueBy.monthValue - 1, dueBy.dayOfMonth,
                    ).show()
                }
            }
        }

        if (review && existing?.assignedToMe == true) {
            Caps("Due by")
            FieldButton(fmtDate(dueBy), Modifier.fillMaxWidth()) {
                DatePickerDialog(
                    ctx, android.R.style.Theme_Material_Dialog,
                    { _, y, m, d -> dueBy = LocalDate.of(y, m + 1, d); error = null },
                    dueBy.year, dueBy.monthValue - 1, dueBy.dayOfMonth,
                ).show()
            }
        }

        if (type != Type.CHECK && accepted && review) {
            Caps("Remind $forName at")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldButton(fmtDate(date), Modifier.weight(1f)) {
                    DatePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d); error = null },
                        date.year, date.monthValue - 1, date.dayOfMonth,
                    ).show()
                }
                FieldButton(fmtTime(time), Modifier.width(124.dp), mono = true) {
                    TimePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, h, mi -> time = LocalTime.of(h, mi); error = null },
                        time.hour, time.minute, false,
                    ).show()
                }
            }
            Text(
                "It's $forName's alarm. Changing it sends it to them for an OK.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
            RepeatPicker(
                type, date, time, repeat, { repeat = it; error = null }, everyDays, { everyDays = it },
                nth, { nth = it }, weekday, { weekday = it }, weekMask, { weekMask = it; error = null },
            )
        } else if (type != Type.CHECK && accepted) {
            Caps("$forName's reminder")
            Text(whenLine(existing!!), color = Muted, fontSize = 14.sp)
            if (review) RepeatPicker(
                type, date, time, repeat, { repeat = it; error = null }, everyDays, { everyDays = it },
                nth, { nth = it }, weekday, { weekday = it }, weekMask, { weekMask = it; error = null },
            )
        } else if (type != Type.CHECK) {
            Caps(if (type == Type.BILL) "Due" else if (review) "Remind me at" else if (assigning) "Remind $forName at" else "When")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldButton(fmtDate(date), Modifier.weight(1f)) {
                    DatePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d); error = null },
                        date.year, date.monthValue - 1, date.dayOfMonth,
                    ).show()
                }
                FieldButton(fmtTime(time), Modifier.width(124.dp), mono = true) {
                    TimePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, h, mi -> time = LocalTime.of(h, mi); error = null },
                        time.hour, time.minute, false,
                    ).show()
                }
            }
            if (review) {
                Text(
                    "Changing it sends it to $other for an OK.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
            RepeatPicker(
                type, date, time, repeat,
                {
                    repeat = it; error = null
                    // New "Every N days" reminders count from when they're done, unless switched off.
                    if (existing == null && it == Repeat.EVERY_N_DAYS) fromDone = true
                },
                everyDays, { everyDays = it },
                nth, { nth = it }, weekday, { weekday = it }, weekMask, { weekMask = it; error = null },
                fromDone = fromDone, setFromDone = if (review) null else ({ fromDone = it }),
            )
        }

        if (assigning && !accepted) {
            Text(
                "$forName gets the alarm once they accept. If they change anything, even the time, it comes back to you for an OK. The same goes both ways after that.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (type == Type.ERRAND) ItemsEditor(items, title.ifBlank { "List" }, showLabel = true, optional = true) { persistItems() }

        if (type == Type.CHECK) {
            Caps("Due date (optional)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldButton(checkDue?.let { fmtDate(it) } ?: "No due date", Modifier.weight(1f)) {
                    val d = checkDue ?: LocalDate.now().plusDays(7)
                    DatePickerDialog(
                        ctx, android.R.style.Theme_Material_Dialog,
                        { _, y, m, dd -> checkDue = LocalDate.of(y, m + 1, dd); error = null },
                        d.year, d.monthValue - 1, d.dayOfMonth,
                    ).show()
                }
                if (checkDue != null) IconButton(onClick = { checkDue = null }) {
                    Icon(Icons.Outlined.Close, "Remove due date", tint = Muted)
                }
            }
            Text(
                "Reminders stop after this date. On the day itself you always get one.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
            Caps("How often")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Repeat.DAILY to "Daily", Repeat.WEEKLY to "Weekly", Repeat.MONTHLY to "Monthly")
                    .forEach { (rp, l) -> Pill(l, checkFreq == rp) { checkFreq = rp } }
            }
            Caps("Number of reminders")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Until I tick it", 1 to "1", 3 to "3", 5 to "5", 7 to "7", 10 to "10")
                    .forEach { (n, l) -> Pill(l, maxNotifs == n) { maxNotifs = n } }
            }
            if (existing != null && wasCheck && existing.notifCount > 0) {
                Text(
                    "Reminded ${existing.notifCount} time${if (existing.notifCount == 1) "" else "s"} so far.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Caps("Notes")
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(), shape = FieldShape, colors = fieldColors(),
            placeholder = { Text("Optional") },
        )

        error?.let { Text(it, color = Red, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }

        if (current != null && current.type == Type.BILL && !current.completed) {
            Spacer(Modifier.height(20.dp))
            BigButton("Mark this cycle as paid", Green, GreenDark) {
                Actions.handle(ctx, current.id, Act.DONE); onClose()
            }
        }
        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {
                Scheduler.cancel(ctx, existing.id)
                AlarmService.resolve(ctx, existing.id)
                Store.delete(existing.id)
                onClose()
            }) {
                Icon(Icons.Outlined.Delete, null, tint = Red)
                Spacer(Modifier.width(6.dp))
                Text(if (existing.assignedByMe) "Cancel and delete" else "Delete reminder", color = Red)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** Once / Daily / ... / Custom, with the custom editor and a plain-English note. */
@Composable
fun RepeatPicker(
    type: Type, date: LocalDate, time: LocalTime,
    repeat: Repeat, setRepeat: (Repeat) -> Unit,
    everyDays: String, setEvery: (String) -> Unit,
    nth: Int, setNth: (Int) -> Unit,
    weekday: Int, setWeekday: (Int) -> Unit,
    weekMask: Int, setMask: (Int) -> Unit,
    label: String = "Repeats",
    fromDone: Boolean = false,
    setFromDone: ((Boolean) -> Unit)? = null,
) {
    Caps(label)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Repeat.NONE to "Once", Repeat.DAILY to "Daily", Repeat.WEEKLY to "Weekly",
            Repeat.MONTHLY to "Monthly", Repeat.YEARLY to "Yearly",
        ).forEach { (rp, l) -> Pill(l, repeat == rp) { setRepeat(rp) } }
        Pill("Custom", repeat.isCustom) { if (!repeat.isCustom) setRepeat(Repeat.NTH_WEEKDAY) }
    }
    if (repeat.isCustom) {
        Column(
            Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .border(1.dp, Line, RoundedCornerShape(18.dp)).background(Color(0xFF141417)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Repeat.NTH_WEEKDAY to "Nth weekday", Repeat.WEEKDAYS to "Weekdays",
                    Repeat.EVERY_N_WEEKS to "Every N weeks", Repeat.EVERY_N_DAYS to "Every N days",
                    Repeat.LAST_DAY to "Last day",
                ).forEach { (rp, l) -> Pill(l, repeat == rp) { setRepeat(rp) } }
            }
            when (repeat) {
                Repeat.NTH_WEEKDAY -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 5 to "Last")
                            .forEach { (n, l) -> Pill(l, nth == n) { setNth(n) } }
                    }
                    DayCircles({ it == weekday }) { setWeekday(it) }
                }
                Repeat.WEEKDAYS -> DayCircles({ weekMask and (1 shl (it - 1)) != 0 }) {
                    setMask(weekMask xor (1 shl (it - 1)))
                }
                Repeat.EVERY_N_WEEKS -> {
                    NumberRow("Every", everyDays, "weeks on") { setEvery(it) }
                    DayCircles({ it == weekday }) { setWeekday(it) }
                }
                Repeat.EVERY_N_DAYS -> NumberRow("Every", everyDays, "days") { setEvery(it) }
                else -> Text("Rings on the last day of every month.", color = Muted, fontSize = 13.sp)
            }
        }
    }
    val t = fmtTime(time)
    val n = everyDays.toIntOrNull()?.coerceIn(1, 365) ?: 1
    val probe = Reminder(
        id = 0, type = type, title = "", repeat = repeat, everyDays = n,
        anchorDay = date.dayOfMonth, nth = nth, weekday = weekday, weekMask = weekMask,
    )
    val firstOn = if (repeat.isCustom && repeat != Repeat.EVERY_N_DAYS && !(repeat == Repeat.WEEKDAYS && weekMask == 0)) {
        val f = Rules.firstOnOrAfter(probe, LocalDateTime.of(date, time))
        if (f.toLocalDate() != date) " · first on ${fmtDate(f.toLocalDate())}" else ""
    } else ""
    val note = when (repeat) {
        Repeat.NONE -> "Once, on ${fmtDate(date)} at $t"
        Repeat.DAILY -> "Every day at $t"
        Repeat.WEEKLY -> "Every ${date.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.ENGLISH)} at $t"
        Repeat.MONTHLY -> "On the ${ordinal(date.dayOfMonth)} of every month at $t" +
            (if (date.dayOfMonth > 28) ". In shorter months, on the last day." else "")
        Repeat.YEARLY -> "Every year on ${date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))} at $t"
        Repeat.EVERY_N_DAYS -> "Every $n days from ${fmtShortDate(fromLdt(date.atTime(time)))} at $t"
        Repeat.NTH_WEEKDAY -> "On the ${Rules.nthLabel(nth)} ${Rules.dayFull(weekday)} of every month at $t"
        Repeat.WEEKDAYS -> "Every ${Rules.maskLabel(weekMask).let { if (it == "Every day") "day" else it }} at $t"
        Repeat.EVERY_N_WEEKS -> "Every $n weeks on ${Rules.dayFull(weekday)} at $t"
        Repeat.LAST_DAY -> "On the last day of every month at $t"
    } + firstOn
    val trails = setFromDone != null && repeat.canTrail && type != Type.BILL
    Text(
        note + if (trails && fromDone) ", counted from the day it's done" else "", color = BlueText, fontSize = 13.sp,
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(BlueBg).padding(horizontal = 12.dp, vertical = 10.dp),
    )
    if (trails) {
        val gap = when (repeat) {
            Repeat.DAILY -> "a day"
            Repeat.WEEKLY -> "a week"
            else -> "$n days"
        }
        Row(
            Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
                .clickable { setFromDone!!(!fromDone) }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Count from when it's done", color = TextC, fontSize = 15.sp)
                Text(
                    if (fromDone) "Done late or early? The next one is $gap after you tick it"
                    else "Off: stays on its dates even if done late",
                    color = Muted, fontSize = 12.sp,
                )
            }
            Switch(
                checked = fromDone, onCheckedChange = { setFromDone!!(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnAccent, checkedTrackColor = Blue,
                    uncheckedThumbColor = Muted, uncheckedTrackColor = CardBg, uncheckedBorderColor = Line,
                ),
            )
        }
    }
}

fun paidLabel(p: Paid): String {
    val late = daysLate(p)
    return when {
        late <= 0 -> "on time"
        late == 1L -> "1 day late"
        else -> "$late days late"
    }
}

@Composable
fun PaidRow(p: Paid) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Check, null, tint = Green, modifier = Modifier.size(18.dp))
        Text("Due ${fmtShortDate(p.dueAt)}", Modifier.weight(1f), color = TextC, fontSize = 15.sp)
        Text(
            "Paid ${fmtShortDate(p.paidAt)} · ${paidLabel(p)}",
            color = if (daysLate(p) > 0) Amber else Muted, fontSize = 12.sp,
        )
    }
    HLine()
}

// ---------- paid history for one bill ----------

@Composable
fun PaidScreen(id: Long, onClose: () -> Unit) {
    val all by Store.reminders.collectAsState()
    val r = all.find { it.id == id }
    val history = r?.history ?: emptyList()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose)
        BigTitle(r?.title ?: "Paid", 34)
        Text(
            "${history.size} payment${if (history.size == 1) "" else "s"}", color = Muted, fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        if (history.isEmpty()) Text("Payments show up here once you mark a cycle as paid.", color = Muted)
        history.forEach { PaidRow(it) }
        Spacer(Modifier.height(40.dp))
    }
}

// ---------- check-later digest ----------

@Composable
fun DigestScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val all by Store.reminders.collectAsState()
    val checks = all.filter { it.type == Type.CHECK }.sortedByDescending { Digest.dueToday(it) }
    fun snooze(mins: Int) {
        Store.digestSnoozeUntil = System.currentTimeMillis() + mins * 60_000L
        Notifs.cancelDigest(ctx)
        Scheduler.scheduleDigest(ctx)
        Toast.makeText(ctx, "Reminding you in ${if (mins < 60) "$mins min" else "${mins / 60} h"}", Toast.LENGTH_SHORT).show()
        onClose()
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose)
        BigTitle("Check later", 34)
        Text(
            "TODAY'S DIGEST · ${fmtTime(LocalTime.of(Store.digestHour, Store.digestMinute))}",
            fontFamily = Mono, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        if (checks.isEmpty()) Text("All checked.", color = Muted)
        LazyColumn(Modifier.weight(1f)) {
            items(checks, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (Digest.dueToday(r)) Blue else Faint))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(r.title, fontSize = 15.sp, color = TextC)
                        Text(Digest.describe(r), color = Muted, fontSize = 12.sp)
                        if (r.notes.isNotBlank()) Text(r.notes, color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { Store.logDone(r); Store.delete(r.id) }) {
                        Icon(Icons.Outlined.Check, "Done, remove", tint = Green)
                    }
                }
                HLine()
            }
        }
        Text(
            "Remind me about all of these in", color = Muted, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 8.dp),
        )
        SnoozeRow { snooze(it) }
        Text(
            "Tomorrow", color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .clickable {
                    Store.digestSnoozeUntil = 0
                    Notifs.cancelDigest(ctx)
                    Scheduler.scheduleDigest(ctx)
                    onClose()
                }.padding(14.dp),
        )
    }
}

// ---------- settings (name, paid cycles, setup) ----------

@Composable
fun SettingsScreen(tick: Int, open: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val all by Store.reminders.collectAsState()
    val pkg = Uri.parse("package:${ctx.packageName}")
    var refresh by remember { mutableIntStateOf(0) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    fun openSettings(action: String, withPkg: Boolean = true) {
        try {
            ctx.startActivity(if (withPkg) Intent(action, pkg) else Intent(action))
        } catch (_: Exception) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
        }
    }
    val key = tick + refresh
    var digestTime by remember { mutableStateOf(LocalTime.of(Store.digestHour, Store.digestMinute)) }
    var name by remember { mutableStateOf(Store.userName) }
    var editingName by remember { mutableStateOf(false) }
    val bills = all.filter { it.type == Type.BILL && !it.isTest }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigTitle("Settings", 44, Modifier.weight(1f))
            FeedbackButton()
        }

        Caps("You")
        SetRow("Your name", value = name, chevron = true, onClick = { editingName = true })

        Caps("History")
        SetRow(
            "History and activity", sub = "What got done, and who changed what",
            chevron = true, onClick = { open(Screen.History) },
        )

        Caps("People")
        val people by Cloud.people.collectAsState()
        var removing by remember { mutableStateOf<Person?>(null) }
        people.forEach { p ->
            SetRow(p.name, sub = "Connected · can share lists", onClick = { removing = p }, trailing = { Avatar(p.name) })
        }
        SetRow("Add a person", titleColor = Blue, chevron = true, onClick = { open(Screen.AddPerson) })
        removing?.let { p ->
            AlertDialog(
                onDismissRequest = { removing = null },
                containerColor = SheetBg,
                title = { Text("Remove ${p.name}?") },
                text = { Text("They'll be taken off any lists you share with them.", color = Muted) },
                confirmButton = {
                    TextButton(onClick = { Cloud.removePerson(p); removing = null }) { Text("Remove", color = Red) }
                },
                dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel", color = Muted) } },
            )
        }

        Caps("Paid cycles")
        if (bills.isEmpty()) {
            Text("Bills you add show up here with their payments.", color = Muted, fontSize = 14.sp)
        }
        bills.forEach { b ->
            SetRow(
                b.title,
                sub = b.history.firstOrNull()?.let { "Last paid ${fmtShortDate(it.paidAt)} · ${paidLabel(it)}" } ?: "Not paid yet",
                value = b.history.size.toString(),
                chevron = true,
                onClick = { open(Screen.Paid(b.id)) },
            )
        }

        Caps("Permissions")
        val missing = remember(tick) { Perms.missingCount(ctx) }
        SetRow(
            "All permissions",
            sub = if (missing == 0) "All allowed" else "$missing need${if (missing == 1) "s" else ""} attention",
            chevron = true,
            onClick = { open(Screen.Permissions) },
        )
        if (missing > 0) Text(
            "Alarms may not open full screen until these are allowed.",
            color = Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
        )

        Caps("Alarms")
        SetRow("Check-later digest", value = fmtTime(digestTime), chevron = true, onClick = {
            TimePickerDialog(
                ctx, android.R.style.Theme_Material_Dialog,
                { _, h, m ->
                    Store.digestHour = h; Store.digestMinute = m
                    digestTime = LocalTime.of(h, m)
                    Scheduler.scheduleDigest(ctx)
                },
                digestTime.hour, digestTime.minute, false,
            ).show()
        })
        SetRow("If not answered", value = "5 min, then every 30 min")
        TalkToAlarmsRow()
        VoiceRow()
        CelebrateRow()
        SetRow("Alarm sound", value = "Phone default", chevron = true, onClick = {
            openSettings(Settings.ACTION_SOUND_SETTINGS, withPkg = false)
        })
        SetRow("Test alarm in 10 seconds", titleColor = Blue, onClick = {
            val r = Reminder(
                id = Store.newId(), type = Type.BILL, title = "Test alarm",
                dueAt = System.currentTimeMillis() + 10_000L, isTest = true,
            )
            Store.upsert(r)
            Scheduler.schedule(ctx, r)
            Toast.makeText(ctx, "Lock the phone or open another app. Ringing in 10 s.", Toast.LENGTH_LONG).show()
        })
        BackupSection()

        Caps("App")
        SetRow("Feedback", sub = "What you've both logged", chevron = true, onClick = { open(Screen.Feedback) })
        var checking by remember { mutableStateOf(false) }
        SetRow(
            "Check for updates", sub = "Version ${Updater.currentName(ctx)}",
            value = if (checking) "Checking…" else null, titleColor = Blue,
            onClick = {
                checking = true
                Updater.check(ctx, force = true) { found ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        checking = false
                        if (!found) Toast.makeText(ctx, "You're on the latest version", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    if (editingName) {
        var draft by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            containerColor = SheetBg,
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(30) }, singleLine = true,
                    shape = FieldShape, colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) {
                        Store.userName = draft.trim(); name = draft.trim(); Cloud.updateName()
                    }
                    editingName = false
                }) { Text("Save", color = Blue) }
            },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel", color = Muted) } },
        )
    }
}

@Composable
fun PermissionsScreen(tick: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val pkg = Uri.parse("package:${ctx.packageName}")
    var refresh by remember { mutableIntStateOf(0) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    fun openSettings(action: String) {
        try {
            ctx.startActivity(Intent(action, pkg))
        } catch (_: Exception) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
        }
    }
    val key = tick + refresh
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose)
        BigTitle("Permissions", 34)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow these once so bills and errands always open full screen and ring on time.",
            color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp),
        )
        PermRow("Notifications", remember(key) { Perms.notifications(ctx) }) {
            if (Build.VERSION.SDK_INT >= 33 && !Store.notifAsked) {
                Store.notifAsked = true
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ctx.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                )
            }
        }
        PermRow("Alarms and reminders", remember(key) { Perms.exact(ctx) }) {
            openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
        PermRow("Display over other apps", remember(key) { Perms.overlay(ctx) }) {
            openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            PermRow("Full screen notifications", remember(key) { Perms.fullScreen(ctx) }) {
                openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
        }
        PermRow("Ignore battery optimisation", remember(key) { Perms.battery(ctx) }) {
            openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }
        Text(
            "If a switch is greyed out: App info → ⋮ (top right) → Allow restricted settings, then try again.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun PermRow(label: String, granted: Boolean, onClick: () -> Unit) {
    SetRow(label, onClick = onClick, trailing = {
        if (granted) {
            Icon(Icons.Outlined.CheckCircle, "Allowed", tint = Green, modifier = Modifier.size(20.dp))
        } else {
            Text(
                "Allow", color = BlueText, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BlueBg)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    })
}

// ---------- list editor (used in view and edit) ----------

@Composable
fun ItemsEditor(items: SnapshotStateList<Item>, title: String, showLabel: Boolean, optional: Boolean = false, onChange: () -> Unit) {
    var newItem by remember { mutableStateOf("") }
    var editing by remember { mutableIntStateOf(-1) }
    var editText by remember { mutableStateOf("") }
    var showDone by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }

    fun add(name: String = newItem) {
        val n = name.trim()
        newItem = ""
        if (n.isEmpty()) return
        Store.rememberItem(n)
        val i = items.indexOfFirst { it.name.equals(n, ignoreCase = true) }
        if (i >= 0 && !items[i].done) return // already on the list
        if (i >= 0) items.removeAt(i) // was marked done: bring it back
        items.add(0, Item(n, by = Store.userName, byUid = Cloud.uid)); editing = -1; onChange()
    }

    if (showLabel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caps(if (optional && items.isEmpty()) "List (optional)" else "Items to get", Modifier.weight(1f))
            if (items.isNotEmpty()) IconButton(onClick = { share = true }, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Outlined.Share, "Share list", tint = Blue, modifier = Modifier.size(20.dp))
            }
        }
    } else {
        Spacer(Modifier.height(20.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newItem, onValueChange = { newItem = it },
            placeholder = { Text("Add an item") }, singleLine = true,
            shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(Blue).clickable { add() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Add, "Add item", tint = OnAccent) }
    }
    val suggestions = remember(newItem, items.toList()) {
        Store.itemSuggestions(newItem, items.filter { !it.done }.map { it.name })
    }
    if (suggestions.isNotEmpty()) {
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { name -> Pill(name, true) { add(name) } }
        }
    }
    Spacer(Modifier.height(8.dp))

    val pending = items.withIndex().filter { !it.value.done }
    val done = items.withIndex().filter { it.value.done }

    val rowFor: @Composable (Int, Item) -> Unit = { i, item ->
        ItemRow(
            item = item,
            isEditing = editing == i,
            editText = editText,
            onEditText = { editText = it },
            onStartEdit = { editing = i; editText = item.name },
            onSaveEdit = {
                if (editText.isNotBlank()) {
                    items[i] = item.copy(name = editText.trim()); Store.rememberItem(editText); onChange()
                }
                editing = -1
            },
            onCancelEdit = { editing = -1 },
            onToggle = { items[i] = item.toggled(); editing = -1; onChange() },
            onDelete = { items.removeAt(i); editing = -1; onChange() },
        )
    }

    // Hold a row, then drag it up or down. Ticked items stay put at the bottom.
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var moved by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    /** Swap with the next unticked item above (-1) or below (1). */
    fun moveBy(id: String, dir: Int): Boolean {
        val cur = items.indexOfFirst { it.id == id }
        if (cur < 0) return false
        var j = cur + dir
        while (j in items.indices && items[j].done) j += dir
        if (j !in items.indices) return false
        val a = items[cur]; items[cur] = items[j]; items[j] = a
        return true
    }

    /** Shared lists sort by ts (newest first), so give the new order matching times. */
    fun dropped() {
        dragId = null; dragY = 0f
        if (!moved) return
        moved = false
        val ts = items.map { it.ts }.sortedDescending().toMutableList()
        for (k in 1 until ts.size) if (ts[k] >= ts[k - 1]) ts[k] = ts[k - 1] - 1
        for (k in items.indices) if (items[k].ts != ts[k]) items[k] = items[k].copy(ts = ts[k])
        onChange()
    }

    pending.forEach { (i, item) ->
        key(item.id) {
            val lifted = dragId == item.id
            Box(
                Modifier
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer { translationY = if (lifted) dragY else 0f }
                    .then(
                        if (lifted) Modifier.clip(RoundedCornerShape(12.dp)).background(CardBg)
                            .border(1.dp, Blue, RoundedCornerShape(12.dp))
                        else Modifier,
                    )
                    .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragId = item.id; dragY = 0f; editing = -1
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragY += amount.y
                                val h = size.height.toFloat()
                                if (dragY > h / 2 && moveBy(item.id, 1)) {
                                    dragY -= h; moved = true
                                } else if (dragY < -h / 2 && moveBy(item.id, -1)) {
                                    dragY += h; moved = true
                                }
                            },
                            onDragEnd = { dropped() },
                            onDragCancel = { dropped() },
                        )
                    },
            ) { rowFor(i, item) }
        }
    }
    if (pending.isEmpty() && done.isEmpty()) {
        Text("No items yet.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    }

    if (done.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().clickable { showDone = !showDone }.padding(horizontal = 2.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = Green, modifier = Modifier.size(20.dp))
            Text("Marked done (${done.size})", Modifier.weight(1f), color = Muted)
            Icon(if (showDone) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = Muted)
        }
        if (showDone) {
            done.forEach { (i, item) -> rowFor(i, item) }
            TextButton(onClick = { items.removeAll { it.done }; editing = -1; onChange() }) {
                Text("Delete all done items", color = Red)
            }
        }
    }
    if (share) ShareSheet(title, items.toList()) { share = false }
}

@Composable
fun ItemRow(
    item: Item,
    isEditing: Boolean,
    editText: String,
    onEditText: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (isEditing) {
            OutlinedTextField(
                value = editText, onValueChange = onEditText, singleLine = true,
                shape = FieldShape, colors = fieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSaveEdit() }),
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            )
            IconButton(onClick = onSaveEdit) { Icon(Icons.Outlined.Check, "Save", tint = Green) }
            IconButton(onClick = onCancelEdit) { Icon(Icons.Outlined.Close, "Cancel", tint = Muted) }
        } else {
            Checkbox(
                item.done, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Color(0xFF4A4A50), checkmarkColor = OnAccent),
            )
            Column(Modifier.weight(1f).clickable(onClick = onStartEdit).padding(vertical = 10.dp)) {
                Text(item.name, color = if (item.done) Muted else TextC, fontSize = 15.sp)
                val me = Store.userName
                val tag = when {
                    item.done && item.doneBy.isNotBlank() && item.doneBy != me -> "${item.doneBy} got it"
                    !item.done && item.byUid.isNotBlank() && item.byUid != Cloud.uid && item.by.isNotBlank() -> "${item.by} added"
                    else -> null
                }
                if (tag != null) Text(tag, color = BlueText, fontSize = 11.sp)
            }
            IconButton(onClick = onStartEdit) { Icon(Icons.Outlined.Edit, "Edit", tint = Faint, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Remove", tint = Faint, modifier = Modifier.size(18.dp)) }
        }
    }
    HLine()
}

// ---------- review an assigned errand ----------

/**
 * Opened from a notification the request may not have arrived from the cloud yet:
 * wait for it. If it was already accepted, show the errand instead.
 */
@Composable
fun ReviewRoute(taskId: String?, localId: Long?, back: () -> Unit, go: (Screen) -> Unit) {
    if (taskId == null) {
        ReviewScreen(null, localId, back)
        return
    }
    val reqs by Cloud.requests.collectAsState()
    val all by Store.reminders.collectAsState()
    var gaveUp by remember(taskId) { mutableStateOf(false) }
    LaunchedEffect(taskId) {
        kotlinx.coroutines.delay(12_000)
        gaveUp = true
    }
    val local = all.find { it.taskId == taskId }
    when {
        reqs.any { it.id == taskId } -> ReviewScreen(taskId, localId, back)
        local != null -> LaunchedEffect(local.id) { go(Screen.View(local.id)) }
        gaveUp -> LaunchedEffect(Unit) { back() }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading request…", color = Muted, fontSize = 15.sp)
        }
    }
}

@Composable
fun ReviewScreen(taskId: String?, localId: Long?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val reqs by Cloud.requests.collectAsState()
    val req = remember(taskId) { taskId?.let { id -> reqs.find { it.id == id } } }
    val base = remember(taskId, localId) { req?.toReminder() ?: localId?.let { Store.get(it) } }
    if (base == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    if (req != null && req.both) {
        BothReview(req, onClose)
        return
    }
    val from = base.taskPerson
    val initDt = remember {
        val d = toLdt(base.nextTrigger)
        if (d.isAfter(LocalDateTime.now())) d else LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
    var date by remember { mutableStateOf(initDt.toLocalDate()) }
    var time by remember { mutableStateOf(initDt.toLocalTime()) }
    var repeat by remember { mutableStateOf(base.repeat) }
    var everyDays by remember { mutableStateOf(base.everyDays.toString()) }
    var nth by remember { mutableIntStateOf(base.nth) }
    var weekday by remember { mutableIntStateOf(base.weekday) }
    var weekMask by remember { mutableIntStateOf(if (base.weekMask == 0) 1 shl (initDt.dayOfWeek.value - 1) else base.weekMask) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun build(): Reminder? {
        val n = everyDays.toIntOrNull()?.coerceIn(1, 365) ?: 1
        if (repeat == Repeat.WEEKDAYS && weekMask == 0) {
            error = "Pick at least one day"; return null
        }
        val rule = base.copy(repeat = repeat, everyDays = n, anchorDay = date.dayOfMonth, nth = nth, weekday = weekday, weekMask = weekMask)
        var first = Rules.firstOnOrAfter(rule, LocalDateTime.of(date, time))
        if (repeat.isCustom) {
            val nowL = LocalDateTime.now()
            var g = 0
            while (!first.isAfter(nowL) && g++ < 2000) first = Rules.step(rule, first)
        }
        val ms = fromLdt(first)
        if (ms <= System.currentTimeMillis()) {
            error = "Pick a time in the future"; return null
        }
        return rule.copy(dueAt = ms, snoozeUntil = null, rings = 0, completed = false)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.Outlined.Close, "Close", onClose)
        Text(if (req != null) "Request from $from" else "Your reminder", color = Muted, fontSize = 13.sp)
        BigTitle(base.title, 30, Modifier.padding(top = 2.dp))
        if (base.notes.isNotBlank()) Text(base.notes, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))

        Caps("Due by")
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(FieldShape).background(Color(0xFF141416)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(base.dueByDate?.let { fmtDate(it) } ?: "No due date", color = TextC, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Lock, null, tint = Faint, modifier = Modifier.size(14.dp))
            Text(" set by $from", color = Muted, fontSize = 12.sp)
        }

        Caps("Remind me at")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldButton(fmtDate(date), Modifier.weight(1f)) {
                DatePickerDialog(
                    ctx, android.R.style.Theme_Material_Dialog,
                    { _, y, m, d -> date = LocalDate.of(y, m + 1, d); error = null },
                    date.year, date.monthValue - 1, date.dayOfMonth,
                ).show()
            }
            FieldButton(fmtTime(time), Modifier.width(124.dp), mono = true) {
                TimePickerDialog(
                    ctx, android.R.style.Theme_Material_Dialog,
                    { _, h, mi -> time = LocalTime.of(h, mi); error = null },
                    time.hour, time.minute, false,
                ).show()
            }
        }
        if (req != null) {
            Text(
                "$from picked ${fmtWhen(req.remindAt)}. Changing it sends it back to $from for an OK.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        RepeatPicker(
            Type.ERRAND, date, time, repeat, { repeat = it; error = null }, everyDays, { everyDays = it },
            nth, { nth = it }, weekday, { weekday = it }, weekMask, { weekMask = it; error = null },
            label = "Repeat",
        )
        if (base.items.isNotEmpty()) {
            Caps("List")
            Text(
                base.items.filter { !it.done }.joinToString(", ") { it.name }.ifBlank { "Everything got" },
                color = TextC, fontSize = 14.sp,
            )
        }
        error?.let { Text(it, color = Red, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(24.dp))
        val changed = req != null && (
            LocalDateTime.of(date, time) != toLdt(req.remindAt).withSecond(0).withNano(0) || repeat != req.repeat ||
                (repeat in setOf(Repeat.EVERY_N_DAYS, Repeat.EVERY_N_WEEKS) && everyDays != req.everyDays.toString()) ||
                (repeat in setOf(Repeat.NTH_WEEKDAY, Repeat.EVERY_N_WEEKS) && weekday != req.weekday) ||
                (repeat == Repeat.NTH_WEEKDAY && nth != req.nth) ||
                (repeat == Repeat.WEEKDAYS && weekMask != req.weekMask)
            )
        if (req != null && changed) {
            BigButton(if (busy) "Sending…" else "Send changes to $from", Blue, OnAccent) {
                if (busy) return@BigButton
                val mine = build() ?: return@BigButton
                busy = true
                Cloud.counterOffer(req, mine) { err ->
                    busy = false
                    if (err != null) error = err
                    else {
                        Toast.makeText(ctx, "Sent to $from for an OK", Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SoftButton("Decline", CardBg, TextC, Modifier.fillMaxWidth()) { Cloud.declineTask(req); onClose() }
        } else if (req != null) {
            BigButton(if (busy) "Accepting…" else "Accept", Blue, OnAccent) {
                if (busy) return@BigButton
                val mine = build() ?: return@BigButton
                busy = true
                Cloud.acceptTask(req, mine) { err ->
                    busy = false
                    if (err != null) error = err
                    else {
                        Toast.makeText(ctx, "Added. Reminder ${fmtWhen(mine.dueAt)}", Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SoftButton("Decline", CardBg, TextC, Modifier.fillMaxWidth()) { Cloud.declineTask(req); onClose() }
        } else {
            BigButton("Save", Blue, OnAccent) {
                val mine = build() ?: return@BigButton
                Store.upsertQuiet(mine)
                Scheduler.schedule(ctx, mine)
                Cloud.updateMyReminder(mine)
                onClose()
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun BothReview(req: TaskRequest, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val me = Store.userName.ifBlank { "Me" }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.Outlined.Close, "Close", onClose)
        Text("Shared alarm from ${req.fromName}", color = Muted, fontSize = 13.sp)
        BigTitle(req.title, 30, Modifier.padding(top = 2.dp))
        Text("$me + ${req.fromName}", color = BlueText, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
        if (req.notes.isNotBlank()) Text(req.notes, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
        Caps("Rings")
        FieldButton(fmtWhen(req.remindAt) + " · " + repeatShort(req.toReminder(0)), Modifier.fillMaxWidth(), mono = false) {}
        if (req.items.isNotEmpty()) {
            Caps("List")
            Text(req.items.filter { !it.done }.joinToString(", ") { it.name }.ifBlank { "Everything got" }, color = TextC, fontSize = 14.sp)
        }
        Text(
            "It rings on both phones. Snoozing or finishing it only changes yours. After you accept, either of you can change the title, time or repeat without asking again.",
            color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp),
        )
        error?.let { Text(it, color = Red, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(24.dp))
        BigButton(if (busy) "Accepting…" else "Accept", Blue, OnAccent) {
            if (busy) return@BigButton
            busy = true
            Cloud.acceptBoth(req) { err ->
                busy = false
                if (err != null) error = err
                else {
                    Toast.makeText(ctx, "Added. Rings ${fmtWhen(req.remindAt)}", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SoftButton("Decline", CardBg, TextC, Modifier.fillMaxWidth()) { Cloud.declineTask(req); onClose() }
        Spacer(Modifier.height(40.dp))
    }
}

// ---------- suggested changes ----------

@Composable
fun ChangesScreen(taskId: String, onClose: () -> Unit, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val props by Cloud.proposals.collectAsState()
    val all by Store.reminders.collectAsState()
    val r = all.find { it.taskId == taskId }
    val p = props[taskId]
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if (r == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    if (p == null || p.byUid == Cloud.uid) {
        if (!busy) LaunchedEffect(Unit) { go(Screen.View(r.id)) }
        return
    }
    val diff = proposalDiff(r, p)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.Outlined.Close, "Close", onClose)
        Text(
            if (r.taskStatus == "pending") "${p.byName} will do it, with changes" else "Changes from ${p.byName}",
            color = Muted, fontSize = 13.sp,
        )
        BigTitle(r.title, 30, Modifier.padding(top = 2.dp))
        diff.forEach { (label, old, new) ->
            Caps(label)
            Text(
                diffText(old, new), fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(FieldShape).background(CardBg)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            )
        }
        Text(
            if (diff.isEmpty()) "Nothing actually changed." else "Nothing else changed.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
        )
        error?.let { Text(it, color = Red, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(24.dp))
        BigButton(if (busy) "Accepting…" else "Accept changes", Blue, OnAccent) {
            if (busy) return@BigButton
            busy = true
            Cloud.acceptProposal(taskId) { err ->
                busy = false
                if (err != null) error = err
                else {
                    Toast.makeText(ctx, "Changes accepted", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SoftButton("Change and send back", CardBg, TextC, Modifier.fillMaxWidth()) { go(Screen.Edit(r.id, sendBack = true)) }
        Spacer(Modifier.height(8.dp))
        SoftButton("Decline", Color.Transparent, Red, Modifier.fillMaxWidth()) {
            Cloud.declineProposal(taskId); onClose()
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ---------- history ----------

private data class HistRow(val ts: Long, val title: String, val text: String, val done: Boolean, val bill: Boolean, val alarm: Boolean = false)

private val histDayFmt = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)

@Composable
fun HistoryScreen(onClose: () -> Unit) {
    val all by Store.reminders.collectAsState()
    val events by Store.events.collectAsState()
    val logs by Cloud.logs.collectAsState()
    var filter by remember { mutableStateOf("All") }
    val rows = remember(all, events, logs) {
        val out = mutableListOf<HistRow>()
        all.filter { !it.isTest && it.type == Type.BILL }.forEach { b ->
            b.history.forEach { out += HistRow(it.paidAt, b.title, "Paid · ${paidLabel(it)}", done = true, bill = true) }
        }
        events.forEach { out += HistRow(it.ts, it.title, it.text, it.done, it.bill, it.alarm) }
        logs.forEach { (key, entries) ->
            val r = all.find { it.sharedId == key || it.taskId == key } ?: return@forEach
            entries.forEach { e ->
                val me = e.uid == Cloud.uid
                val who = if (me) "You" else e.who
                out += when {
                    e.text.startsWith("got ") ->
                        HistRow(e.ts, e.text.removePrefix("got "), "${r.title} · ticked by ${if (me) "you" else e.who}", true, false)
                    e.text.startsWith("did it") -> HistRow(
                        e.ts, r.title,
                        (if (me) "Done · you" else "Done by ${e.who}") + if (r.assignedByMe || r.isBoth) "" else if (r.taskId != null) " · asked by ${r.taskPerson}" else "",
                        true, false,
                    )
                    else -> HistRow(e.ts, r.title, "$who ${e.text}", false, false)
                }
            }
        }
        out.filter { it.ts > 0 }.sortedByDescending { it.ts }.take(400)
    }
    val shown = rows.filter {
        when (filter) {
            "Done" -> it.done
            "Changes" -> !it.done && !it.alarm
            "Alarms" -> it.alarm
            "Bills" -> it.bill
            else -> true
        }
    }
    val today = LocalDate.now()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
    ) {
        item {
            TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose)
            BigTitle("History", 34, Modifier.padding(top = 4.dp))
            Text("EVERYTHING YOU'VE BOTH DONE", fontFamily = Mono, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
            Row(
                Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("All", "Alarms", "Done", "Changes", "Bills").forEach { f -> Pill(f, filter == f) { filter = f } }
            }
            if (shown.isEmpty()) Text(
                "Nothing here yet. Things you finish and change show up here.",
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp),
            )
        }
        shown.groupBy { toLdt(it.ts).toLocalDate() }.forEach { (day, list) ->
            item(key = "d_$day") {
                val label = when (day) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    else -> null
                }
                Caps(listOfNotNull(label, day.format(histDayFmt)).joinToString(" · "))
            }
            list.forEachIndexed { i, h ->
                item(key = "h_${day}_${i}_${h.ts}") {
                    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(fmtTime(h.ts), color = Muted, fontSize = 13.sp, modifier = Modifier.width(58.dp).padding(top = 14.dp))
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(CardBg)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(h.title, fontSize = 16.sp, color = TextC, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(h.text, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------- search ----------

private fun highlight(text: String, q: String) = buildAnnotatedString {
    val i = if (q.isBlank()) -1 else text.indexOf(q, ignoreCase = true)
    if (i < 0) {
        append(text); return@buildAnnotatedString
    }
    append(text.substring(0, i))
    withStyle(SpanStyle(color = BlueText)) { append(text.substring(i, i + q.length)) }
    append(text.substring(i + q.length))
}

@Composable
fun SearchScreen(onClose: () -> Unit, open: (Screen) -> Unit) {
    val all by Store.reminders.collectAsState()
    var q by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val query = q.trim()
    val pool = all.filter { !it.isTest && (filter == null || it.type.name == filter) }
    val hits = if (query.isEmpty()) emptyList() else pool.filter {
        it.title.contains(query, true) || it.notes.contains(query, true) ||
            it.taskPerson.contains(query, true) || it.sharedBy.contains(query, true)
    }.sortedWith(compareBy({ it.completed }, { !it.title.contains(query, true) }, { it.nextTrigger }))
    val noteHits = if (filter == null) searchNotes(query) else emptyList()
    val itemHits = if (query.isEmpty()) emptyList() else pool.flatMap { r ->
        r.items.filter { it.name.contains(query, true) }.map { r to it }
    }.sortedWith(compareBy({ it.second.done }, { !it.second.name.startsWith(query, true) }))

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text("Search reminders and lists") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Outlined.Close, "Clear", tint = Muted) } },
                shape = FieldShape, colors = fieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            Text(
                "Cancel", color = Muted, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClose).padding(8.dp),
            )
        }
        FlowRow(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(null to "All", "BILL" to "Bills", "ERRAND" to "Errands", "CHECK" to "Check later")
                .forEach { (f, l) -> Pill(l, filter == f) { filter = f } }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (query.isEmpty()) item {
                Text(
                    "Search titles, notes and list items, including shared lists.",
                    color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp),
                )
            } else if (hits.isEmpty() && itemHits.isEmpty() && noteHits.isEmpty()) item {
                Text("Nothing matches \"$query\".", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            }
            if (itemHits.isNotEmpty()) {
                item { Caps("In lists") }
                items(itemHits.take(40), key = { "i_" + it.first.id + it.second.id }) { (r, item) ->
                    TaskCard(onClick = { open(Screen.View(r.id)) }) {
                        Text(highlight(item.name, query), fontSize = 16.sp, color = if (item.done) Muted else TextC)
                        Text(
                            r.title + " · " + if (item.done) "got it" else "not got yet",
                            color = Muted, fontSize = 13.sp,
                        )
                    }
                }
            }
            if (hits.isNotEmpty()) {
                item { Caps("Reminders") }
                items(hits.take(40), key = { "r_" + it.id }) { r ->
                    TaskCard(onClick = { open(Screen.View(r.id)) }) {
                        Text(highlight(r.title, query), fontSize = 16.sp, color = if (r.completed) Muted else TextC)
                        val kind = when (r.type) {
                            Type.BILL -> "Bill"
                            Type.ERRAND -> "Errand"
                            Type.CHECK -> "Check later"
                        }
                        Text(kind + " · " + whenLine(r), color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (r.notes.contains(query, true) && !r.title.contains(query, true)) {
                            Text(highlight(r.notes, query), color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (noteHits.isNotEmpty()) {
                item { Caps("Notes") }
                items(noteHits.take(40), key = { "n_" + it.id }) { n ->
                    TaskCard(onClick = { open(Screen.NoteView(n.id)) }) {
                        Text(highlight(n.title.ifBlank { "Untitled" }, query), fontSize = 16.sp, color = TextC)
                        Text(
                            highlight(n.text.replace('\n', ' '), query), color = Muted, fontSize = 13.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Turns shared text into a title and notes. A subject (Chrome's page title) becomes the title;
 * otherwise the first line, cut at a word to keep it short. Anything more goes in the notes.
 */
fun shareToTitle(subject: String, text: String): Pair<String, String> {
    if (subject.isNotBlank()) return subject.take(80) to text.takeIf { it != subject }.orEmpty()
    val first = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    val title = if (first.length <= 60) first else first.take(60).substringBeforeLast(' ').ifBlank { first.take(60) } + "…"
    return title to if (title == text) "" else text
}
