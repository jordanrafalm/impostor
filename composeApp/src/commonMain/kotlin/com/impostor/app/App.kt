package com.impostor.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.input.KeyboardType
import com.impostor.data.bundledCatalogRepository
import com.impostor.data.analyticsService
import com.impostor.data.SubscriptionRepositoryImpl
import com.impostor.data.TrialRepositoryImpl
import com.impostor.domain.AnalyticsEvent
import com.impostor.domain.canStartGame
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import impostor.composeapp.generated.resources.Res

private val Ink = Color(0xFF050914)
private val Navy = Color(0xFF162846)
private val Cyan = Color(0xFF00F0FF)
private val Frost = Color(0xFFBDEBFF)
private val PanelBorder = Color(0xFF7FCBFF).copy(alpha = .44f)
private val PanelTop = Color(0xFF5E9FE0).copy(alpha = .28f)
private val PanelBottom = Color(0xFF0A1A43).copy(alpha = .72f)
private sealed interface Route {
    data object Splash : Route
    data object Home : Route
    data object Players : Route
    data object Categories : Route
    data object Premium : Route
    data class Reveal(val session: GameSession) : Route
    data class GameStart(val session: GameSession) : Route
}

@Composable
fun ImpostorApp() {
    var route by remember { mutableStateOf<Route>(Route.Splash) }
    var showComet by remember { mutableStateOf(false) }
    var players by remember { mutableStateOf(loadPlayers()) }
    var selectedCategories by remember { mutableStateOf(setOf(readSelectedCategoryId() ?: "animals")) }
    var impostorCount by remember { mutableStateOf(1) }
    var hintsEnabled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<SetupValidation.Reason?>(null) }
    val repository = remember { bundledCatalogRepository() }
    val subscriptionRepository = remember { SubscriptionRepositoryImpl() }
    val trialRepository = remember { TrialRepositoryImpl() }
    val scope = rememberCoroutineScope()
    var startingGame by remember { mutableStateOf(false) }
    val analytics = remember { analyticsService() }
    val entitlement by subscriptionRepository.entitlement.collectAsState(
        com.impostor.domain.Entitlement(
            state = SubscriptionState.LOCKED,
            source = com.impostor.domain.EntitlementSource.LOCAL_CACHE,
        ),
    )
    val trialState by trialRepository.state.collectAsState()
    val categories = remember { repository.categories() }
    val allCategoryIds = remember(categories) { categories.map { it.id }.toSet() }
    val prompts = remember { repository.prompts(categories.map { it.id }.toSet()) }
    val premiumUnlocked = entitlement.unlocksAllCategories()
    val unlockedCategories = if (trialState.canStartGame(entitlement)) {
        allCategoryIds
    } else {
        emptySet()
    }

    LaunchedEffect(allCategoryIds) {
        if (selectedCategories.any { it !in allCategoryIds }) {
            selectedCategories = setOf("animals")
        }
    }

    LaunchedEffect(selectedCategories, unlockedCategories) {
        selectedCategories.singleOrNull()?.takeIf { it in unlockedCategories }?.let(::writeSelectedCategoryId)
    }

    LaunchedEffect(Unit) {
        route = Route.Home
        platformNotificationScheduler().requestPermissionAndSchedulePartyReminder()
    }
    Box(Modifier.fillMaxSize()) {
        IceBackdrop()
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
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
                        onCometLongPress = { showComet = true },
                        onStart = start@{
                            if (startingGame || route != Route.Home) return@start
                            if (!trialRepository.state.value.canStartGame(entitlement)) {
                                route = Route.Premium
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
                                            if (!premiumUnlocked) {
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
                        options = categories.map { it.displayName to it.id },
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
                            route = Route.Home
                        },
                    )
                }
            }
        }
        if (showComet) {
            CometOverlay(onDismiss = { showComet = false })
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
    players: List<Player>, impostorCount: Int, hintsEnabled: Boolean, error: SetupValidation.Reason?,
    onPlayers: () -> Unit,
    onCategories: () -> Unit,
    onCodes: () -> Unit,
    onImpostorDecrease: () -> Unit,
    onImpostorIncrease: () -> Unit,
    onHintsChanged: (Boolean) -> Unit,
    onCometLongPress: () -> Unit,
    onStart: () -> Unit,
): Unit = Column(
    modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("GRA IMPREZOWA", color = Frost.copy(alpha = .78f), fontSize = 11.sp, letterSpacing = 5.sp)
        Spacer(Modifier.height(8.dp))
        LongPressLogo(onLongPress = onCometLongPress)
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.width(120.dp).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(alpha = .8f), Color.Transparent))),
        )
        Spacer(Modifier.height(28.dp))
        HomeReveal(index = 0, height = 88.dp) {
            FrostSettingButton("Gracze", players.size.toString(), onPlayers)
        }
        Spacer(Modifier.height(8.dp))
        HomeReveal(index = 1, height = 88.dp) {
            FrostSettingButton("Kategorie", "→", onCategories)
        }
        Spacer(Modifier.height(8.dp))
        HomeReveal(index = 2, height = 72.dp) {
            FrostToggleSetting("Podpowiedź", hintsEnabled, onHintsChanged)
        }
        Spacer(Modifier.height(8.dp))
        HomeReveal(index = 3, height = 88.dp) {
            FrostImpostorSetting(impostorCount, onImpostorDecrease, onImpostorIncrease)
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Ustawienia wymagają uwagi: ${error.name.lowercase().replace('_', ' ')}",
                color = Color(0xFFFFA5B3),
                fontSize = 13.sp,
            )
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FrostButton(
            text = "G R A J",
            onClick = onStart,
            style = FrostButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        FrostButton(
            text = "♧  ODBLOKUJ PEŁNĄ WERSJĘ",
            onClick = onCodes,
            style = FrostButtonStyle.Subtle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LongPressLogo(onLongPress: () -> Unit) {
    var isHolding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(if (isHolding) 2000 else 120),
        label = "comet-long-press-progress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(onLongPress) {
            awaitEachGesture {
                awaitFirstDown()
                isHolding = true
                val timedOut = withTimeoutOrNull(2000) {
                    waitForUpOrCancellation()
                    false
                } == null
                isHolding = false
                if (timedOut) {
                    onLongPress()
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
private fun CometOverlay(onDismiss: () -> Unit) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes("files/comet.json").decodeToString())
    }
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    LaunchedEffect(compositionResult.isFailure, composition, progress) {
        if (compositionResult.isFailure) {
            onDismiss()
            return@LaunchedEffect
        }
        if (composition != null && progress >= 0.999f) {
            onDismiss()
        }
    }

    Box(
        Modifier.fillMaxSize()
            .clipToBounds()
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
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = 180f },
                contentScale = ContentScale.FillBounds,
                contentDescription = "Comet animation",
            )
        }
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
                contentDescription = "Animacja walking lady",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(280.dp),
            )
        }
    }
}

