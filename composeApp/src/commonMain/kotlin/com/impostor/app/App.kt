package com.impostor.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.impostor.data.syncedCatalogRepository
import com.impostor.data.analyticsService
import com.impostor.data.SubscriptionRepositoryImpl
import com.impostor.data.TrialRepositoryImpl
import com.impostor.domain.AnalyticsEvent
import com.impostor.domain.canStartGame
import com.impostor.domain.requiresTrialGame
import com.impostor.domain.GameSession
import com.impostor.domain.GameSetup
import com.impostor.domain.Player
import com.impostor.domain.RandomSource
import com.impostor.domain.Role
import com.impostor.domain.SetupValidation
import com.impostor.domain.StartGameUseCase
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionRepository
import com.impostor.domain.SubscriptionState
import com.impostor.domain.PurchasePremiumUseCase
import com.impostor.domain.RestorePurchasesUseCase
import com.impostor.domain.RecordStartedGameUseCase
import com.impostor.domain.TrialState
import com.impostor.domain.ValidateGameSetupUseCase
import com.impostor.domain.unlocksAllCategories
import com.impostor.domain.revealText
import com.impostor.domain.normalizePlayerName
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.painterResource
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import impostor.composeapp.generated.resources.*

private val Ink = Color(0xFF050914)
private val Navy = Color(0xFF162846)
private val Cyan = Color(0xFF00F0FF)
private val Frost = Color(0xFFBDEBFF)
private fun glassOpacity(color: Color): Color = color.copy(alpha = color.alpha + (1f - color.alpha) * .3f)
private val PanelBorder = glassOpacity(Color(0xFF7FCBFF).copy(alpha = .44f))
private val PanelTop = glassOpacity(Color(0xFF5E9FE0).copy(alpha = .28f))
private val PanelBottom = glassOpacity(Color(0xFF0A1A43).copy(alpha = .72f))
private val BlackFridayAccent = Color(0xFFD8FF3F)
private val BlackFridaySurface = Color(0xFF111411)
private val BlackFridayMuted = Color(0xFFA6ABA1)
private const val HOLIDAY_BUTTON_TRANSITION_MILLIS = 650L
private const val HOLIDAY_REINDEER_FALLBACK_MILLIS = 5_500L
private val reviewProcessId = Random.nextLong().toString()
private const val PrivacyPolicyUrl = "https://jordanrafalm.github.io/impostor/privacy.html"
private const val TermsOfUseUrl = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
internal sealed interface Route {
    data object Splash : Route
    data object Home : Route
    data object Players : Route
    data object Categories : Route
    data object Premium : Route
    data class Reveal(val session: GameSession) : Route
    data class GameStart(val session: GameSession) : Route
}

internal fun saveRoute(route: Route): List<String> =
        when (route) {
            Route.Splash -> listOf("splash")
            Route.Home -> listOf("home")
            Route.Players -> listOf("players")
            Route.Categories -> listOf("categories")
            Route.Premium -> listOf("premium")
            is Route.Reveal -> listOf("reveal") + saveGameSession(route.session)
            is Route.GameStart -> listOf("gameStart") + saveGameSession(route.session)
        }

internal fun restoreRoute(saved: List<String>): Route? =
        when (saved.firstOrNull()) {
            "splash" -> Route.Splash
            "home" -> Route.Home
            "players" -> Route.Players
            "categories" -> Route.Categories
            "premium" -> Route.Premium
            "reveal" -> restoreGameSession(saved.drop(1))?.let(Route::Reveal)
            "gameStart" -> restoreGameSession(saved.drop(1))?.let(Route::GameStart)
            else -> null
        }

