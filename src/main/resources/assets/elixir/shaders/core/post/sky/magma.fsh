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
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.1;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;

    float time = uTime * uSpeed * 0.6;

    // Dark Volcanic Ash Sky
    vec3 bg = vec3(0.06, 0.015, 0.01);

    // Spherical coordinates for magma sky dome
    vec2 magmaUv = vec2(atan(rayW.z, rayW.x), acos(clamp(rayW.y, -1.0, 1.0))) * (uScale * 0.5);

    // Boiling Magma Fissures (Domain Warped Noise)
    vec2 q = vec2(
        fbm(magmaUv + vec2(time * 0.3, time * 0.15)),
        fbm(magmaUv + vec2(4.1, 2.8) - vec2(time * 0.2, time * 0.25))
    );

    float magma = fbm(magmaUv + 3.0 * q + vec2(time * 0.1, time * 0.2));

    // Lava Heat Veins & Colors
    vec3 colDarkCrust = vec3(0.05, 0.02, 0.02);
    vec3 colDeepLava  = vec3(0.9, 0.15, 0.02);
    vec3 colHotLava   = vec3(1.0, 0.65, 0.05);
    vec3 colWhiteHeat = vec3(1.0, 0.95, 0.7);

    vec3 magmaCol = mix(colDarkCrust, colDeepLava, smoothstep(0.3, 0.55, magma));
    magmaCol = mix(magmaCol, colHotLava, smoothstep(0.55, 0.75, magma));
    magmaCol = mix(magmaCol, colWhiteHeat, smoothstep(0.75, 0.95, magma));

    // Glow and heat intensity
    magmaCol = mix(magmaCol, uColor * 1.5, 0.35);
    bg += magmaCol * (uIntensity * 110.0);

    // Flying Ember Sparks
    vec2 emberCoord = magmaUv * 8.0 + vec2(sin(time * 2.0) * 0.5, -time * 3.0);
    vec2 grid = fract(emberCoord) - 0.5;
    vec2 id = floor(emberCoord);
    float h = hash(id);
    if (h > 0.92) {
        float ember = smoothstep(0.18 * (h - 0.92) * 12.0, 0.0, length(grid));
        bg += vec3(1.0, 0.7, 0.2) * ember * 2.0;
    }

    fragColor = vec4(bg, uAlpha);
}
