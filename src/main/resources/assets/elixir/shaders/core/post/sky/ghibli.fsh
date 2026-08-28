#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 74.5);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float f = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 6; i++) {
        f += amp * noise(p);
        p *= 2.13;
        amp *= 0.5;
    }
    return f;
}

// Double-warped FBM for realistic puffy clouds
float cloudShape(vec2 p) {
    vec2 q = vec2(fbm(p + vec2(0.0, 0.0)), fbm(p + vec2(5.2, 1.3)));
    vec2 r = vec2(fbm(p + 4.0 * q + vec2(1.7, 9.2)), fbm(p + 4.0 * q + vec2(8.3, 2.8)));
    return fbm(p + 4.5 * r);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayDir = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;

    float upness = rayDir.y;

    // ========= SKY GRADIENT =========
    // Vibrant Ghibli sky: deep cerulean blue at top, warm light blue at mid, creamy white at horizon
    vec3 skyZenith   = vec3(0.10, 0.40, 0.88);  // Deep vivid blue
    vec3 skyMid      = vec3(0.35, 0.65, 0.97);  // Mid clear blue
    vec3 skyHorizon  = vec3(0.78, 0.91, 1.00);  // Hazy horizon
    vec3 skySunSide  = vec3(0.98, 0.93, 0.82);  // Warm golden near sun

    float up01 = clamp(upness, 0.0, 1.0);
    vec3 skyColor = mix(skyHorizon, skyMid, smoothstep(0.0, 0.35, up01));
    skyColor = mix(skyColor, skyZenith, smoothstep(0.3, 1.0, up01));

    // ========= SUN =========
    vec3 sunDir = normalize(vec3(0.55, 0.55, 0.6));
    float sunDot = dot(rayDir, sunDir);

    // Sun disc - crisp white circle
    float sunDisc = smoothstep(0.9993, 0.9997, sunDot);

    // Inner halo - soft golden ring
    float innerHalo = pow(max(0.0, sunDot), 512.0) * (1.0 - sunDisc);

    // Outer corona - wide atmospheric scatter
    float outerCorona = pow(max(0.0, sunDot), 12.0) * 0.12;

    // Airy diffraction spikes - 4 faint star rays from the sun
    vec3 toSun = normalize(rayDir - sunDir * sunDot);
    float spike1 = abs(toSun.x);
    float spike2 = abs(toSun.y);
    float spikes = pow(max(spike1, spike2), 20.0) * pow(max(0.0, sunDot), 4.0) * 0.25;

    vec3 sunColor = vec3(0.0);
    sunColor += vec3(1.0, 0.99, 0.95) * sunDisc * 4.0;           // White disc
    sunColor += vec3(1.0, 0.92, 0.70) * innerHalo * 2.5;         // Golden inner halo
    sunColor += vec3(1.0, 0.88, 0.65) * outerCorona;             // Warm corona
    sunColor += vec3(1.0, 0.95, 0.85) * spikes;                  // Star spikes

    // Warm sky glow around sun (large-scale)
    float sunGlow = pow(max(0.0, sunDot), 5.0);
    skyColor = mix(skyColor, skySunSide, sunGlow * 0.45);

    // ========= GOD RAYS (crepuscular rays) =========
    // Fake volumetric light shafts by sampling noise along the sun direction
    float godRay = 0.0;
    if (sunDot > 0.3) {
        vec2 sunUv = vec2(dot(rayDir, normalize(cross(sunDir, vec3(0.0, 1.0, 0.0)))),
                         dot(rayDir, normalize(cross(cross(sunDir, vec3(0.0, 1.0, 0.0)), sunDir))));
        float angle = atan(sunUv.y, sunUv.x);
        float beams = sin(angle * 8.0 + 0.5) * 0.5 + 0.5;
        beams = pow(beams, 3.0);
        godRay = beams * pow(max(0.0, sunDot - 0.3) / 0.7, 2.0) * 0.08;
    }
    skyColor += vec3(1.0, 0.95, 0.80) * godRay;

    // ========= CLOUDS =========
    vec3 finalColor = skyColor;
    float cloudAlpha = 0.0;

    float t = uTime * uSpeed * 0.01;

    // --- Layer 1: Big puffy cumulus ---
    if (rayDir.y > 0.01) {
        float tHit = 1.0 / rayDir.y;
        vec2 cPos = rayDir.xz * tHit * 0.18 * uScale;
        cPos += vec2(t * 0.8, t * 0.35);

        float dens = cloudShape(cPos);
        float thresh = 0.52;
        float cloud = smoothstep(thresh, thresh + 0.10, dens);

        // Top-down lighting: sunlit top, shadowed underbelly
        float topLight = smoothstep(thresh, thresh + 0.20, dens);          // how "inside" the cloud we are
        float shadow   = cloudShape(cPos + vec2(0.015, 0.025));
        float shadowFactor = smoothstep(thresh, thresh + 0.18, shadow);

        vec3 cloudTop  = vec3(1.00, 0.99, 0.97);   // Brilliant sunlit white
        vec3 cloudMid  = vec3(0.90, 0.93, 0.97);   // Mid cloud - slightly cool
        vec3 cloudBase = vec3(0.65, 0.72, 0.84);   // Blue-grey shadow belly (classic Ghibli)

        vec3 cCol = mix(cloudBase, cloudMid, shadowFactor);
        cCol = mix(cCol, cloudTop, topLight * topLight);

        // Sun rim light - edges of cloud facing sun glow warm
        float rimLight = pow(max(0.0, sunDot), 3.0) * (1.0 - topLight) * 0.4;
        cCol += vec3(1.0, 0.92, 0.75) * rimLight;

        float fade = smoothstep(0.01, 0.15, rayDir.y);
        float a = cloud * fade;
        finalColor = mix(finalColor, cCol, a);
        cloudAlpha = max(cloudAlpha, a);
    }

    // --- Layer 2: Mid-level altocumulus (smaller, rounder puffs) ---
    if (rayDir.y > 0.06) {
        float tHit2 = 1.6 / rayDir.y;
        vec2 cPos2 = rayDir.xz * tHit2 * 0.28 * uScale;
        cPos2 += vec2(t * 1.3, t * 0.6);

        float dens2 = fbm(cPos2 + vec2(10.5, 23.7));
        float cloud2 = smoothstep(0.60, 0.68, dens2) * 0.55; // More transparent

        vec3 cCol2 = vec3(0.93, 0.96, 1.0);
        // Backlit by sun - slightly warm tint
        cCol2 += vec3(0.05, 0.03, 0.0) * pow(max(0.0, sunDot), 3.0);

        float fade2 = smoothstep(0.06, 0.25, rayDir.y);
        float a2 = cloud2 * fade2 * (1.0 - cloudAlpha * 0.7);
        finalColor = mix(finalColor, cCol2, a2);
    }

    // --- Layer 3: High wispy cirrus ---
    if (rayDir.y > 0.1) {
        float tHit3 = 3.0 / rayDir.y;
        vec2 cPos3 = rayDir.xz * tHit3 * 0.45 * uScale;
        cPos3 += vec2(t * 2.0, t * 0.9);

        float dens3 = noise(cPos3 * 2.0) * 0.5 + noise(cPos3 * 4.3 + 5.0) * 0.3 + noise(cPos3 * 9.0 + 11.0) * 0.2;
        float cloud3 = smoothstep(0.55, 0.65, dens3) * 0.35;

        // Cirrus are almost pure white, slightly translucent
        vec3 cCol3 = vec3(0.97, 0.98, 1.0);
        // Sun through thin clouds = warm glow
        float sunThrough = pow(max(0.0, sunDot), 8.0) * 0.5;
        cCol3 += vec3(0.03, 0.02, 0.0) * sunThrough;

        float fade3 = smoothstep(0.1, 0.4, rayDir.y);
        float a3 = cloud3 * fade3 * (1.0 - cloudAlpha * 0.5);
        finalColor = mix(finalColor, cCol3, a3);
    }

    // ========= SUN ON TOP OF CLOUDS =========
    finalColor += sunColor;

    // ========= BELOW HORIZON =========
    if (upness < 0.0) {
        float below = smoothstep(0.0, -0.25, upness);
        vec3 fog = mix(skyHorizon, vec3(0.6, 0.75, 0.88), below * 0.5);
        finalColor = mix(finalColor, fog, below * 0.85);
    }

    // ========= TONE MAP & COLOR GRADE =========
    // Reinhard with toe for bright highlights
    finalColor = finalColor / (finalColor + 0.4) * 1.4;

    // Saturation boost - Ghibli style is vivid
    float lum = dot(finalColor, vec3(0.2126, 0.7152, 0.0722));
    finalColor = mix(vec3(lum), finalColor, 1.30);

    // Very gentle warm grade (film emulation)
    finalColor.r = pow(finalColor.r, 0.95);
    finalColor.g = pow(finalColor.g, 0.97);

    finalColor = clamp(finalColor, 0.0, 1.0);

    // User tint
    finalColor *= mix(vec3(1.0), uColor, 0.12);

    fragColor = vec4(finalColor, uAlpha);
}