internal val routeSaver = listSaver<Route, String>(
    save = { saveRoute(it) },
    restore = ::restoreRoute,
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImpostorApp() {
    var route by rememberSaveable(stateSaver = routeSaver) { mutableStateOf<Route>(Route.Splash) }
    var launchSoundRequested by rememberSaveable { mutableStateOf(false) }
    val launchSoundPlayer = remember { platformLaunchSoundPlayer() }
    var holidaySequencePhase by remember { mutableStateOf(HomeHolidaySequencePhase.IDLE) }
    var showFreeLimitDialog by remember { mutableStateOf(false) }
    var showSexCategoryIntro by remember {
        val savedEligibility = readSexCategoryIntroEligibility()
        val eligibility = resolveSexCategoryIntroEligibility(
            storedEligibility = savedEligibility,
            detectedExistingInstallation = if (savedEligibility == null) isExistingInstallation() else false,
        )
        if (savedEligibility == null) writeSexCategoryIntroEligibility(eligibility)
        mutableStateOf(shouldShowSexCategoryIntro(eligibility, hasDismissedSexCategoryIntro()))
    }
    var sexIntroIsUpgradeNotice by remember { mutableStateOf(showSexCategoryIntro) }
    val reviewPreferences = remember { ReviewPromptPreferences(platformPreferenceStore()) }
    var showReviewPrompt by rememberSaveable { mutableStateOf(false) }
    var reviewCheckedProcessId by rememberSaveable { mutableStateOf("") }
    val reviewLaunchChecked = reviewCheckedProcessId == reviewProcessId
    var skipBlackFridayForReview by rememberSaveable { mutableStateOf(false) }
    var reviewOpenFailed by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(showReviewPrompt) {
        if (showReviewPrompt && reviewPreferences.hasOpenedStore()) showReviewPrompt = false
    }
    var players by rememberSaveable(stateSaver = playerListSaver) { mutableStateOf(loadPlayers()) }
    var selectedCategories by rememberSaveable(stateSaver = stringSetSaver) {
        mutableStateOf(readSelectedCategoryIds() ?: setOf("animals"))
    }
    var impostorCount by rememberSaveable { mutableStateOf(1) }
    var hintsEnabled by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable(stateSaver = setupValidationReasonSaver) {
        mutableStateOf<SetupValidation.Reason?>(null)
    }
    val repository = remember { syncedCatalogRepository() }
    var catalogRevision by remember { mutableStateOf(0) }
    LaunchedEffect(repository) {
        repository.refresh().onFailure {
            println("Prompt catalog refresh failed: ${it.message ?: "unknown error"}")
        }
        catalogRevision += 1
    }
    val subscriptionRepository = remember { SubscriptionRepositoryImpl() }
    val trialRepository = remember { TrialRepositoryImpl() }
    val scope = rememberCoroutineScope()
    var startingGame by remember { mutableStateOf(false) }
    val analytics = remember { analyticsService() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(subscriptionRepository) {
        subscriptionRepository.reconcileEntitlement()
    }
    DisposableEffect(lifecycle, subscriptionRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { subscriptionRepository.reconcileEntitlement() }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val entitlement by subscriptionRepository.entitlement.collectAsState(
        com.impostor.domain.Entitlement(
            state = SubscriptionState.LOCKED,
            source = com.impostor.domain.EntitlementSource.LOCAL_CACHE,
        ),
    )
    val trialState by trialRepository.state.collectAsState()
    var promotionDismissedThisLaunch by rememberSaveable { mutableStateOf(false) }
    var promotionActionInProgress by remember { mutableStateOf(false) }
    var reminderRequested by rememberSaveable { mutableStateOf(false) }
    val showBlackFriday = !trialState.blackFridayClaimed && !promotionDismissedThisLaunch
    val categories = remember { repository.categories() }
    val allCategoryIds = remember(categories) { categories.map { it.id }.toSet() }
    val prompts = remember(repository, catalogRevision) {
        repository.prompts(categories.map { it.id }.toSet())
    }
    val premiumUnlocked = entitlement.unlocksAllCategories()
    val unlockedCategories = if (trialState.canStartGame(entitlement)) {
        allCategoryIds
    } else {
        allCategoryIds.intersect(com.impostor.domain.AlwaysFreeCategoryIds)
    }

    LaunchedEffect(allCategoryIds) {
        val validSelection = selectedCategories.intersect(allCategoryIds)
        if (validSelection != selectedCategories) {
            selectedCategories = if (validSelection.isNotEmpty() || selectedCategories.isEmpty()) {
                validSelection
            } else {
                setOf("animals").intersect(allCategoryIds)
            }
        }
    }

    LaunchedEffect(selectedCategories, allCategoryIds) {
        writeSelectedCategoryIds(selectedCategories.intersect(allCategoryIds))
    }

    LaunchedEffect(Unit) {
        if (route == Route.Splash) route = Route.Home
    }
    LaunchedEffect(route) {
        if (route != Route.Splash && !reviewLaunchChecked) {
            showReviewPrompt = route == Route.Home && reviewPreferences.shouldShowAtLaunch(
                nowMillis = currentReviewTimeMillis(),
                hasOtherStartupDialog = showSexCategoryIntro,
                blackFridayVisible = showBlackFriday,
            )
            skipBlackFridayForReview = showReviewPrompt && showBlackFriday
            if (showReviewPrompt) analytics.log(AnalyticsEvent.REVIEW_PROMPT_SHOWN)
            reviewCheckedProcessId = reviewProcessId
        }
    }
    DisposableEffect(launchSoundPlayer) {
        onDispose { launchSoundPlayer.release() }
    }
    LaunchedEffect(Unit) {
        if (!launchSoundRequested) {
            launchSoundRequested = true
            val bytes = try {
                Res.readBytes("files/launch_sleigh_bells.mp3")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("Cannot read launch audio: ${error.message}")
                null
            }
            if (bytes != null) launchSoundPlayer.play(bytes)
        }
    }
    LaunchedEffect(route, showBlackFriday, showSexCategoryIntro, showReviewPrompt, reviewLaunchChecked) {
        if (route == Route.Home && !showBlackFriday && !showSexCategoryIntro &&
            reviewLaunchChecked && !showReviewPrompt && !reminderRequested
        ) {
            reminderRequested = true
            platformNotificationScheduler().requestPermissionAndSchedulePartyReminder()
        }
    }
    LaunchedEffect(route, holidaySequencePhase) {
        if (route != Route.Home) {
            if (holidaySequencePhase != HomeHolidaySequencePhase.IDLE) {
                holidaySequencePhase = reduceHomeHolidaySequence(
                    holidaySequencePhase,
                    HomeHolidaySequenceEvent.INTERRUPTED,
                )
            }
            return@LaunchedEffect
        }

        when (holidaySequencePhase) {
            HomeHolidaySequencePhase.IDLE -> Unit
            HomeHolidaySequencePhase.BUTTONS_EXITING -> {
                delay(HOLIDAY_BUTTON_TRANSITION_MILLIS)
                holidaySequencePhase = reduceHomeHolidaySequence(
                    holidaySequencePhase,
                    HomeHolidaySequenceEvent.BUTTONS_EXITED,
                )
            }
            HomeHolidaySequencePhase.REINDEER -> {
                delay(HOLIDAY_REINDEER_FALLBACK_MILLIS)
                holidaySequencePhase = reduceHomeHolidaySequence(
                    holidaySequencePhase,
                    HomeHolidaySequenceEvent.REINDEER_FINISHED,
                )
            }
            HomeHolidaySequencePhase.BUTTONS_ENTERING -> {
                delay(HOLIDAY_BUTTON_TRANSITION_MILLIS + 250L)
                holidaySequencePhase = reduceHomeHolidaySequence(
                    holidaySequencePhase,
                    HomeHolidaySequenceEvent.BUTTONS_RETURNED,
                )
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        IceBackdrop()
        SnowfallBackdrop()
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        ) {
            AnimatedContent(route) { current ->
                when (current) {
                    Route.Splash -> SplashScreen()
                    Route.Home -> HomeScreen(players, impostorCount, hintsEnabled, error,
                        onPlayers = { route = Route.Players }, onCategories = { route = Route.Categories },
                        onCodes = { route = Route.Premium }, onImpostorDecrease = {
                            error = null
                            impostorCount = (impostorCount - 1).coerceAtLeast(1)
                        }, onImpostorIncrease = {
                            error = null
                            impostorCount = (impostorCount + 1).coerceAtMost((players.size - 1).coerceAtLeast(1))
                        },
                        onHintsChanged = {
                            error = null
                            hintsEnabled = it
                        },
                        holidaySequencePhase = holidaySequencePhase,
                        onHolidayLongPress = {
                            holidaySequencePhase = reduceHomeHolidaySequence(
                                holidaySequencePhase,
                                HomeHolidaySequenceEvent.START,
                            )
                        },
                        onReindeerAnimationFinished = {
                            holidaySequencePhase = reduceHomeHolidaySequence(
                                holidaySequencePhase,
                                HomeHolidaySequenceEvent.REINDEER_FINISHED,
                            )
                        },
                        onButtonsReturned = {
                            holidaySequencePhase = reduceHomeHolidaySequence(
                                holidaySequencePhase,
                                HomeHolidaySequenceEvent.BUTTONS_RETURNED,
                            )
                        },
                        onShowSexCategoryIntro = {
                            sexIntroIsUpgradeNotice = false
                            showSexCategoryIntro = true
                        },
                        onStart = start@{
                            if (startingGame || route != Route.Home) return@start
                            val usesOnlyAlwaysFreeCategories = selectedCategories.isNotEmpty() &&
                                selectedCategories.all { it in com.impostor.domain.AlwaysFreeCategoryIds }
                            if (selectedCategories.isNotEmpty() && !usesOnlyAlwaysFreeCategories &&
                                !trialRepository.state.value.canStartGame(entitlement)
                            ) {
                                showFreeLimitDialog = true
                                route = Route.Categories
                                return@start
                            }
                            val setup = GameSetup(players, selectedCategories, impostorCount, hintsEnabled)
                            when (val validation = ValidateGameSetupUseCase().invoke(setup)) {
                                SetupValidation.Valid -> {
                                    error = null
                                    val session = StartGameUseCase(RandomSource { Random.nextInt(it) }).invoke(
                                        setup,
                                        prompts,
                                        sessionId = Random.nextLong().toString(),
                                    )
                                    startingGame = true
                                    scope.launch {
                                        try {
                                            if (requiresTrialGame(setup.selectedCategoryIds, entitlement)) {
                                                RecordStartedGameUseCase(trialRepository).invoke(session.sessionId)
                                            }
                                            route = Route.Reveal(session)
                                        } finally {
                                            startingGame = false
                                        }
                                    }
                                }
                                is SetupValidation.Invalid -> error = validation.reason
                            }
                        })
                    Route.Players -> PlayersScreen(players, onBack = { route = Route.Home }, onPlayers = {
                        error = null
                        players = it
                        savePlayers(it)
                        impostorCount = impostorCount.coerceAtMost((it.size - 1).coerceAtLeast(1))
                    })
                    Route.Categories -> CategoriesScreen(
                        options = sortByPopularity(categories.map { it.displayName to it.id }) { it.second },
                        selected = selectedCategories,
                        unlocked = unlockedCategories,
                        trialState = trialState,
                        premiumUnlocked = premiumUnlocked,
                        onBack = { route = Route.Home },
                        onOpenPremium = { route = Route.Premium },
                        onSelected = {
                            error = null
                            selectedCategories = it
                        },
                    )
                    Route.Premium -> PremiumSubscriptionScreen(
                        repository = subscriptionRepository,
                        analytics = analytics,
                        onClose = { route = Route.Home },
                    )
                    is Route.Reveal -> RevealScreen(current.session, players) { next -> route = next }
                    is Route.GameStart -> GameStartScreen(
                        session = current.session,
                        players = players,
                        onCompleted = {
                            reviewPreferences.recordCompletedGame()
                            route = Route.Home
                        },
                    )
                }
            }
        }
        if (route == Route.Home && reviewLaunchChecked && showBlackFriday && !skipBlackFridayForReview) {
            BlackFridayDialog(
                enabled = !promotionActionInProgress,
                onAccept = {
                    if (!promotionActionInProgress) {
                        promotionActionInProgress = true
                        scope.launch {
                            try {
                                trialRepository.claimBlackFriday()
                                promotionDismissedThisLaunch = true
                                analytics.log(AnalyticsEvent.BLACK_FRIDAY_ACCEPTED)
                            } finally {
                                promotionActionInProgress = false
                            }
                        }
                    }
                },
                onCancel = {
                    if (!promotionActionInProgress) {
                        reviewPreferences.markPromotionDismissed()
                        promotionDismissedThisLaunch = true
                        analytics.log(AnalyticsEvent.BLACK_FRIDAY_CANCELLED)
                    }
                },
            )
        } else if (route == Route.Home && reviewLaunchChecked && showSexCategoryIntro) {
            SexCategoryIntroDialog {
                if (sexIntroIsUpgradeNotice) markSexCategoryIntroDismissed()
                sexIntroIsUpgradeNotice = false
                showSexCategoryIntro = false
            }
        } else if (route == Route.Home && reviewLaunchChecked && showReviewPrompt &&
            !reviewPreferences.hasOpenedStore()
        ) {
            ReviewPromptDialog(
                openFailed = reviewOpenFailed,
                onRate = {
                    val opened = try {
                        uriHandler.openUri(reviewStoreUrl())
                        true
                    } catch (error: Exception) {
                        println("Cannot open review store: ${error.message}")
                        reviewOpenFailed = true
                        false
                    }
                    if (opened) {
                        analytics.log(AnalyticsEvent.REVIEW_PROMPT_RATE_CLICKED)
                        reviewPreferences.markStoreOpened()
                        showReviewPrompt = false
                        reviewOpenFailed = false
                    }
                },
                onNotNow = {
                    analytics.log(AnalyticsEvent.REVIEW_PROMPT_DISMISSED)
                    reviewPreferences.postpone(currentReviewTimeMillis())
                    showReviewPrompt = false
                    reviewOpenFailed = false
                },
            )
        }
        if (route == Route.Categories && showFreeLimitDialog) {
            FreeLimitDialog { showFreeLimitDialog = false }
        }
    }
}

/** Design: https://www.figma.com/design/Lkd0bevCBZZH40R4HNjSNy/impostor?node-id=83-73 */
@Composable
private fun ReviewPromptDialog(openFailed: Boolean, onRate: () -> Unit, onNotNow: () -> Unit) {
    Dialog(onDismissRequest = onNotNow) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier.fillMaxWidth().widthIn(max = 354.dp)
                .clip(shape)
                .background(Color(0xFF112033))
                .border(1.dp, Color(0xFF315D70), shape)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.review_prompt), color = Color(0xFFE5F1FA),
                fontSize = 16.sp, lineHeight = 22.sp)
            if (openFailed) {
                Text(stringResource(Res.string.review_open_failed), color = Color(0xFFFFA5B3), fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1.15f).heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF00DCEB), Color(0xFF0654D6))))
                        .clickable(onClick = onRate).padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.review_rate), color = Color.White, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onNotNow).padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.review_not_now), color = Color(0xFFB4CFDF), fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Black Friday promotion; design: https://www.figma.com/design/Lkd0bevCBZZH40R4HNjSNy/impostor?node-id=2-193 */
