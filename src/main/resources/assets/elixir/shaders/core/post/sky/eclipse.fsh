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
        p *= 2.05;
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

    float time = uTime * uSpeed * 0.5;

    // Dark eclipsed sky
    vec3 bg = vec3(0.015, 0.018, 0.035);

    // Eclipse location in sky (straight ahead and up)
    vec3 eclipseCenter = normalize(vec3(0.0, 0.65, 0.75));
    float cosAngle = dot(rayW, eclipseCenter);
    float angleDist = acos(clamp(cosAngle, -1.0, 1.0));

    float moonRadius = 0.16;
    float coronaRadius = 0.75;

    if (angleDist < coronaRadius) {
        float relDist = (angleDist - moonRadius) / (coronaRadius - moonRadius);
        
        // Solar Corona Rays and Flares
        vec3 dirToSun = rayW - eclipseCenter * cosAngle;
        float rayAngle = atan(dirToSun.x, dirToSun.y);

        float flare1 = fbm(vec2(rayAngle * 5.0 + time * 0.3, angleDist * 8.0 - time * 0.4));
        float flare2 = fbm(vec2(rayAngle * 10.0 - time * 0.5, angleDist * 16.0));
        float coronaFlares = pow(flare1 * 0.7 + flare2 * 0.3, 1.5);

        float coronaFalloff = exp(-relDist * 4.5);
        vec3 coronaCol = mix(uColor, vec3(1.0, 0.9, 0.6), 0.6) * (uIntensity * 140.0);

        // Chromatic Diamond Ring Flare
        float ringGlow = smoothstep(moonRadius + 0.02, moonRadius, angleDist);
        vec3 ringCol = vec3(1.0, 0.95, 0.8) * 3.0;

        bg += coronaCol * coronaFlares * coronaFalloff;
        bg += ringCol * ringGlow;

        // Dark Moon Body with slight rim lighting
        if (angleDist < moonRadius) {
            bg = vec3(0.005, 0.005, 0.01);
            float rim = smoothstep(moonRadius - 0.015, moonRadius, angleDist);
            bg += vec3(0.4, 0.6, 1.0) * rim * 0.8;
        }
    }

    // Distant background stars
    vec2 starUv = vec2(atan(rayW.z, rayW.x), acos(clamp(rayW.y, -1.0, 1.0))) * 70.0;
    vec2 grid = fract(starUv) - 0.5;
    vec2 id = floor(starUv);
    float h = hash(id);
    if (h > 0.9) {
        float twinkle = sin(time * 5.0 + h * 50.0) * 0.5 + 0.5;
        bg += vec3(0.9, 0.95, 1.0) * smoothstep(0.12, 0.0, length(grid)) * twinkle;
    }

    fragColor = vec4(bg, uAlpha);
}
