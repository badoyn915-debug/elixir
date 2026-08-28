#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform vec2 uCameraAngles;
uniform vec2 uCameraDir;
uniform float uFov;
uniform float uDensity;
uniform float uGlow;
uniform float uParallax;
uniform float uParticles;
uniform int uQualitySteps;

out vec4 fragColor;

// =========================================================
// HIGH-PERFORMANCE 3D VOLUMETRIC LIQUID NEBULA SHADER
// =========================================================

// Fast 3D Rotation matrices
mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

// Ultra-fast Hash
float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float hash2D(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// High-speed 3D Value Noise with smooth Hermite interpolation
float fastNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash(i + vec3(1.0, 1.0, 1.0));

    return mix(
        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
        f.z
    );
}

// Fast 2-octave FBM
float fastFbm2(vec3 p) {
    return fastNoise3D(p) * 0.65 + fastNoise3D(p * 2.02 + vec3(0.2, 0.4, 0.6)) * 0.35;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(uResolution.xy, vec2(1.0));
    vec2 sp = (uv * 2.0 - 1.0);
    float aspect = uResolution.x / max(uResolution.y, 1.0);

    // Compute Camera Ray using Internal FOV and Camera Orientation
    vec2 camAngle = (length(uCameraAngles) > 0.001) ? uCameraAngles : uCameraDir;
    float fovRad = radians(clamp(uFov, 30.0, 130.0));
    float tanHalfFov = tan(fovRad * 0.5);

    vec3 rayCam = normalize(vec3(sp.x * tanHalfFov * aspect, sp.y * tanHalfFov, 1.0));
    mat3 rotMat = rotY(camAngle.x) * rotX(camAngle.y);
    vec3 rayDir = rotMat * rayCam;

    // Ray origin with parallax translation
    vec3 rayOrigin = rotMat * vec3(0.0, 0.0, -1.0) + vec3(sin(camAngle.x) * 0.25, -sin(camAngle.y) * 0.25, 0.0) * uParallax;

    float t = uTime * uSpeed * 0.25;

    // Determine raymarching quality steps (Optimized for ultra-smooth 100+ FPS)
    int steps = clamp(uQualitySteps, 8, 28);

    // Raymarching bounds
    float tMin = 0.6;
    float tMax = 4.8;
    float stepSize = (tMax - tMin) / float(steps);

    // Fast stochastic jitter
    float jitter = hash2D(gl_FragCoord.xy) * stepSize;
    float currentT = tMin + jitter;

    // Volumetric Accumulation buffers
    vec3 accumColor = vec3(0.0);
    float transmittance = 1.0;

    // Cosmic Color Palette
    vec3 cDeepSpace  = vec3(0.006, 0.008, 0.018);
    vec3 cBaseTint   = uColor;
    vec3 cCyanGas    = vec3(0.08, 0.75, 0.95);
    vec3 cMagentaGas = vec3(0.92, 0.28, 0.72);
    vec3 cGoldenCore = vec3(1.00, 0.88, 0.55);
    vec3 cVioletDust = vec3(0.55, 0.18, 0.85);

    // Dynamic Core Center
    vec3 corePos = vec3(sin(t * 0.25) * 0.35, cos(t * 0.20) * 0.25, 2.5);

    // Highly Optimized Raymarching Loop
    for (int i = 0; i < 28; i++) {
        if (i >= steps || transmittance < 0.02) break;

        vec3 samplePos = rayOrigin + rayDir * currentT;
        float distFromCenter = length(samplePos - vec3(0.0, 0.0, 2.6));

        // Early containment culling
        if (distFromCenter < 2.8) {
            // Fast Single-pass Domain Warp
            vec3 warp = vec3(
                fastNoise3D(samplePos * 0.8 + vec3(t * 0.10, 0.0, t * 0.05)),
                fastNoise3D(samplePos * 0.8 + vec3(3.2, t * 0.08, -t * 0.06)),
                fastNoise3D(samplePos * 0.8 + vec3(-t * 0.07, 2.1, t * 0.09))
            );

            // Fast Gas Density
            float rawDensity = fastFbm2(samplePos * 1.1 + warp * 1.25);
            float radialFalloff = clamp(1.0 - distFromCenter * 0.38, 0.0, 1.0);
            float density = max(rawDensity - 0.26, 0.0) * radialFalloff * uDensity;

            if (density > 0.001) {
                // Volumetric Core Glow
                float coreDist = length(samplePos - corePos);
                float coreIntensity = (uGlow * 0.8) / (1.0 + coreDist * coreDist * 3.0);
                float corePulsar = sin(t * 1.8 - coreDist * 3.0) * 0.15 + 0.85;
                coreIntensity *= corePulsar;

                // Gas color mixing
                vec3 gasColor = mix(cBaseTint, cVioletDust, clamp(warp.x * 1.3, 0.0, 1.0));
                gasColor = mix(gasColor, cCyanGas, clamp(warp.y * 1.2, 0.0, 1.0));
                gasColor = mix(gasColor, cMagentaGas, clamp(warp.z * 1.1, 0.0, 1.0));
                gasColor += cGoldenCore * (coreIntensity * 0.85);

                // Volumetric absorption & lighting
                float absorption = exp(-density * stepSize * 3.8);
                vec3 stepLight = gasColor * (density * 3.2 + coreIntensity * 1.6) * stepSize;

                accumColor += stepLight * transmittance;
                transmittance *= absorption;
            }
        }

        currentT += stepSize;
    }

    // High-performance 3D Background Starfield (Single Pass, Zero Loops)
    if (uParticles > 0.05) {
        vec3 pDir = rayDir * 16.0;
        vec3 starCell = floor(pDir + vec3(t * 0.02, 0.0, t * 0.01));
        float h = hash(starCell);
        if (h > (0.965 - uParticles * 0.012)) {
            vec3 starFract = fract(pDir + vec3(t * 0.02, 0.0, t * 0.01)) - 0.5;
            float d = length(starFract);
            float twinkle = sin(t * 3.5 + h * 62.8) * 0.4 + 0.6;
            float starGlow = smoothstep(0.14 * (h - 0.75), 0.0, d) * twinkle;
            accumColor += mix(vec3(0.85, 0.95, 1.0), cGoldenCore, fract(h * 5.0)) * starGlow * 1.2 * uParticles;
        }
    }

    // Blend background space void with accumulated volumetric nebula light
    vec3 finalColor = mix(cDeepSpace, accumColor, 1.0 - transmittance) + accumColor;

    // ACES / Filmic Tone Mapping
    finalColor = finalColor / (finalColor + vec3(1.0));
    finalColor = pow(finalColor, vec3(1.0 / 1.15));

    // Subtle edge vignette
    float vig = length(sp) * 0.65;
    finalColor *= smoothstep(1.3, 0.35, vig);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), clamp(uAlpha, 0.0, 1.0));
}