@Composable
private fun BlackFridayDialog(enabled: Boolean, onAccept: () -> Unit, onCancel: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BlackFridaySurface)
                .border(1.dp, BlackFridayMuted.copy(alpha = .3f), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                Modifier.fillMaxWidth().background(BlackFridayAccent).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("BLACK FRIDAY", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(Res.string.black_friday_eyebrow), color = BlackFridayMuted, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("5", color = BlackFridayAccent, fontSize = 120.sp, lineHeight = 120.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(Res.string.black_friday_games), color = Color.White, fontSize = 24.sp,
                        lineHeight = 28.sp, fontWeight = FontWeight.Black)
                }
                Text(stringResource(Res.string.black_friday_title), color = Color.White, fontSize = 28.sp,
                    lineHeight = 32.sp, fontWeight = FontWeight.Black)
                Text(stringResource(Res.string.black_friday_description), color = BlackFridayMuted, fontSize = 16.sp)
                Box(Modifier.fillMaxWidth().height(1.dp).background(BlackFridayMuted.copy(alpha = .3f)))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.black_friday_price), color = BlackFridayAccent, fontSize = 28.sp,
                        fontWeight = FontWeight.Black)
                    Text(stringResource(Res.string.black_friday_gift), color = BlackFridayMuted, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                        .background(BlackFridayAccent).clickable(enabled = enabled, onClick = onAccept).padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.black_friday_accept), color = Color.Black,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, onClick = onCancel),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.black_friday_cancel), color = BlackFridayMuted,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Shell(
    title: String,
    bottomContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(title, color = Color.White, fontSize = 28.sp, letterSpacing = 1.sp)
            content()
        }
        bottomContent?.invoke()
    }
}

@Composable private fun SplashScreen() = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Text("IMPOSTOR", color = Cyan, fontSize = 34.sp, letterSpacing = 4.sp)
}

@Composable private fun HomeScreen(
    players: List<Player>,
    impostorCount: Int,
    hintsEnabled: Boolean,
    error: SetupValidation.Reason?,
    holidaySequencePhase: HomeHolidaySequencePhase,
    onPlayers: () -> Unit,
    onCategories: () -> Unit,
    onCodes: () -> Unit,
    onImpostorDecrease: () -> Unit,
    onImpostorIncrease: () -> Unit,
    onHintsChanged: (Boolean) -> Unit,
    onHolidayLongPress: () -> Unit,
    onReindeerAnimationFinished: () -> Unit,
    onButtonsReturned: () -> Unit,
    onShowSexCategoryIntro: () -> Unit,
    onStart: () -> Unit,
): Unit {
    var showRules by remember { mutableStateOf(false) }
    val buttonOffset = remember { Animatable(0f) }
    val currentOnButtonsReturned by rememberUpdatedState(onButtonsReturned)
    val currentOnReindeerAnimationFinished by rememberUpdatedState(onReindeerAnimationFinished)
    val density = LocalDensity.current

    LaunchedEffect(holidaySequencePhase) {
        when (holidaySequencePhase) {
            HomeHolidaySequencePhase.IDLE -> buttonOffset.snapTo(0f)
            HomeHolidaySequencePhase.BUTTONS_EXITING -> {
                buttonOffset.animateTo(
                    1f,
                    tween(HOLIDAY_BUTTON_TRANSITION_MILLIS.toInt(), easing = FastOutSlowInEasing),
                )
            }
            HomeHolidaySequencePhase.REINDEER -> buttonOffset.snapTo(1f)
            HomeHolidaySequencePhase.BUTTONS_ENTERING -> {
                buttonOffset.snapTo(-1f)
                buttonOffset.animateTo(
                    0f,
                    tween(HOLIDAY_BUTTON_TRANSITION_MILLIS.toInt(), easing = FastOutSlowInEasing),
                )
                currentOnButtonsReturned()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactLayout = maxHeight < 740.dp
        val verticalPadding = if (compactLayout) 8.dp else 20.dp
        val snowmanLaneHeight = if (compactLayout) 40.dp else 52.dp
        val settingsDownshift = 88.dp / 3
        val screenWidthPx = with(density) { maxWidth.toPx() }
        if (hasTwoPanes(maxWidth.value, minPaneWidth = 280f, horizontalGaps = 72f)) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(Res.string.home_tagline), color = Frost.copy(alpha = .78f),
                        fontSize = 11.sp, letterSpacing = 5.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    LongPressLogo(onLongPress = onHolidayLongPress, onTripleTap = onShowSexCategoryIntro)
                    Spacer(Modifier.height(10.dp))
                    val playingNow = remember { playersOnline(currentLocalMoment()) }
                    Text("${stringResource(Res.string.home_playing_now)}: $playingNow",
                        color = Cyan, fontSize = 13.sp, letterSpacing = 2.sp)
                    Box(Modifier.widthIn(max = 480.dp).fillMaxWidth().aspectRatio(1125f / 647f)) {
                        HolidayLottie(
                            assetName = "christmas_sleigh.json",
                            modifier = Modifier.fillMaxSize(),
                            iterations = Int.MAX_VALUE,
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 56.dp + settingsDownshift)
                        .graphicsLayer { translationX = buttonOffset.value * screenWidthPx },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeReveal(index = 0, height = 88.dp) {
                        FrostSettingButton(stringResource(Res.string.home_players), players.size.toString(),
                            onPlayers, autoPulse = true, pulseDelayMillis = 0L)
                    }
                    HomeReveal(index = 1, height = 88.dp) {
                        FrostSettingButton(stringResource(Res.string.home_categories), "→", onCategories,
                            autoPulse = true, pulseDelayMillis = 600L)
                    }
                    HomeReveal(index = 2, height = 72.dp) {
                        FrostToggleSetting(stringResource(Res.string.home_hint), hintsEnabled, onHintsChanged)
                    }
                    HomeReveal(index = 3, height = 88.dp) {
                        FrostImpostorSetting(impostorCount, onImpostorDecrease, onImpostorIncrease)
                    }
                    error?.let { reason ->
                        val reasonLabel = when (reason) {
                            SetupValidation.Reason.TOO_FEW_PLAYERS -> stringResource(Res.string.validation_too_few_players)
                            SetupValidation.Reason.NO_CATEGORY -> stringResource(Res.string.validation_no_category)
                            SetupValidation.Reason.INVALID_IMPOSTOR_COUNT -> stringResource(Res.string.validation_invalid_impostor_count)
                            SetupValidation.Reason.DUPLICATE_PLAYER_IDS -> stringResource(Res.string.validation_duplicate_player_ids)
                            SetupValidation.Reason.DUPLICATE_PLAYER_NAMES -> stringResource(Res.string.validation_duplicate_player_names)
                            SetupValidation.Reason.EMPTY_PLAYER_NAME -> stringResource(Res.string.validation_empty_player_name)
                        }
                        Text(stringResource(Res.string.home_validation_error, reasonLabel),
                            color = Color(0xFFFFA5B3), fontSize = 13.sp)
                    }
                    SnowmanLane(height = snowmanLaneHeight)
                    FrostButton(stringResource(Res.string.play), onStart, Modifier.fillMaxWidth(),
                        style = FrostButtonStyle.Primary, autoPulse = true, pulseDelayMillis = 1200L)
                    Spacer(Modifier.height(10.dp))
                    FrostButton(stringResource(Res.string.unlock_full_version), onCodes, Modifier.fillMaxWidth(),
                        style = FrostButtonStyle.Subtle, autoPulse = true, pulseDelayMillis = 1800L)
                }
            }
        } else Column(
            modifier = Modifier.fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.home_tagline), color = Frost.copy(alpha = .78f), fontSize = 11.sp, letterSpacing = 5.sp)
                Spacer(Modifier.height(8.dp))
                LongPressLogo(
                    onLongPress = onHolidayLongPress,
                    onTripleTap = onShowSexCategoryIntro,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.width(120.dp).height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Cyan.copy(alpha = .8f), Color.Transparent),
                            ),
                        ),
                )
                Spacer(Modifier.height(10.dp))
                val playingNow = remember { playersOnline(currentLocalMoment()) }
                Text(
                    "${stringResource(Res.string.home_playing_now)}: $playingNow",
                    color = Cyan,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                )
                Box(
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        // The sleigh artwork only fills the top of its canvas; drop the empty lower part.
                        val height = (placeable.height * 3 / 10)
                            .coerceIn(constraints.minHeight, constraints.maxHeight)
                        layout(placeable.width, height) { placeable.placeRelative(0, 0) }
                    }.widthIn(max = 480.dp).fillMaxWidth().aspectRatio(1125f / 647f),
                ) {
                    HolidayLottie(
                        assetName = "christmas_sleigh.json",
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxSize(),
                        iterations = Int.MAX_VALUE,
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(settingsDownshift))
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        translationX = buttonOffset.value * screenWidthPx
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeReveal(index = 0, height = 88.dp) {
                        FrostSettingButton(
                            stringResource(Res.string.home_players),
                            players.size.toString(),
                            onPlayers,
                            autoPulse = true,
                            pulseDelayMillis = 0L,
                        )
                    }
                    HomeReveal(index = 1, height = 88.dp) {
                        FrostSettingButton(
                            stringResource(Res.string.home_categories),
                            "→",
                            onCategories,
                            autoPulse = true,
                            pulseDelayMillis = 600L,
                        )
                    }
                    HomeReveal(index = 2, height = 72.dp) {
                        FrostToggleSetting(stringResource(Res.string.home_hint), hintsEnabled, onHintsChanged)
                    }
                    HomeReveal(index = 3, height = 88.dp) {
                        FrostImpostorSetting(impostorCount, onImpostorDecrease, onImpostorIncrease)
                    }
                    val validationMessage = error?.let { reason ->
                        val reasonLabel = when (reason) {
                            SetupValidation.Reason.TOO_FEW_PLAYERS -> stringResource(Res.string.validation_too_few_players)
                            SetupValidation.Reason.NO_CATEGORY -> stringResource(Res.string.validation_no_category)
                            SetupValidation.Reason.INVALID_IMPOSTOR_COUNT -> stringResource(Res.string.validation_invalid_impostor_count)
                            SetupValidation.Reason.DUPLICATE_PLAYER_IDS -> stringResource(Res.string.validation_duplicate_player_ids)
                            SetupValidation.Reason.DUPLICATE_PLAYER_NAMES -> stringResource(Res.string.validation_duplicate_player_names)
                            SetupValidation.Reason.EMPTY_PLAYER_NAME -> stringResource(Res.string.validation_empty_player_name)
                        }
                        stringResource(Res.string.home_validation_error, reasonLabel)
                    }
                    if (validationMessage != null) {
                        Text(
                            validationMessage,
                            color = Color(0xFFFFA5B3),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().graphicsLayer {
                    translationX = buttonOffset.value * screenWidthPx
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SnowmanLane(height = snowmanLaneHeight)
                FrostButton(
                    text = stringResource(Res.string.play),
                    onClick = onStart,
                    style = FrostButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    autoPulse = true,
                    pulseDelayMillis = 1200L,
                )
                Spacer(Modifier.height(18.dp))
                FrostButton(
                    text = stringResource(Res.string.unlock_full_version),
                    onClick = onCodes,
                    style = FrostButtonStyle.Subtle,
                    modifier = Modifier.fillMaxWidth(),
                    autoPulse = true,
                    pulseDelayMillis = 1800L,
                )
            }
        }
        FrostButton(
            text = "?",
            onClick = { showRules = true },
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp).size(52.dp).graphicsLayer {
                translationX = buttonOffset.value * screenWidthPx
            },
            closeButton = true,
        )
        Text(
            text = "v${appVersion()}",
            color = Frost.copy(alpha = .78f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
        )
        if (showRules) {
            GameRulesOverlay(onDismiss = { showRules = false })
        }
        if (holidaySequencePhase == HomeHolidaySequencePhase.REINDEER) {
            HolidayLottie(
                assetName = "reindeer.json",
                modifier = Modifier.align(Alignment.Center).size(if (compactLayout) 220.dp else 280.dp),
                contentScale = ContentScale.Fit,
                onFinished = currentOnReindeerAnimationFinished,
            )
        }
    }
}

