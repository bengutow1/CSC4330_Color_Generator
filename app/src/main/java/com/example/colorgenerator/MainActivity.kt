package com.example.colorgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.colorgenerator.ui.theme.ColorGeneratorTheme
import kotlin.random.Random

/**
 * Off-white app background and a muted slate button, chosen to sit
 * comfortably next to whatever saturated colors get generated below.
 */
private val OffWhite = Color(0xFFFAF7F2)
private val SlateButton = Color(0xFF4A5568)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorGeneratorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ColorGeneratorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Converts an HSL color (h in degrees [0,360), s/l in [0,1]) to a Compose
 * Color, following the standard algorithm referenced by MDN's hsl() docs.
 */
private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r1 + m, g1 + m, b1 + m)
}

/** Classic color-wheel hue-harmony rules to pick from each generation. */
private enum class HueHarmony {
    /** Evenly spaced 120° apart. */
    TRIADIC,

    /** Base plus the two hues flanking its complement (+150°/+210°). */
    SPLIT_COMPLEMENTARY,

    /** Base, +90°, and its direct complement (+180°) -- three corners of a square. */
    SQUARE,

    /** Base and a nearby analogous hue, with the third landing exactly
     *  halfway between them in hue, saturation, AND lightness. */
    ANALOGOUS_BRIDGE
}

/**
 * Generates three colors using a randomly chosen hue-harmony rule so
 * repeated taps don't all look like the same "vivid triad" shape.
 *
 * Hue alone repeating at fixed saturation/lightness tends to look same-y
 * (every set reads as "medium vivid"), so most schemes also step lightness
 * up/down from the base to make one color a tint and one a shade, with
 * saturation nudged up at both extremes -- near white or black, HSL's
 * saturation channel visually washes out/muddies unless boosted to
 * compensate, a standard tint/shade-generation technique. ANALOGOUS_BRIDGE
 * instead makes its third color the literal midpoint of the other two
 * across all three channels, so it visibly bridges them.
 */
private fun generateHarmoniousColors(): List<Color> {
    val baseHue = Random.nextFloat() * 360f
    val baseSaturation = Random.nextFloat() * 0.25f + 0.55f // [0.55, 0.80]
    val baseLightness = Random.nextFloat() * 0.20f + 0.42f  // [0.42, 0.62]

    val lightnessStep = 0.16f
    val saturationBoost = 0.12f

    fun tint(l: Float) = (l + lightnessStep).coerceAtMost(0.82f)
    fun shade(l: Float) = (l - lightnessStep).coerceAtLeast(0.22f)
    fun boost(s: Float) = (s + saturationBoost).coerceAtMost(0.95f)

    // (hue, saturation, lightness) triples for each of the three colors.
    val hsl: List<Triple<Float, Float, Float>> = when (HueHarmony.entries.random()) {
        HueHarmony.TRIADIC -> listOf(
            Triple(baseHue, baseSaturation, baseLightness),
            Triple(baseHue + 120f, boost(baseSaturation), tint(baseLightness)),
            Triple(baseHue + 240f, boost(baseSaturation), shade(baseLightness))
        )

        HueHarmony.SPLIT_COMPLEMENTARY -> listOf(
            Triple(baseHue, baseSaturation, baseLightness),
            Triple(baseHue + 150f, boost(baseSaturation), tint(baseLightness)),
            Triple(baseHue + 210f, boost(baseSaturation), shade(baseLightness))
        )

        HueHarmony.SQUARE -> listOf(
            Triple(baseHue, baseSaturation, baseLightness),
            Triple(baseHue + 90f, boost(baseSaturation), tint(baseLightness)),
            Triple(baseHue + 180f, boost(baseSaturation), shade(baseLightness))
        )

        HueHarmony.ANALOGOUS_BRIDGE -> {
            val spread = Random.nextFloat() * 30f + 40f // 40°-70° apart
            val hue2 = baseHue + spread
            val saturation2 = boost(baseSaturation)
            val lightness2 = tint(baseLightness)
            listOf(
                Triple(baseHue, baseSaturation, baseLightness),
                Triple(hue2, saturation2, lightness2),
                Triple(
                    baseHue + spread / 2f,
                    (baseSaturation + saturation2) / 2f,
                    (baseLightness + lightness2) / 2f
                )
            )
        }
    }

    return hsl.map { (h, s, l) -> hslToColor(h, s, l) }
}

@Composable
fun ColorGeneratorScreen(modifier: Modifier = Modifier) {
    var colors by remember { mutableStateOf<List<Color>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { colors = generateHarmoniousColors() },
            colors = ButtonDefaults.buttonColors(containerColor = SlateButton),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(
                text = "Generate Colors",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) { index ->
                val boxColor = colors.getOrNull(index)
                val squareModifier = Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))

                if (boxColor != null) {
                    Box(modifier = squareModifier.background(boxColor))
                } else {
                    Box(
                        modifier = squareModifier
                            .background(OffWhite)
                            .border(
                                width = 1.5.dp,
                                color = SlateButton.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorGeneratorScreenPreview() {
    ColorGeneratorTheme {
        ColorGeneratorScreen()
    }
}
