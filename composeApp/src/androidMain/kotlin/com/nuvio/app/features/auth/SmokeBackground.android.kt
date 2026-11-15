package com.nuvio.app.features.auth

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ShaderBrush

private const val SMOKE_AGSL_SHADER = """
uniform float2 uRes;
uniform float uTime;
uniform float2 uMouse;

float hash(float2 p) {
    p = fract(p * float2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
        mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    float2x2 r = float2x2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = r * p * 2.02 + float2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 res = max(uRes, float2(1.0, 1.0));
    float2 uv = float2(fragCoord.x / res.x, 1.0 - (fragCoord.y / res.y));
    float ar = res.x / res.y;
    float2 p = float2(uv.x * ar, uv.y) * 1.25;
    float t = uTime * 0.07;

    // Pointer stirs a gentle curl eddy into the smoke
    float2 mousePos = float2(uMouse.x * ar, 1.0 - uMouse.y) * 1.25;
    float2 d = p - mousePos;
    float k = exp(-dot(d, d) * 1.6);
    p += float2(-d.y, d.x) * k * 0.55;

    // Two-pass domain warping for billowing curls
    float2 q = float2(fbm(p + float2(0.0, t)), fbm(p + float2(5.2, 1.3) - t * 0.8));
    float2 r = float2(
        fbm(p + 3.0 * q + float2(1.7, 9.2) + t * 1.2),
        fbm(p + 3.0 * q + float2(8.3, 2.8) - t)
    );
    float f = fbm(p + 2.6 * r + float2(t * 0.4, -t * 0.3));

    float smoke = smoothstep(0.30, 0.85, f);
    smoke *= smoke;
    float wisp = smoothstep(0.55, 0.75, f) * (1.0 - smoothstep(0.75, 0.95, f));
    float vein = (1.0 - smoothstep(0.0, 0.048, abs(f - 0.58))) * smoothstep(0.38, 0.64, r.y);
    float deepVein = (1.0 - smoothstep(0.0, 0.14, abs(f - 0.58))) * smoothstep(0.34, 0.66, r.y);

    // Near-black obsidian paper + dark charcoal smoke + subtle strong royal/electric blue veins
    float3 night = mix(float3(0.022, 0.026, 0.038), float3(0.032, 0.040, 0.062), uv.y);
    float3 smokeBody = float3(0.18, 0.23, 0.33);
    float3 cobaltGlow = float3(0.07, 0.22, 0.58);
    float3 strongBlue = float3(0.16, 0.44, 0.96);
    float3 iceBlueCore = float3(0.38, 0.68, 1.00);

    float3 col = night
        + smokeBody * smoke * 0.72
        + float3(0.24, 0.42, 0.76) * wisp * 0.16
        + cobaltGlow * deepVein * 0.36
        + strongBlue * vein * 0.88
        + iceBlueCore * vein * vein * 0.26;

    col += (hash(fragCoord + fract(t)) - 0.5) / 255.0;
    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""

@Composable
internal actual fun SmokeShaderSurface(
    time: Float,
    pointerNormalized: Offset,
    modifier: Modifier,
) {
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                RuntimeShader(SMOKE_AGSL_SHADER).also { shader ->
                    shader.setFloatUniform("uRes", 128f, 128f)
                    shader.setFloatUniform("uTime", 40f)
                    shader.setFloatUniform("uMouse", 0.68f, 0.42f)
                }
            }.getOrNull()
        } else {
            null
        }
    }

    if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width.coerceAtLeast(1f)
            val height = size.height.coerceAtLeast(1f)
            runtimeShader.setFloatUniform("uRes", width, height)
            runtimeShader.setFloatUniform("uTime", time)
            runtimeShader.setFloatUniform("uMouse", pointerNormalized.x, pointerNormalized.y)
            drawRect(brush = ShaderBrush(runtimeShader))
        }
    } else {
        SmokeFallbackCanvas(
            time = time,
            pointerNormalized = pointerNormalized,
            modifier = modifier,
        )
    }
}