@Composable
private fun FreeLimitDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF16365A))
                .border(1.dp, Cyan.copy(alpha = .72f), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(Res.string.free_limit_dialog_message), color = Color.White, fontSize = 17.sp)
            FrostButton(
                text = stringResource(Res.string.ok),
                onClick = onDismiss,
                style = FrostButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SexCategoryIntroDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier.fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Cyan.copy(alpha = .58f), RoundedCornerShape(28.dp)),
        ) {
            Image(
                painter = painterResource(Res.drawable.sex_category_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier.matchParentSize()
                    .background(Color(0xB20A1530)),
            )
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(Res.string.new_category),
                    color = Frost.copy(alpha = .84f),
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                )
                Text(
                    "SEX",
                    color = Color.White,
                    fontSize = 30.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(Res.string.sex_category_description),
                    color = Color.White,
                    fontSize = 16.sp,
                )
                Box(
                    Modifier.fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Cyan.copy(alpha = .7f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.sex_category_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(Modifier.matchParentSize().background(Color(0x66000000)))
                    Text(
                        stringResource(Res.string.ok),
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameRulesOverlay(onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(Ink.copy(alpha = .72f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    waitForUpOrCancellation()
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
                .border(1.dp, glassOpacity(Cyan.copy(alpha = .6f)), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(Res.string.rules_title), color = Color.White, fontSize = 24.sp, letterSpacing = 2.sp)
            Text(stringResource(Res.string.rules_step_1), color = Frost, fontSize = 16.sp)
            Text(stringResource(Res.string.rules_step_2), color = Frost, fontSize = 16.sp)
            Text(stringResource(Res.string.rules_step_3), color = Frost, fontSize = 16.sp)
            Text(stringResource(Res.string.rules_step_4), color = Frost, fontSize = 16.sp)
            FrostButton(
                text = stringResource(Res.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FrostButtonStyle.Subtle,
            )
        }
    }
}

@Composable
private fun LongPressLogo(
    onLongPress: () -> Unit,
    onTripleTap: () -> Unit,
) {
    var isHolding by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapMark by remember { mutableStateOf<TimeMark?>(null) }
    val currentOnTripleTap by rememberUpdatedState(onTripleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(if (isHolding) 2000 else 120),
        label = "holiday-sequence-long-press-progress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                isHolding = true
                val completedTap = withTimeoutOrNull(2000) {
                    waitForUpOrCancellation() != null
                } == true
                isHolding = false
                if (completedTap) {
                    val previous = lastTapMark
                    tapCount = if (previous != null && previous.elapsedNow() < 650.milliseconds) tapCount + 1 else 1
                    lastTapMark = TimeSource.Monotonic.markNow()
                    if (tapCount == 3) {
                        tapCount = 0
                        lastTapMark = null
                        currentOnTripleTap()
                    }
                } else {
                    currentOnLongPress()
                    waitForUpOrCancellation()
                }
            }
        },
    ) {
        Text("IMPOSTOR", color = Color(0xFFEAF7FF), fontSize = 44.sp, letterSpacing = 1.sp)
        Box(
            Modifier.padding(top = 4.dp).width(96.dp).height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Cyan.copy(alpha = .16f)),
        ) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxSize()
                    .background(Cyan.copy(alpha = .8f)),
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun HolidayLottie(
    assetName: String,
    modifier: Modifier,
    iterations: Int = 1,
    contentScale: ContentScale = ContentScale.Fit,
    onFinished: (() -> Unit)? = null,
) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes("files/$assetName").decodeToString())
    }
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(composition, iterations = iterations)

    LaunchedEffect(compositionResult.isFailure, composition, progress) {
        if (compositionResult.isFailure || (iterations == 1 && composition != null && progress >= .999f)) {
            onFinished?.invoke()
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (composition != null) {
            Image(
                painter = rememberLottiePainter(composition = composition, progress = { progress }),
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SnowmanLane(height: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "snowman-crossing")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
        ),
        label = "snowman-position",
    )
    val animationSize = if (height < 44.dp) 36.dp else 46.dp
    val alpha = when {
        progress < .08f -> progress / .08f
        progress > .92f -> (1f - progress) / .08f
        else -> 1f
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        HolidayLottie(
            assetName = "snowman_dance_and_sing.json",
            modifier = Modifier
                .offset(x = -animationSize + (maxWidth + animationSize) * progress)
                .size(animationSize)
                .graphicsLayer { this.alpha = alpha },
            iterations = Int.MAX_VALUE,
            contentScale = ContentScale.Fit,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun WalkingLadyOverlay(onDismiss: () -> Unit) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes("files/walking_lady.json").decodeToString())
    }
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    LaunchedEffect(compositionResult.isFailure, composition, progress) {
        if (compositionResult.isFailure || (composition != null && progress >= .999f)) onDismiss()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Ink.copy(alpha = .58f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    waitForUpOrCancellation()
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (composition != null) {
            Image(
                painter = rememberLottiePainter(composition = composition, progress = { progress }),
                contentDescription = stringResource(Res.string.walking_lady_animation),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(280.dp),
            )
        }
    }
}

@Composable
private fun HomeReveal(index: Int, height: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = height)) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = tween(durationMillis = 420, delayMillis = index * 70),
            ) + scaleIn(
                initialScale = .92f,
                animationSpec = tween(durationMillis = 420, delayMillis = index * 70),
            ),
        ) {
            content()
        }
    }
}

