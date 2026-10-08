package com.hustlewithvikram.icamera.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Reusable camera-style mode rail.
 *
 * One state drives selection visuals; LazyRow exclusively owns horizontal
 * scrolling. This prevents nested content transitions from fighting scrolling.
 */
@Composable
fun <T> SmoothModeRail(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    onShowAllModes: (() -> Unit)? = null,
    itemWidth: Dp = 78.dp,
    itemSpacing: Dp = 6.dp,
    itemHeight: Dp = 40.dp
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelected by rememberUpdatedState(onSelected)
    var programmaticSelection by remember { mutableStateOf(false) }
    var centerJob by remember { mutableStateOf<Job?>(null) }

    fun centeredIndex(): Int? {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return null
        val center = (
            listState.layoutInfo.viewportStartOffset +
                listState.layoutInfo.viewportEndOffset
            ) / 2f
        return visible.minByOrNull {
            abs((it.offset + it.size / 2f) - center)
        }?.index
    }

    suspend fun animateToCenter(index: Int) {
        if (index !in items.indices) return

        listState.animateScrollToItem(index)

        val target = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?: return

        val viewportCenter = (
            listState.layoutInfo.viewportStartOffset +
                listState.layoutInfo.viewportEndOffset
            ) / 2f
        val targetCenter = target.offset + target.size / 2f
        val correction = targetCenter - viewportCenter

        if (abs(correction) > with(density) { 0.5.dp.toPx() }) {
            listState.animateScrollBy(
                value = correction,
                animationSpec = tween(90, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(items, selected) {
        val index = items.indexOf(selected)
        if (index < 0 || programmaticSelection) return@LaunchedEffect

        if (centeredIndex() != index) {
            centerJob?.cancel()
            centerJob = scope.launch {
                programmaticSelection = true
                try {
                    animateToCenter(index)
                } finally {
                    programmaticSelection = false
                }
            }
        }
    }

    LaunchedEffect(listState, items) {
        snapshotFlow {
            if (listState.isScrollInProgress) null else centeredIndex()
        }.collect { index ->
            if (
                index != null &&
                index in items.indices &&
                !programmaticSelection &&
                items[index] != latestSelected
            ) {
                latestOnSelected(items[index])
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val sidePadding = (maxWidth - itemWidth) / 2f

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            contentPadding = PaddingValues(horizontal = sidePadding)
        ) {
            items(items = items, key = { label(it) }) { item ->
                val active = item == selected
                val transition = updateTransition(
                    targetState = active,
                    label = "modeItem:${label(item)}"
                )

                val scale by transition.animateFloat(
                    transitionSpec = {
                        spring(
                            dampingRatio = 0.92f,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "scale"
                ) { if (it) 1f else 0.94f }

                val alpha by transition.animateFloat(
                    transitionSpec = { tween(100, easing = FastOutSlowInEasing) },
                    label = "alpha"
                ) { if (it) 1f else 0.62f }

                val containerAlpha by transition.animateFloat(
                    transitionSpec = { tween(150, easing = FastOutSlowInEasing) },
                    label = "containerAlpha"
                ) { if (it) 0.34f else 0f }

                val textColor by transition.animateColor(
                    transitionSpec = { tween(150, easing = FastOutSlowInEasing) },
                    label = "textColor"
                ) { if (it) Color(0xFFFFD60A) else Color.White }

                val horizontalPadding by transition.animateDp(
                    transitionSpec = {
                        spring(
                            dampingRatio = 0.9f,
                            stiffness = Spring.StiffnessMedium
                        )
                    },
                    label = "horizontalPadding"
                ) { if (it) 12.dp else 9.dp }

                val shape = RoundedCornerShape(19.dp)

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clickable {
                            if (active) {
                                onShowAllModes?.invoke()
                            } else {
                                onSelected(item)
                                centerJob?.cancel()
                                centerJob = scope.launch {
                                    programmaticSelection = true
                                    try {
                                        animateToCenter(items.indexOf(item))
                                    } finally {
                                        programmaticSelection = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .height(itemHeight)
                            .background(
                                Color.White.copy(alpha = containerAlpha),
                                shape
                            ),
                        shape = shape,
                        color = Color.Transparent,
                        border = if (active) {
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                        } else null
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label(item),
                                maxLines = 1,
                                softWrap = false,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
