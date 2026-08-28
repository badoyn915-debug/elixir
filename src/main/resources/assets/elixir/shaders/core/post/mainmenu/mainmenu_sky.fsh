#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform float uTime;
uniform vec2 uResolution;
uniform vec2 uMouseFov;

out vec4 fragColor;

// =========================================================
// PROCEDURAL LIQUID MARBLE SHADER (CONTINUOUS FLOW + JITTER-FREE PARALLAX)
// =========================================================

// --- Configurable Parameters ---
#define SPEED           0.11     // Base fluid flow rate
#define SCALE           1.85     // Pattern scale / frequency
#define WARP_INTENSITY  1.40     // Fluid turbulence strength
#define CONTRAST        1.22     // Tone curve response
#define BRIGHTNESS      0.035    // Base black level lift

// 2D Rotation matrix
mat2 rot(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, -s, s, c);
}

// Fast continuous 2D hash
vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

// 2D Simplex Noise for smooth organic derivatives
float snoise(vec2 p) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/2
    const float K2 = 0.211324865; // (3-sqrt(3))/6

    vec2 i = floor(p + (p.x + p.y) * K1);
    vec2 a = p - i + (i.x + i.y) * K2;
    float m = step(a.y, a.x);
    vec2 o = vec2(m, 1.0 - m);
    vec2 b = a - o + K2;
    vec2 c = a - 1.0 + 2.0 * K2;

    vec3 h = max(0.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);
    vec3 n = h * h * h * h * vec3(dot(a, hash2(i)), dot(b, hash2(i + o)), dot(c, hash2(i + 1.0)));

    return dot(n, vec3(70.0));
}

// Fractal Brownian Motion (4 octaves for optimal performance & richness)
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = rot(0.55);
    for (int i = 0; i < 4; i++) {
        v += a * snoise(p);
        p = m * p * 2.02 + vec2(0.13, 0.27);
        a *= 0.5;
    }
    return v;
}

// Deep Multi-Layer Domain Warping for viscous liquid marble folding
float liquidMarble(vec2 p, float t, out vec2 warpQ, out vec2 warpR) {
    // Primary Warp Layer (Macro organic swirling masses)
    vec2 q = vec2(
        fbm(p + vec2(0.0, 0.0) + vec2(t * 0.22, -t * 0.14)),
        fbm(p + vec2(5.2, 1.3) + vec2(-t * 0.16, t * 0.20))
    );
    warpQ = q;

    // Secondary Warp Layer (Intricate fluid folding veins)
    vec2 r = vec2(
        fbm(p + WARP_INTENSITY * q + vec2(1.7, 9.2) + vec2(t * 0.10, t * 0.06)),
        fbm(p + WARP_INTENSITY * q + vec2(8.3, 2.8) + vec2(-t * 0.08, -t * 0.12))
    );
    warpR = r;

    // Tertiary Final Field
    return fbm(p + WARP_INTENSITY * r);
}

void main() {
    // Isotropic, aspect-ratio corrected coordinates (never stretches on any resolution)
    vec2 aspect = vec2(uResolution.x / max(uResolution.y, 1.0), 1.0);
    vec2 uv = (texCoord - 0.5) * aspect * SCALE;

    // --- SMOOTH CINEMATIC PARALLAX ---
    // Smooth 3D depth tilt & parallax shift (ultra-smooth, zero jitter)
    vec2 parallax = uMouseFov * 0.18;
    uv += parallax * (0.90 + 0.25 * length(texCoord - 0.5));

    // Independent continuous time animation
    float t = uTime * SPEED;

    // Calculate layered fluid marble pattern
    vec2 q, r;
    float f = liquidMarble(uv, t, q, r);

    // Normalize pattern to [0, 1]
    float pattern = clamp(f * 0.5 + 0.5, 0.0, 1.0);

    // Fine delicate marble veins & filament lines
    float veins = 1.0 - abs(f);
    veins = pow(clamp(veins, 0.0, 1.0), 3.2);

    // Capillary fluid streaks from secondary warp vectors
    float streaks = length(r) * 0.5;
    streaks = smoothstep(0.2, 0.85, streaks);

    // Subtle ambient occlusion & surface depth lighting
    float light = clamp(0.5 + 0.5 * (q.x - q.y + r.x), 0.0, 1.0);

    // =========================================================
    // MINIMALIST PREMIUM DARK / GRAPHITE MARBLE PALETTE
    // =========================================================
    vec3 cVoid     = vec3(0.020, 0.020, 0.024); // Deepest obsidian black
    vec3 cDarkChar = vec3(0.055, 0.055, 0.065); // Dark charcoal
    vec3 cGraphite = vec3(0.135, 0.135, 0.155); // Matte graphite
    vec3 cSilver   = vec3(0.260, 0.265, 0.295); // Liquid silver / smoke
    vec3 cSheen    = vec3(0.440, 0.450, 0.490); // Soft specular highlight

    // Multi-stage tonal layering with ultra-soft transitions
    vec3 col = mix(cVoid, cDarkChar, smoothstep(0.08, 0.55, pattern));
    col = mix(col, cGraphite, smoothstep(0.38, 0.82, pattern * light));
    col = mix(col, cSilver, veins * 0.42);
    col += cSheen * (pow(veins, 2.6) * 0.32 + streaks * 0.10 * light);

    // Dynamic contrast & gamma adjustment
    col = pow(col, vec3(1.0 / CONTRAST));
    col += vec3(BRIGHTNESS);

    // Cinematic vignette for depth and elegance
    float dist = length((texCoord - 0.5) * 1.30);
    float vignette = smoothstep(1.45, 0.25, dist);
    col *= vignette;

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