internal enum class FrostButtonStyle {
    Primary,
    Glass,
    Subtle,
}

/** Shared interactive control with a left-to-right ice-light sweep on hover transitions and taps. */
@Composable
internal fun FrostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FrostButtonStyle = FrostButtonStyle.Glass,
    enabled: Boolean = true,
    closeButton: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    autoPulse: Boolean = false,
    pulseDelayMillis: Long = 0L,
    fontSize: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    backgroundImage: Painter? = null,
    selected: Boolean = false,
) {
    FrostButtonSurface(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        closeButton = closeButton,
        onLongPress = onLongPress,
        autoPulse = autoPulse,
        pulseDelayMillis = pulseDelayMillis,
        backgroundImage = backgroundImage,
        selected = selected,
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (style == FrostButtonStyle.Primary) Ink else Frost
            } else {
                (if (style == FrostButtonStyle.Primary) Ink else Frost).copy(alpha = .4f)
            },
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else when {
                closeButton -> 26.sp
                style == FrostButtonStyle.Primary -> 22.sp
                else -> 15.sp
            },
            letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else when {
                closeButton -> 0.sp
                style == FrostButtonStyle.Primary -> 6.sp
                else -> 2.sp
            },
            lineHeight = if (closeButton) 26.sp else TextUnit.Unspecified,
            style = if (closeButton) {
                LocalTextStyle.current.copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                )
            } else {
                LocalTextStyle.current
            },
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun FrostButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FrostButtonStyle = FrostButtonStyle.Glass,
    enabled: Boolean = true,
    closeButton: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    autoPulse: Boolean = false,
    pulseDelayMillis: Long = 0L,
    backgroundImage: Painter? = null,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .965f else 1f, tween(120), label = "button-press")
    val sweep = remember { Animatable(-.45f) }
    val scope = rememberCoroutineScope()
    var hoverWasObserved by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    fun runSweep() {
        scope.launch {
            sweep.snapTo(-.45f)
            sweep.animateTo(1.45f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(hovered) {
        if (hoverWasObserved) runSweep()
        hoverWasObserved = true
    }

    if (autoPulse) {
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(2500 + pulseDelayMillis)
                runSweep()
            }
        }
    }

    val shape = RoundedCornerShape(if (closeButton) 18.dp else if (style == FrostButtonStyle.Subtle) 28.dp else 24.dp)
    val background = when (style) {
        FrostButtonStyle.Primary -> Brush.verticalGradient(listOf(Color(0xFF84D7FF), Color(0xFF3D9BDF)))
        FrostButtonStyle.Glass -> Brush.verticalGradient(listOf(PanelTop, PanelBottom))
        FrostButtonStyle.Subtle -> Brush.horizontalGradient(
            listOf(glassOpacity(PanelTop.copy(alpha = .55f)), PanelBottom),
        )
    }
    val textColor = if (style == FrostButtonStyle.Primary) Ink else Frost

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(background)
            .then(
                if (backgroundImage != null) {
                    Modifier.paint(backgroundImage, sizeToIntrinsics = false, contentScale = ContentScale.FillBounds)
                } else {
                    Modifier
                },
            )
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) Color(0xFF071A3A) else if (style == FrostButtonStyle.Primary) Color(0xFFD5F5FF) else PanelBorder,
                shape,
            )
            .hoverable(interactionSource, enabled)
            .then(if (onLongPress == null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                ) {
                    runSweep()
                    onClick()
                }
            } else {
                Modifier.semantics {
                    role = androidx.compose.ui.semantics.Role.Button
                    onClick {
                        if (enabled) {
                            runSweep()
                            currentOnClick()
                            true
                        } else {
                            false
                        }
                    }
                }.pointerInput(enabled) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val completedBeforeLongPress = withTimeoutOrNull(2_000) {
                            waitForUpOrCancellation() != null
                        }
                        when (completedBeforeLongPress) {
                            true -> if (enabled) {
                                runSweep()
                                currentOnClick()
                            }
                            null -> {
                                if (enabled) currentOnLongPress?.invoke()
                                waitForUpOrCancellation()
                            }
                            false -> Unit
                        }
                    }
                }
            })
            .frostSweep(
                sweep.value,
                shapeRadius = if (closeButton) 18.dp else if (style == FrostButtonStyle.Subtle) 28.dp else 24.dp,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun FrostCloseButton(onClick: () -> Unit) {
    FrostButton(
        text = "X",
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        closeButton = true,
    )
}

@Composable
internal fun FrostSettingButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    autoPulse: Boolean = false,
    pulseDelayMillis: Long = 0L,
) {
    FrostButtonSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
        style = FrostButtonStyle.Glass,
        autoPulse = autoPulse,
        pulseDelayMillis = pulseDelayMillis,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color(0xFFF2FAFF), fontSize = 21.sp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                    .background(glassOpacity(Color(0xFF65BFF5).copy(alpha = .18f)))
                    .border(1.dp, PanelBorder, RoundedCornerShape(22.dp)),
            ) {
                Text(value, color = Frost, fontSize = if (value == "→") 28.sp else 22.sp)
            }
        }
    }
}

@Composable
private fun FrostToggleSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
            .border(1.dp, PanelBorder, shape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFFF2FAFF), fontSize = 21.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Cyan,
                checkedBorderColor = Frost,
                uncheckedThumbColor = Frost,
                uncheckedTrackColor = PanelBottom,
                uncheckedBorderColor = PanelBorder,
            ),
        )
    }
}

@Composable
private fun FrostImpostorSetting(
    impostorCount: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
            .border(1.dp, PanelBorder, shape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(Res.string.impostors), color = Color(0xFFF2FAFF), fontSize = 21.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FrostButton(text = "−", onClick = onDecrease, modifier = Modifier.width(54.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(glassOpacity(Color(0xFF65BFF5).copy(alpha = .18f)))
                    .border(1.dp, PanelBorder, RoundedCornerShape(18.dp)),
            ) {
                Text(impostorCount.toString(), color = Frost, fontSize = 24.sp)
            }
            FrostButton(text = "+", onClick = onIncrease, modifier = Modifier.width(54.dp))
        }
    }
}

private fun Modifier.frostSweep(progress: Float, shapeRadius: androidx.compose.ui.unit.Dp = 24.dp) = drawWithCache {
    val sweepWidth = size.width * .38f
    val x = -sweepWidth + (size.width + sweepWidth * 2) * progress
    val sweepBrush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = .26f), Cyan.copy(alpha = .15f), Color.Transparent),
        start = Offset(x - sweepWidth, 0f),
        end = Offset(x + sweepWidth, size.height),
    )
    val clipPath = androidx.compose.ui.graphics.Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                0f,
                0f,
                size.width,
                size.height,
                shapeRadius.toPx(),
                shapeRadius.toPx(),
            ),
        )
    }
    onDrawWithContent {
        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath)
        drawContent()
        drawRect(sweepBrush)
        drawContext.canvas.restore()
    }
}

