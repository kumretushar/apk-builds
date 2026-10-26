package com.gmtusharx.friday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gmtusharx.friday.model.OrbState
import com.gmtusharx.friday.ui.theme.NeonCyan
import com.gmtusharx.friday.ui.theme.NeonMagenta
import com.gmtusharx.friday.ui.theme.NeonPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Glowing voice orb.
 *  Idle       -> slow breathing cyan core
 *  Listening  -> expanding ripples, core pulses with mic level
 *  Processing -> purple core with a rotating sweep arc
 *  Speaking   -> magenta core with an undulating wave ring
 */
@Composable
fun VoiceOrb(
    state: OrbState,
    level: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val t = rememberInfiniteTransition(label = "orb")
    val breathe by t.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val spin by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "spin")
    val ripple by t.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ripple")
    val wave by t.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "wave")

    val smoothLevel by animateFloatAsState(level, spring(dampingRatio = 0.7f, stiffness = 200f), label = "level")
    val primary by animateColorAsState(
        when (state) {
            OrbState.Idle, OrbState.Listening -> NeonCyan
            OrbState.Processing -> NeonPurple
            OrbState.Speaking -> NeonMagenta
        }, tween(500), label = "primary"
    )
    val secondary by animateColorAsState(
        when (state) {
            OrbState.Idle, OrbState.Listening -> NeonPurple
            OrbState.Processing -> NeonCyan
            OrbState.Speaking -> NeonCyan
        }, tween(500), label = "secondary"
    )

    Canvas(
        modifier
            .semantics { contentDescription = "Friday voice orb, tap to talk" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        val c = center
        val base = size.minDimension * 0.2f
        val grow = when (state) {
            OrbState.Idle -> 0.05f * breathe
            OrbState.Listening -> 0.06f + 0.30f * smoothLevel
            OrbState.Processing -> 0.04f * breathe
            OrbState.Speaking -> 0.05f + 0.06f * (0.5f + 0.5f * sin(wave))
        }
        val radius = base * (1f + grow)

        // Ripple rings
        if (state == OrbState.Listening || state == OrbState.Speaking) {
            repeat(3) { i ->
                val p = (ripple + i / 3f) % 1f
                drawCircle(
                    color = primary.copy(alpha = (1f - p) * 0.35f),
                    radius = radius + p * base * 1.5f,
                    center = c,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = 0.50f), secondary.copy(alpha = 0.16f), Color.Transparent),
                center = c, radius = radius * 2.4f
            ),
            radius = radius * 2.4f, center = c
        )

        // Core sphere
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.92f), primary, secondary.copy(alpha = 0.9f)),
                center = c - Offset(radius * 0.25f, radius * 0.25f), radius = radius * 1.35f
            ),
            radius = radius, center = c
        )

        // Processing: rotating sweep arc
        if (state == OrbState.Processing) {
            rotate(spin, c) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colorStops = arrayOf(0f to Color.Transparent, 0.75f to primary, 1f to secondary),
                        center = c
                    ),
                    startAngle = 0f, sweepAngle = 270f, useCenter = false,
                    topLeft = Offset(c.x - radius * 1.4f, c.y - radius * 1.4f),
                    size = Size(radius * 2.8f, radius * 2.8f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Speaking: undulating wave ring
        if (state == OrbState.Speaking) {
            val path = Path()
            val steps = 140
            val ringR = radius * 1.3f
            val amp = 9.dp.toPx() * (0.55f + 0.45f * sin(wave * 2f))
            for (i in 0..steps) {
                val a = i / steps.toFloat() * 2f * PI.toFloat()
                val r = ringR + amp * sin(a * 7f + wave)
                val x = c.x + r * cos(a)
                val y = c.y + r * sin(a)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, secondary, style = Stroke(width = 2.5.dp.toPx()))
        }
    }
}