@Composable
private fun HomeReveal(index: Int, height: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) {
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
) {
    FrostButtonSurface(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        closeButton = closeButton,
        onLongPress = onLongPress,
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (style == FrostButtonStyle.Primary) Ink else Frost
            } else {
                (if (style == FrostButtonStyle.Primary) Ink else Frost).copy(alpha = .4f)
            },
            fontSize = when {
                closeButton -> 26.sp
                style == FrostButtonStyle.Primary -> 22.sp
                else -> 15.sp
            },
            letterSpacing = when {
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
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .965f else 1f, tween(120), label = "button-press")
    val sweep = remember { Animatable(-.45f) }
    val scope = rememberCoroutineScope()
    var hoverWasObserved by remember { mutableStateOf(false) }
    val longPressAction = onLongPress

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

    val shape = RoundedCornerShape(if (closeButton) 18.dp else if (style == FrostButtonStyle.Subtle) 28.dp else 24.dp)
    val background = when (style) {
        FrostButtonStyle.Primary -> Brush.verticalGradient(listOf(Color(0xFF84D7FF), Color(0xFF3D9BDF)))
        FrostButtonStyle.Glass -> Brush.verticalGradient(listOf(PanelTop, PanelBottom))
        FrostButtonStyle.Subtle -> Brush.horizontalGradient(listOf(PanelTop.copy(alpha = .55f), PanelBottom))
    }
    val textColor = if (style == FrostButtonStyle.Primary) Ink else Frost

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, if (style == FrostButtonStyle.Primary) Color(0xFFD5F5FF) else PanelBorder, shape)
            .hoverable(interactionSource, enabled)
            .then(if (longPressAction == null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                ) {
                    runSweep()
                    onClick()
                }
            } else {
                Modifier.pointerInput(longPressAction, enabled) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val completedBeforeLongPress = withTimeoutOrNull(2_000) {
                            waitForUpOrCancellation()
                            true
                        } == true
                        if (completedBeforeLongPress) {
                            if (enabled) {
                                runSweep()
                                onClick()
                            }
                        } else {
                            longPressAction()
                            waitForUpOrCancellation()
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
) {
    FrostButtonSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(88.dp),
        style = FrostButtonStyle.Glass,
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
                    .background(Color(0xFF65BFF5).copy(alpha = .18f))
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
        modifier = Modifier.fillMaxWidth().height(72.dp)
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
        modifier = Modifier.fillMaxWidth().height(88.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PanelTop, PanelBottom)))
            .border(1.dp, PanelBorder, shape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Impostorzy", color = Color(0xFFF2FAFF), fontSize = 21.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FrostButton(text = "−", onClick = onDecrease, modifier = Modifier.width(54.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF65BFF5).copy(alpha = .18f))
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
    var newPlayerName by remember { mutableStateOf("") }
    var clearedDefaultPlayerIds by remember { mutableStateOf(emptySet<String>()) }

    Column(
        Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            FrostButton(
                text = "‹",
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).size(52.dp),
                closeButton = true,
            )
            Text(
                "Gracze",
                color = Color.White,
                fontSize = 28.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                players.size.toString(),
                color = Frost,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Spacer(Modifier.height(18.dp))
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
                            Text("Wpisz imię gracza...", color = Frost.copy(alpha = .58f), fontSize = 17.sp)
                        }
                        field()
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            FrostButton(
                text = "+",
                onClick = {
                    val displayName = normalizePlayerName(newPlayerName).ifEmpty { "Gracz ${players.size + 1}" }
                    onPlayers(players + Player("p${players.size + 1}", displayName))
                    newPlayerName = ""
                },
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
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
                        .background(Color(0xFF65BFF5).copy(alpha = .18f))
                        .border(1.dp, PanelBorder, RoundedCornerShape(13.dp)),
                ) {
                    Text("${index + 1}", color = Frost, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = player.displayName,
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
                            player.displayName == "Gracz ${index + 1}"
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
    Spacer(Modifier.height(16.dp))
    FrostButton(text = "GOTOWE", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = FrostButtonStyle.Subtle)
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
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "KATEGORIE",
            color = Color.White,
            fontSize = 28.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        FrostCloseButton(onClick = onBack)
    }
    Spacer(Modifier.height(18.dp))
    Text(
        if (premiumUnlocked) "Premium odblokowuje wszystkie kategorie."
        else if (trialState.hasFreeCategories)
            "Wszystkie kategorie odblokowane. Pozostało darmowych gier: ${trialState.remainingFreeGames}."
        else "Wykorzystano 3 darmowe gry. Subskrypcja Premium odblokuje wszystkie kategorie.",
        color = Frost,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(18.dp))
    if ((showPremiumPrompt || !trialState.hasFreeCategories) && !premiumUnlocked) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF16365A).copy(alpha = .86f))
                .border(1.dp, Cyan.copy(alpha = .72f), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Ta kategoria wymaga Premium.", color = Color.White, fontSize = 16.sp)
            FrostButton(
                text = "ODBLOKUJ PREMIUM",
                onClick = onOpenPremium,
                style = FrostButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
    }
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.chunked(2).forEach { categoryRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryRow.forEach { (name, id) ->
                    val isUnlocked = id in unlocked
                    FrostButton(
                        text = if (!isUnlocked) "🔒  $name" else if (id in selected) "✓  $name" else name,
                        onClick = {
                            if (isUnlocked) {
                                onSelected(setOf(id))
                            } else {
                                showPremiumPrompt = true
                            }
                        },
                        onLongPress = if (id == "adult") ({ showWalkingLady = true }) else null,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (categoryRow.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
        Spacer(Modifier.height(16.dp))
        FrostButton(text = "GOTOWE", onClick = onBack, modifier = Modifier.fillMaxWidth(), style = FrostButtonStyle.Subtle)
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
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val purchase = remember(repository) { PurchasePremiumUseCase(repository) }
    val restore = remember(repository) { RestorePurchasesUseCase(repository) }

    LaunchedEffect(Unit) {
        analytics.log(AnalyticsEvent.PREMIUM_SCREEN_OPENED)
        repository.loadProduct()
            .onSuccess {
                product = it
                analytics.log(AnalyticsEvent.SUBSCRIPTION_PRODUCT_LOADED)
            }
            .onFailure { message = subscriptionMessage(it) }
        // Restore is store-account authenticated (AppStore.sync() on iOS) and must stay user-initiated only.
    }
    LaunchedEffect(entitlement.state, entitlement.source) {
        analytics.log(AnalyticsEvent.PREMIUM_ENTITLEMENT_CHANGED, mapOf("state" to entitlement.state.name.lowercase()))
    }

    Shell(
        title = "PREMIUM",
        bottomContent = {
            FrostButton(
                text = "ZAMKNIJ",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                style = FrostButtonStyle.Subtle,
            )
        },
    ) {
        Text(product?.title ?: "Impostor Premium", color = Color.White, fontSize = 22.sp)
        Text(product?.let { "${it.formattedPrice} / ${it.periodLabel}" } ?: "Subskrypcja jest obecnie niedostępna", color = Frost)
        Text(entitlement.state.name.lowercase().replace('_', ' '), color = if (entitlement.state == SubscriptionState.ACTIVE) Cyan else Frost)
        if (message != null) Text(message!!, color = Color(0xFFFF8A8A))
        FrostButton(
            text = "SUBSKRYBUJ",
            onClick = {
                analytics.log(AnalyticsEvent.SUBSCRIPTION_PURCHASE_STARTED)
                message = null
                scope.launch {
                    purchase.invoke()
                        .onFailure { message = subscriptionMessage(it) }
                        .onSuccess { analytics.log(AnalyticsEvent.SUBSCRIPTION_PURCHASE_RESULT, mapOf("result" to purchaseResult(it.state))) }
                }
            },
            enabled = product != null && entitlement.state != SubscriptionState.PURCHASING && entitlement.state != SubscriptionState.RESTORING,
            modifier = Modifier.fillMaxWidth(),
        )
        FrostButton(
            text = "PRZYWRÓĆ ZAKUPY",
            onClick = {
                message = null
                scope.launch {
                    restore.invoke()
                        .onFailure {
                            message = subscriptionMessage(it)
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
            text = "ZARZĄDZAJ SUBSKRYPCJĄ",
            onClick = { scope.launch { repository.openManageSubscriptions().onFailure { message = subscriptionMessage(it) } } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun purchaseResult(state: SubscriptionState): String = when (state) {
    SubscriptionState.ACTIVE -> "success"
    SubscriptionState.PURCHASING -> "pending"
    else -> "failed"
}

private fun subscriptionMessage(error: Throwable): String = when ((error as? SubscriptionException)?.error) {
    SubscriptionError.PURCHASE_PENDING -> "Zakup oczekuje na potwierdzenie."
    SubscriptionError.PURCHASE_CANCELLED -> "Zakup został anulowany."
    SubscriptionError.ALREADY_OWNED -> "Subskrypcja jest już aktywna. Przywróć zakupy."
    SubscriptionError.PRODUCT_UNAVAILABLE, SubscriptionError.STORE_UNAVAILABLE -> "Sklep jest obecnie niedostępny."
    SubscriptionError.RESTORE_FAILED -> "Nie udało się przywrócić zakupów."
    SubscriptionError.PURCHASE_FAILED, null -> "Zakup nie powiódł się."
}

@Composable private fun RevealScreen(
    session: GameSession,
    players: List<Player>,
    onRoute: (Route) -> Unit,
) {
    val assignment = session.assignments[session.revealIndex]
    val reveal = remember(session.revealIndex) { Animatable(0f) }
    var hasStartedReveal by remember(session.revealIndex) { mutableStateOf(false) }
    val canProceed = hasStartedReveal
    val haptics = LocalHapticFeedback.current
    val thermalFeedback = remember { platformThermalFeedback() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                players.firstOrNull { it.id == assignment.playerId }?.displayName ?: assignment.playerId,
                color = Frost,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f),
            )
            FrostCloseButton(onClick = { onRoute(Route.Home) })
        }
        RevealProgress(
            current = session.revealIndex + 1,
            total = session.assignments.size,
        )
        Text(
            "PRZYTRZYMAJ EKRAN ABY ODSŁONIĆ HASŁO I PRZEKAŻ TELEFON KOLEJNEJ OSOBIE",
            color = Frost.copy(alpha = .78f),
            fontSize = 12.sp,
            letterSpacing = 1.6.sp,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = .08f))
                .pointerInput(session.revealIndex) {
                    awaitEachGesture {
                        awaitFirstDown()
                        hasStartedReveal = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        thermalFeedback.start()
                        val thaw = scope.launch {
                            reveal.animateTo(1f, tween(1_000, easing = LinearEasing))
                        }
                        val pulseJob = scope.launch {
                            while (isActive) {
                                delay(120)
                                thermalFeedback.pulse()
                            }
                        }
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            pulseJob.cancel()
                            thaw.cancel()
                            thermalFeedback.complete()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                reveal.animateTo(0f, tween(380, easing = FastOutLinearInEasing))
                            }
                        }
                    }
                },
            Alignment.Center,
        ) {
            Column(
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
                    if (assignment.role == Role.AGENT) "CYWIL" else "IMPOSTOR",
                    color = if (assignment.role == Role.IMPOSTOR) Color(0xFFFF758F) else Cyan,
                    fontSize = 18.sp,
                    letterSpacing = 3.sp,
                )
            }
            FrostGlass(
                reveal = reveal.value,
            )
        }
        FrostButton(
            text = if (session.revealIndex == session.assignments.lastIndex) "ROZPOCZNIJ RUNDĘ" else "NASTĘPNY GRACZ",
            onClick = {
                val next = com.impostor.domain.RevealNextPlayerUseCase().invoke(session, canProceed)
                if (canProceed && next == session) onRoute(Route.GameStart(session)) else onRoute(Route.Reveal(next))
            },
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth(),
        )
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

        listOf(.18f to .16f, .33f to .25f, .58f to .18f, .76f to .31f).forEach { (x, length) ->
            drawRoundRect(
                color = frostColor.copy(alpha = .32f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * x, size.height * (.72f + x * .12f)),
                size = androidx.compose.ui.geometry.Size(size.minDimension * .025f, size.height * length),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * .02f),
            )
        }
        val radius = (thaw * .95f - .2f) * size.maxDimension * .75f
        if (radius > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        .82f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
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
    val starterName = players.firstOrNull { it.id == session.starterPlayerId }?.displayName
        ?: session.starterPlayerId
    val impostorCount = session.assignments.count { it.role == Role.IMPOSTOR }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("GRA ROZPOCZĘTA", color = Frost.copy(alpha = .78f), fontSize = 13.sp, letterSpacing = 6.sp)
        Spacer(Modifier.height(56.dp))
        Text("Grę zaczyna", color = Color(0xFF6DCBFF), fontSize = 27.sp)
        Spacer(Modifier.height(44.dp))
        val starterCardShape = RoundedCornerShape(34.dp)
        Column(
            modifier = Modifier.fillMaxWidth().height(396.dp)
                .clip(starterCardShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFE3F4FF).copy(alpha = .40f), PanelBottom)))
                .border(1.dp, Color(0xFFD4F3FF).copy(alpha = .58f), starterCardShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("❄", color = Frost, fontSize = 72.sp)
            Spacer(Modifier.height(28.dp))
            Text(starterName, color = Color(0xFFF4FAFF), fontSize = 50.sp)
        }
        Spacer(Modifier.height(48.dp))
        Text(
            "Wśród graczy ukrywa się ",
            color = Color(0xFF64B9E8),
            fontSize = 18.sp,
        )
        Text(
            if (impostorCount == 1) "1 impostor. Powodzenia!" else "$impostorCount impostorów. Powodzenia!",
            color = Frost,
            fontSize = 18.sp,
        )
        Spacer(Modifier.weight(1f, fill = true).height(36.dp))
        FrostButton(
            text = "K O N I E C   G R Y",
            onClick = {
                if (!hasCompleted) {
                    hasCompleted = true
                    scope.launch { onCompleted() }
                }
            },
            enabled = !hasCompleted,
            style = FrostButtonStyle.Subtle,
            modifier = Modifier.fillMaxWidth().height(76.dp),
        )
    }
}
