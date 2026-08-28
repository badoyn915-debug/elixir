#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform float uTime;
uniform vec2 uResolution;
uniform vec2 uMouseFov;

out vec4 fragColor;

// =========================================================
// ULTRA-FAST HIGH-FPS PROCEDURAL LIQUID MARBLE SHADER
// =========================================================

#define SPEED           0.10
#define SCALE           1.80
#define CONTRAST        1.20
#define BRIGHTNESS      0.035

// Fast 2D noise with polynomial smoothing
float hash2(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float fastNoise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n00 = hash2(i);
    float n10 = hash2(i + vec2(1.0, 0.0));
    float n01 = hash2(i + vec2(0.0, 1.0));
    float n11 = hash2(i + vec2(1.0, 1.0));

    return mix(mix(n00, n10, f.x), mix(n01, n11, f.x), f.y);
}

// Fast 2-octave FBM with constant rotation matrix
float fastFbm2(vec2 p) {
    const mat2 m = mat2(0.80, 0.60, -0.60, 0.80);
    return fastNoise2D(p) * 0.62 + fastNoise2D(m * p * 2.02 + vec2(0.13, 0.27)) * 0.38;
}

void main() {
    vec2 aspect = vec2(uResolution.x / max(uResolution.y, 1.0), 1.0);
    vec2 uv = (texCoord - 0.5) * aspect * SCALE;

    // Smooth Jitter-Free Parallax
    vec2 parallax = uMouseFov * 0.16;
    uv += parallax * (0.90 + 0.20 * length(texCoord - 0.5));

    float t = uTime * SPEED;

    // High-performance 2-pass domain warp
    vec2 q = vec2(
        fastFbm2(uv + vec2(t * 0.20, -t * 0.12)),
        fastFbm2(uv + vec2(5.2, 1.3) + vec2(-t * 0.15, t * 0.18))
    );

    vec2 r = vec2(
        fastFbm2(uv + 1.25 * q + vec2(1.7, 9.2) + vec2(t * 0.08, t * 0.05)),
        fastFbm2(uv + 1.25 * q + vec2(8.3, 2.8) + vec2(-t * 0.07, -t * 0.10))
    );

    float f = fastFbm2(uv + 1.25 * r);

    // Normalize pattern
    float pattern = clamp(f, 0.0, 1.0);
    float veins = pow(clamp(1.0 - abs(f * 2.0 - 1.0), 0.0, 1.0), 3.0);
    float light = clamp(0.5 + 0.5 * (q.x - q.y + r.x), 0.0, 1.0);

    // Obsidian / Silver Marble Color Grading
    vec3 cVoid     = vec3(0.018, 0.018, 0.022);
    vec3 cDarkChar = vec3(0.050, 0.050, 0.060);
    vec3 cGraphite = vec3(0.130, 0.130, 0.150);
    vec3 cSilver   = vec3(0.250, 0.255, 0.285);
    vec3 cSheen    = vec3(0.420, 0.430, 0.470);

    vec3 col = mix(cVoid, cDarkChar, smoothstep(0.10, 0.55, pattern));
    col = mix(col, cGraphite, smoothstep(0.40, 0.85, pattern * light));
    col = mix(col, cSilver, veins * 0.40);
    col += cSheen * (veins * veins * 0.30 + length(r) * 0.08 * light);

    col = pow(col, vec3(1.0 / CONTRAST)) + vec3(BRIGHTNESS);

    // Smooth Vignette
    float dist = length((texCoord - 0.5) * 1.30);
    float vignette = smoothstep(1.45, 0.25, dist);
    col *= vignette;

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
