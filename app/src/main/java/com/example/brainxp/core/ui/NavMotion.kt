package com.example.brainxp.core.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

const val TOWARDS_END = 1
const val TOWARDS_START = -1
const val NAV_MOTION_MILLIS = 380

enum class NavMotion {
    NONE,
    PUSH,
    FLOW,
}

enum class SlideAxis {
    HORIZONTAL,
    VERTICAL,
    NONE,
}

data class SlideSpec(
    val axis: SlideAxis,
    val sign: Int,
)

private val HELD = SlideSpec(SlideAxis.NONE, 0)

val BAR_SLIDE = SlideSpec(SlideAxis.VERTICAL, TOWARDS_END)

private val HOUSE_EASING = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private const val ABOVE = 1f
private const val BELOW = -1f

private fun <T> motionTween() = tween<T>(durationMillis = NAV_MOTION_MILLIS, easing = HOUSE_EASING)

fun navMotionOf(
    fromTab: Int?,
    toTab: Int?,
    fromFlow: Boolean,
    toFlow: Boolean,
): NavMotion =
    when {
        toFlow || fromFlow -> NavMotion.FLOW
        toTab != null && fromTab != null -> NavMotion.NONE
        else -> NavMotion.PUSH
    }

fun enterSlideOf(motion: NavMotion): SlideSpec =
    when (motion) {
        NavMotion.NONE -> HELD
        NavMotion.PUSH -> SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_END)
        NavMotion.FLOW -> SlideSpec(SlideAxis.VERTICAL, TOWARDS_END)
    }

fun exitSlideOf(motion: NavMotion): SlideSpec =
    when (motion) {
        NavMotion.NONE -> HELD
        NavMotion.PUSH -> SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_START)
        NavMotion.FLOW -> HELD
    }

fun popEnterSlideOf(motion: NavMotion): SlideSpec =
    when (motion) {
        NavMotion.NONE -> HELD
        NavMotion.PUSH -> SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_START)
        NavMotion.FLOW -> HELD
    }

fun popExitSlideOf(motion: NavMotion): SlideSpec =
    when (motion) {
        NavMotion.NONE -> HELD
        NavMotion.PUSH -> SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_END)
        NavMotion.FLOW -> SlideSpec(SlideAxis.VERTICAL, TOWARDS_END)
    }

fun enterOf(slide: SlideSpec): EnterTransition =
    when (slide.axis) {
        SlideAxis.HORIZONTAL -> slideInHorizontally(motionTween()) { full -> full * slide.sign }
        SlideAxis.VERTICAL -> slideInVertically(motionTween()) { full -> full * slide.sign }
        SlideAxis.NONE -> EnterTransition.None
    }

fun exitOf(slide: SlideSpec): ExitTransition =
    when (slide.axis) {
        SlideAxis.HORIZONTAL -> slideOutHorizontally(motionTween()) { full -> full * slide.sign }
        SlideAxis.VERTICAL -> slideOutVertically(motionTween()) { full -> full * slide.sign }
        SlideAxis.NONE -> ExitTransition.None
    }

fun forwardOf(motion: NavMotion): ContentTransform =
    ContentTransform(
        targetContentEnter = enterOf(enterSlideOf(motion)),
        initialContentExit = exitOf(exitSlideOf(motion)),
        targetContentZIndex = ABOVE,
        sizeTransform = null,
    )

fun backwardOf(motion: NavMotion): ContentTransform =
    ContentTransform(
        targetContentEnter = enterOf(popEnterSlideOf(motion)),
        initialContentExit = exitOf(popExitSlideOf(motion)),
        targetContentZIndex = BELOW,
        sizeTransform = null,
    )