@Composable
private fun PlayersScreen(
    players: List<Player>,
    onBack: () -> Unit,
    onPlayers: (List<Player>) -> Unit,
) {
    var newPlayerName by rememberSaveable { mutableStateOf("") }
    var clearedDefaultPlayerIds by rememberSaveable(stateSaver = stringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    val newPlayerFallbackName = stringResource(Res.string.default_player, players.size + 1)

    val playerList: @Composable (Modifier) -> Unit = { modifier ->
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        players.forEachIndexed { index, player ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
                    .border(1.dp, PanelBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                        .background(glassOpacity(Color(0xFF65BFF5).copy(alpha = .18f)))
                        .border(1.dp, PanelBorder, RoundedCornerShape(13.dp)),
                ) {
                    Text("${index + 1}", color = Frost, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                val defaultName = stringResource(Res.string.default_player, index + 1)
                val legacyPolishName = stringResource(Res.string.legacy_polish_default_player, index + 1)
                val englishDefaultName = stringResource(Res.string.english_default_player, index + 1)
                val isDefaultName = player.displayName == defaultName ||
                    player.displayName == legacyPolishName ||
                    player.displayName == englishDefaultName ||
                    player.displayName == defaultPlayerToken(index + 1)
                BasicTextField(
                    value = if (isDefaultName) defaultName else player.displayName,
                    onValueChange = { displayName ->
                        onPlayers(players.mapIndexed { playerIndex, current ->
                            if (playerIndex == index) {
                                current.copy(displayName = normalizePlayerName(displayName))
                            } else {
                                current
                            }
                        })
                    },
                    modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                        if (
                            focusState.isFocused &&
                            player.id !in clearedDefaultPlayerIds &&
                            isDefaultName
                        ) {
                            clearedDefaultPlayerIds += player.id
                            onPlayers(players.mapIndexed { playerIndex, current ->
                                if (playerIndex == index) current.copy(displayName = "") else current
                            })
                        }
                    },
                    textStyle = TextStyle(color = Color.White, fontSize = 22.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                FrostButton(
                    text = "X",
                    onClick = { if (players.size > 3) onPlayers(players.filterIndexed { i, _ -> i != index }) },
                    modifier = Modifier.size(48.dp),
                    closeButton = true,
                )
            }
        }
    }
    }
    val playerControls: @Composable (Modifier) -> Unit = { modifier ->
    Column(modifier) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
                .border(1.dp, PanelBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = newPlayerName,
                onValueChange = { newPlayerName = normalizePlayerName(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                decorationBox = { field ->
                    if (newPlayerName.isEmpty()) {
                        Text(stringResource(Res.string.player_name_placeholder), color = Frost.copy(alpha = .58f), fontSize = 17.sp)
                    }
                    field()
                },
            )
        }
        Spacer(Modifier.width(10.dp))
        FrostButton(
            text = "+",
            onClick = {
                val displayName = normalizePlayerName(newPlayerName)
                    .ifEmpty { newPlayerFallbackName }
                onPlayers(players + Player(nextPlayerId(players), displayName))
                newPlayerName = ""
            },
            modifier = Modifier.size(56.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    FrostButton(
        text = stringResource(Res.string.done),
        onClick = {
            val updatedPlayers = appendPendingPlayer(players, newPlayerName)
            if (updatedPlayers.size != players.size) onPlayers(updatedPlayers)
            onBack()
        },
        modifier = Modifier.fillMaxWidth(),
        style = FrostButtonStyle.Subtle,
    )
    }
    }
    BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 20.dp)) {
        val wide = hasTwoPanes(maxWidth.value, minPaneWidth = 284f, horizontalGaps = 24f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                FrostButton("‹", onBack, Modifier.align(Alignment.CenterStart).size(52.dp), closeButton = true)
                Text(stringResource(Res.string.players_title), color = Color.White, fontSize = 28.sp,
                    letterSpacing = 1.sp, modifier = Modifier.align(Alignment.Center))
                Text(players.size.toString(), color = Frost, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(18.dp))
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    playerControls(Modifier.weight(1f))
                    playerList(Modifier.weight(1f))
                }
            } else {
                playerList(Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                playerControls(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CategoriesScreen(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    unlocked: Set<String>,
    trialState: TrialState,
    premiumUnlocked: Boolean,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onSelected: (Set<String>) -> Unit,
) {
    var showPremiumPrompt by remember { mutableStateOf(false) }
    var showWalkingLady by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        val columns = categoryColumnCount(maxWidth.value)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.categories_title),
            color = Color.White,
            fontSize = 28.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        FrostCloseButton(onClick = onBack)
    }
    Spacer(Modifier.height(18.dp))
    Text(
        if (premiumUnlocked) stringResource(Res.string.premium_unlocks_all_categories)
        else if (trialState.hasFreeCategories)
            stringResource(Res.string.categories_remaining_games, trialState.remainingFreeGames)
        else stringResource(Res.string.categories_free_limit),
        color = Frost,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(18.dp))
    if ((showPremiumPrompt || !trialState.hasFreeCategories) && !premiumUnlocked) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(glassOpacity(Color(0xFF16365A).copy(alpha = .86f)))
                .border(1.dp, glassOpacity(Cyan.copy(alpha = .72f)), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(Res.string.category_requires_premium), color = Color.White, fontSize = 16.sp)
            FrostButton(
                text = stringResource(Res.string.unlock_premium),
                onClick = onOpenPremium,
                style = FrostButtonStyle.Primary,
                fontSize = 16.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val categoryButtonHeight = 72.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val categoryButton: @Composable (Pair<String, String>, Modifier) -> Unit = { (name, id), modifier ->
            val isUnlocked = id in unlocked
            FrostButton(
                text = when {
                    !isUnlocked && id in selected -> "🔒  ✓  $name"
                    !isUnlocked -> "🔒  $name"
                    id in selected -> "✓  $name"
                    else -> name
                },
                onClick = {
                    if (isUnlocked || id in selected) {
                        onSelected(toggleCategorySelection(selected, id))
                    } else {
                        showPremiumPrompt = true
                    }
                },
                onLongPress = if (id == "adult") ({ showWalkingLady = true }) else null,
                enabled = true,
                modifier = modifier.height(categoryButtonHeight),
                backgroundImage = if (id == "christmas") painterResource(Res.drawable.christmas_category_background) else null,
                selected = id in selected,
            )
        }
        val (popularOptions, otherOptions) = categorySections(options) { it.second }
        Text(
            stringResource(Res.string.categories_most_popular, remember { currentLocalMoment().dateLabel }),
            color = Cyan,
            fontSize = 14.sp,
        )
        popularOptions.chunked(columns).forEach { categoryRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryRow.forEach { categoryButton(it, Modifier.weight(1f)) }
                repeat(columns - categoryRow.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (otherOptions.isNotEmpty()) {
            Text(stringResource(Res.string.categories_rest), color = Cyan, fontSize = 14.sp)
        }
        otherOptions.chunked(columns).forEach { categoryRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryRow.forEach { categoryButton(it, Modifier.weight(1f)) }
                repeat(columns - categoryRow.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
        Spacer(Modifier.height(16.dp))
        FrostButton(text = stringResource(Res.string.done), onClick = onBack, modifier = Modifier.fillMaxWidth(), style = FrostButtonStyle.Subtle)
        }
        }
        if (showWalkingLady) {
            Box(Modifier.fillMaxSize().zIndex(1f)) {
                WalkingLadyOverlay(onDismiss = { showWalkingLady = false })
            }
        }
    }
}

@Composable
private fun PremiumSubscriptionScreen(
    repository: SubscriptionRepository,
    analytics: com.impostor.domain.AnalyticsService,
    onClose: () -> Unit,
) {
    val entitlement by repository.entitlement.collectAsState(SubscriptionState.LOCKED.let {
        com.impostor.domain.Entitlement(state = it, source = com.impostor.domain.EntitlementSource.LOCAL_CACHE)
    })
    var product by remember { mutableStateOf<com.impostor.domain.SubscriptionProduct?>(null) }
    var message by remember { mutableStateOf<SubscriptionError?>(null) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val purchase = remember(repository) { PurchasePremiumUseCase(repository) }
    val restore = remember(repository) { RestorePurchasesUseCase(repository) }

    LaunchedEffect(Unit) {
        analytics.log(AnalyticsEvent.PREMIUM_SCREEN_OPENED)
        repository.loadProduct()
            .onSuccess {
                product = it
                analytics.log(AnalyticsEvent.SUBSCRIPTION_PRODUCT_LOADED)
            }
            .onFailure { message = subscriptionError(it) }
        // Restore is store-account authenticated (AppStore.sync() on iOS) and must stay user-initiated only.
    }
    LaunchedEffect(entitlement.state, entitlement.source) {
        analytics.log(AnalyticsEvent.PREMIUM_ENTITLEMENT_CHANGED, mapOf("state" to entitlement.state.name.lowercase()))
    }

    Shell(
        title = stringResource(Res.string.premium_title),
        bottomContent = {
            FrostButton(
                text = stringResource(Res.string.close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                style = FrostButtonStyle.Subtle,
            )
        },
    ) {
        Text(product?.title ?: stringResource(Res.string.premium_product_fallback), color = Color.White, fontSize = 22.sp)
        Text(stringResource(Res.string.premium_benefit_description), color = Frost)
        Text(
            product?.let { "${it.formattedPrice} / ${localizedPeriodLabel(it.periodLabel)}" }
                ?: stringResource(Res.string.subscription_unavailable),
            color = Frost,
        )
        product?.let {
            Text(
                stringResource(Res.string.subscription_auto_renewal, localizedPeriodLabel(it.periodLabel)),
                color = Frost,
                fontSize = 13.sp,
            )
        }
        Text(subscriptionStateLabel(entitlement.state), color = if (entitlement.state == SubscriptionState.ACTIVE) Cyan else Frost)
        message?.let { Text(subscriptionErrorLabel(it), color = Color(0xFFFF8A8A)) }
        FrostButton(
            text = stringResource(Res.string.subscribe),
            onClick = {
                analytics.log(AnalyticsEvent.SUBSCRIPTION_PURCHASE_STARTED)
                message = null
                scope.launch {
                    purchase.invoke()
                        .onFailure { message = subscriptionError(it) }
                        .onSuccess { analytics.log(AnalyticsEvent.SUBSCRIPTION_PURCHASE_RESULT, mapOf("result" to purchaseResult(it.state))) }
                }
            },
            enabled = product != null && entitlement.state != SubscriptionState.PURCHASING && entitlement.state != SubscriptionState.RESTORING,
            modifier = Modifier.fillMaxWidth(),
        )
        FrostButton(
            text = stringResource(Res.string.restore_purchases),
            onClick = {
                message = null
                scope.launch {
                    restore.invoke()
                        .onFailure {
                            message = subscriptionError(it)
                            analytics.log(AnalyticsEvent.SUBSCRIPTION_RESTORE_RESULT, mapOf("result" to "failed"))
                        }
                        .onSuccess { restored ->
                            analytics.log(
                                AnalyticsEvent.SUBSCRIPTION_RESTORE_RESULT,
                                mapOf("result" to if (restored.state == SubscriptionState.ACTIVE) "active" else "none"),
                            )
                        }
                }
            },
            enabled = entitlement.state != SubscriptionState.PURCHASING && entitlement.state != SubscriptionState.RESTORING,
            modifier = Modifier.fillMaxWidth(),
        )
        FrostButton(
            text = stringResource(Res.string.manage_subscription),
            onClick = { scope.launch { repository.openManageSubscriptions().onFailure { message = subscriptionError(it) } } },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.privacy_policy),
                color = Cyan,
                fontSize = 13.sp,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { uriHandler.openUri(PrivacyPolicyUrl) },
            )
            Text("·", color = Frost, fontSize = 13.sp)
            Text(
                text = stringResource(Res.string.terms_of_use),
                color = Cyan,
                fontSize = 13.sp,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { uriHandler.openUri(TermsOfUseUrl) },
            )
        }
    }
}

private fun purchaseResult(state: SubscriptionState): String = when (state) {
    SubscriptionState.ACTIVE -> "success"
    SubscriptionState.PURCHASING -> "pending"
    else -> "failed"
}

private fun subscriptionError(error: Throwable): SubscriptionError =
    (error as? SubscriptionException)?.error ?: SubscriptionError.PURCHASE_FAILED

@Composable
private fun subscriptionErrorLabel(error: SubscriptionError): String = when (error) {
    SubscriptionError.PURCHASE_PENDING -> stringResource(Res.string.purchase_pending_message)
    SubscriptionError.PURCHASE_CANCELLED -> stringResource(Res.string.purchase_cancelled_message)
    SubscriptionError.ALREADY_OWNED -> stringResource(Res.string.already_owned_message)
    SubscriptionError.NETWORK_UNAVAILABLE -> stringResource(Res.string.network_unavailable_message)
    SubscriptionError.PRODUCT_UNAVAILABLE, SubscriptionError.STORE_UNAVAILABLE ->
        stringResource(Res.string.store_unavailable_message)
    SubscriptionError.RESTORE_FAILED -> stringResource(Res.string.restore_failed_message)
    SubscriptionError.PURCHASE_FAILED -> stringResource(Res.string.purchase_failed_message)
}

@Composable
private fun subscriptionStateLabel(state: SubscriptionState): String = when (state) {
    SubscriptionState.LOCKED -> stringResource(Res.string.subscription_locked)
    SubscriptionState.PURCHASING -> stringResource(Res.string.subscription_purchasing)
    SubscriptionState.ACTIVE -> stringResource(Res.string.subscription_active)
    SubscriptionState.EXPIRED -> stringResource(Res.string.subscription_expired)
    SubscriptionState.REVOKED -> stringResource(Res.string.subscription_revoked)
    SubscriptionState.RESTORING -> stringResource(Res.string.subscription_restoring)
    SubscriptionState.UNAVAILABLE -> stringResource(Res.string.subscription_unavailable_state)
}

@Composable
private fun localizedPeriodLabel(period: String): String {
    val normalized = period.trim().lowercase()
    val quantity = normalized.substringBefore(' ').toIntOrNull()
    val unit = if (quantity == null) normalized else normalized.substringAfter(' ')
    return when (unit.trimEnd('s')) {
        "day" -> when {
            quantity == 1 -> stringResource(Res.string.period_day)
            quantity != null && quantity in 2..4 -> stringResource(Res.string.period_days_2_4, quantity)
            quantity != null -> stringResource(Res.string.period_days, quantity)
            normalized.endsWith('s') -> stringResource(Res.string.period_days_unit)
            else -> stringResource(Res.string.period_day)
        }
        "week" -> when {
            quantity == 1 -> stringResource(Res.string.period_week)
            quantity != null && quantity in 2..4 -> stringResource(Res.string.period_weeks_2_4, quantity)
            quantity != null -> stringResource(Res.string.period_weeks, quantity)
            normalized.endsWith('s') -> stringResource(Res.string.period_weeks_unit)
            else -> stringResource(Res.string.period_week)
        }
        "month" -> when {
            quantity == 1 -> stringResource(Res.string.period_month)
            quantity != null && quantity in 2..4 -> stringResource(Res.string.period_months_2_4, quantity)
            quantity != null -> stringResource(Res.string.period_months, quantity)
            normalized.endsWith('s') -> stringResource(Res.string.period_months_unit)
            else -> stringResource(Res.string.period_month)
        }
        "year" -> when {
            quantity == 1 -> stringResource(Res.string.period_year)
            quantity != null && quantity in 2..4 -> stringResource(Res.string.period_years_2_4, quantity)
            quantity != null -> stringResource(Res.string.period_years, quantity)
            normalized.endsWith('s') -> stringResource(Res.string.period_years_unit)
            else -> stringResource(Res.string.period_year)
        }
        else -> stringResource(Res.string.period_other)
    }
}

@Composable private fun RevealScreen(
    session: GameSession,
    players: List<Player>,
    onRoute: (Route) -> Unit,
) {
    val assignment = session.assignments[session.revealIndex]
    val playerName = localizedPlayerName(players.firstOrNull { it.id == assignment.playerId }) ?: assignment.playerId
    var identityVisible by remember(session.sessionId, session.revealIndex) { mutableStateOf(true) }
    val reveal = remember(session.sessionId, session.revealIndex) { Animatable(0f) }
    var hasCompletedReveal by remember(session.sessionId, session.revealIndex) { mutableStateOf(false) }
    var holding by remember(session.sessionId, session.revealIndex) { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!active) {
                holding = false
                identityVisible = true
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            holding = false
            lifecycle.removeObserver(observer)
        }
    }
    val canProceed = hasCompletedReveal
    val thermalFeedback = remember { platformThermalFeedback() }
    val scope = rememberCoroutineScope()

    val card: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = .08f))
                .pointerInput(session.sessionId, session.revealIndex, viewportSize, active) {
                    if (!active) return@pointerInput
                    var freezeJob: Job? = null
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        freezeJob?.cancel()
                        holding = true
                        thermalFeedback.start()
                        val thaw = scope.launch {
                            launch {
                                delay(333)
                                hasCompletedReveal = true
                            }
                            reveal.animateTo(1f, tween(1_000, easing = LinearEasing))
                        }
                        val pulseJob = scope.launch {
                            while (isActive) {
                                delay(120)
                                thermalFeedback.pulse()
                            }
                        }
                        try {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed || change.isOutOfBounds(size, extendedTouchPadding)) break
                                // Keep small finger movements from starting the parent scroll.
                                change.consume()
                                if (!change.pressed) break
                            }
                        } finally {
                            holding = false
                            pulseJob.cancel()
                            thaw.cancel()
                            thermalFeedback.complete()
                            freezeJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                reveal.animateTo(0f, tween(380, easing = FastOutLinearInEasing))
                            }
                        }
                    }
                },
            Alignment.Center,
        ) {
            if (canShowRevealSecret(holding, active, identityVisible)) Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(28.dp)
                    .blur((10.dp * (1f - reveal.value)).coerceAtLeast(0.dp)),
            ) {
                Text(
                    session.prompt.revealText(assignment.role, session.hintsEnabled),
                    color = Color.White,
                    fontSize = 34.sp,
                )
                Text(
                    if (assignment.role == Role.AGENT) {
                        stringResource(Res.string.role_agent)
                    } else {
                        stringResource(Res.string.role_impostor)
                    },
                    color = if (assignment.role == Role.IMPOSTOR) Color(0xFFFF758F) else Cyan,
                    fontSize = 18.sp,
                    letterSpacing = 3.sp,
                )
            }
            FrostGlass(
                reveal = if (canShowRevealSecret(holding, active, identityVisible)) reveal.value else 0f,
            )
            if (identityVisible) {
                Text(
                    stringResource(Res.string.reveal_identity, playerName),
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
    val instructionsAndAction: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(Res.string.reveal_instructions),
            color = Frost.copy(alpha = .78f),
            fontSize = 12.sp,
            letterSpacing = 1.6.sp,
        )
        FrostButton(
            text = stringResource(
                if (session.revealIndex == session.assignments.lastIndex) {
                    Res.string.begin_round
                } else {
                    Res.string.next_player
                },
            ),
            onClick = {
                holding = false
                identityVisible = true
                val next = com.impostor.domain.RevealNextPlayerUseCase().invoke(session, canProceed)
                if (canProceed && next == session) onRoute(Route.GameStart(session)) else onRoute(Route.Reveal(next))
            },
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth(),
        )
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().onSizeChanged {
            if (viewportSize != it) {
                holding = false
                identityVisible = true
                viewportSize = it
            }
        }.pointerInput(session.sessionId, session.revealIndex) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed && !it.previousPressed }) identityVisible = false
                }
            }
        }.padding(horizontal = 24.dp),
    ) {
        val wide = hasTwoPanes(maxWidth.value, minPaneWidth = 280f, horizontalGaps = 24f)
        val cardHeight = (maxHeight - 48.dp - 160.dp).coerceIn(180.dp, 420.dp)
        val homeVerticalPadding = if (maxHeight < 740.dp) 8.dp else 20.dp
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(top = 24.dp, bottom = if (wide) 24.dp else homeVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(playerName, color = Frost, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                FrostCloseButton(onClick = {
                    holding = false
                    identityVisible = true
                    onRoute(Route.Home)
                })
            }
            RevealProgress(current = session.revealIndex + 1, total = session.assignments.size)
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    card(Modifier.weight(1f).height(cardHeight))
                    instructionsAndAction(Modifier.weight(1f))
                }
            } else {
                card(Modifier.fillMaxWidth().height(cardHeight))
                Spacer(Modifier.weight(1f))
                instructionsAndAction(Modifier.fillMaxWidth())
                // Reserve the home footer's measured height, including localized text.
                Box(Modifier.padding(top = 2.dp).alpha(0f).clearAndSetSemantics {}) {
                    FrostButton(
                        text = stringResource(Res.string.unlock_full_version),
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        style = FrostButtonStyle.Subtle,
                        enabled = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun localizedPlayerName(player: Player?): String? {
    player ?: return null
    val index = player.id.removePrefix("p").toIntOrNull() ?: return player.displayName
    val localizedDefault = stringResource(Res.string.default_player, index)
    val legacyPolishDefault = stringResource(Res.string.legacy_polish_default_player, index)
    val englishDefault = stringResource(Res.string.english_default_player, index)
    return if (player.displayName == localizedDefault ||
        player.displayName == legacyPolishDefault ||
        player.displayName == englishDefault ||
        player.displayName == defaultPlayerToken(index)
    ) {
        localizedDefault
    } else {
        player.displayName
    }
}

@Composable
private fun RevealProgress(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index < current) Cyan else Color.White.copy(alpha = .16f)),
            )
        }
        Text(
            "$current/$total",
            color = Frost.copy(alpha = .72f),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun FrostGlass(
    reveal: Float,
) {
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val thaw = reveal.coerceIn(0f, 1f)
        val frostColor = Color(0xFFEAF5FF)
        val lineColor = Color(0xFFCFEAFF).copy(alpha = .42f)
        drawRoundRect(
            brush = Brush.radialGradient(
                listOf(
                    frostColor.copy(alpha = .98f),
                    Color(0xFFC2DDF1).copy(alpha = .94f),
                    Color(0xFF6D92BF).copy(alpha = .9f),
                    Color(0xFF39568A).copy(alpha = .94f),
                ),
                center = Offset(size.width * .3f, size.height * .22f),
                radius = size.maxDimension,
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * .055f),
        )

        val cracks = listOf(
            androidx.compose.ui.geometry.Offset(.10f, .22f) to androidx.compose.ui.geometry.Offset(.42f, .38f),
            androidx.compose.ui.geometry.Offset(.92f, .28f) to androidx.compose.ui.geometry.Offset(.66f, .44f),
            androidx.compose.ui.geometry.Offset(.16f, .78f) to androidx.compose.ui.geometry.Offset(.38f, .62f),
            androidx.compose.ui.geometry.Offset(.82f, .88f) to androidx.compose.ui.geometry.Offset(.62f, .66f),
        )
        cracks.forEach { (start, end) ->
            drawLine(
                lineColor,
                androidx.compose.ui.geometry.Offset(size.width * start.x, size.height * start.y),
                androidx.compose.ui.geometry.Offset(size.width * end.x, size.height * end.y),
                strokeWidth = 2.5f,
            )
        }

        listOf(.16f to .16f, .78f to .2f, .3f to .67f, .76f to .75f).forEach { (x, y) ->
            drawCircle(
                color = frostColor.copy(alpha = .2f),
                radius = size.minDimension * .025f,
                center = Offset(size.width * x, size.height * y),
            )
        }
        val thawPoints = listOf(
            Triple(.5f, .5f, 0f),
            Triple(.18f, .3f, .18f),
            Triple(.8f, .32f, .3f),
            Triple(.3f, .8f, .42f),
            Triple(.76f, .74f, .54f),
        )
        thawPoints.forEach { (x, y, delay) ->
            val progress = ((thaw - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (progress == 0f) return@forEach
            val radius = progress * size.maxDimension * if (delay == 0f) .72f else .36f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        .72f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    center = Offset(size.width * x, size.height * y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(size.width * x, size.height * y),
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

@Composable private fun GameStartScreen(
    session: GameSession,
    players: List<Player>,
    onCompleted: suspend () -> Unit,
): Unit {
    var hasCompleted by remember(session.sessionId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val starterName = localizedPlayerName(players.firstOrNull { it.id == session.starterPlayerId })
        ?: session.starterPlayerId
    val impostorCount = session.assignments.count { it.role == Role.IMPOSTOR }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.game_started), color = Frost.copy(alpha = .78f), fontSize = 13.sp, letterSpacing = 6.sp)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(Res.string.game_starter), color = Color(0xFF6DCBFF), fontSize = 27.sp)
        Spacer(Modifier.height(24.dp))
        val starterCardShape = RoundedCornerShape(34.dp)
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                .clip(starterCardShape)
                .background(
                    Brush.verticalGradient(
                        listOf(glassOpacity(Color(0xFFB8CEE0).copy(alpha = .40f)), PanelBottom),
                    ),
                )
                .border(1.dp, glassOpacity(Color(0xFFD4F3FF).copy(alpha = .58f)), starterCardShape)
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("❄", color = Frost, fontSize = 72.sp)
            Spacer(Modifier.height(28.dp))
            Text(starterName, color = Color(0xFFF4FAFF), fontSize = 40.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "${stringResource(Res.string.game_start_hidden_impostor)} ",
            color = Color(0xFF64B9E8),
            fontSize = 18.sp,
        )
        Text(
            stringResource(
                if (impostorCount == 1) Res.string.game_start_impostor_singular
                else Res.string.game_start_impostor_plural,
                impostorCount,
            ),
            color = Frost,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(36.dp))
        FrostButton(
            text = stringResource(Res.string.end_game),
            onClick = {
                if (!hasCompleted) {
                    hasCompleted = true
                    scope.launch { onCompleted() }
                }
            },
            enabled = !hasCompleted,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
