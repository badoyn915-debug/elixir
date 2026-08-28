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

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;

    float time = uTime * uSpeed * 0.8;

    // Dark cyberpunk gradient sky
    vec3 skyTop = vec3(0.04, 0.02, 0.12);
    vec3 skyHorizon = vec3(0.35, 0.05, 0.28);
    vec3 bg = mix(skyHorizon, skyTop, clamp(rayW.y * 1.5, 0.0, 1.0));

    // Synthwave Sun in the sky
    vec3 sunPos = normalize(vec3(0.0, 0.25, 1.0));
    float sunDot = dot(rayW, sunPos);
    if (sunDot > 0.88) {
        float sunDist = acos(clamp(sunDot, -1.0, 1.0));
        float sunRadius = 0.28;
        if (sunDist < sunRadius) {
            float sunGrad = (rayW.y - 0.05) / 0.45;
            vec3 sunCol = mix(vec3(1.0, 0.1, 0.4), vec3(1.0, 0.85, 0.2), clamp(sunGrad, 0.0, 1.0));
            // Horizontal slice bars
            float bar = sin(rayW.y * 70.0 - time * 0.5);
            if (bar > 0.35 && rayW.y < 0.28) {
                sunCol *= 0.15;
            }
            bg = mix(bg, sunCol, smoothstep(sunRadius, sunRadius - 0.02, sunDist));
        }
    }

    // Horizon Neon Glow
    float horizonGlow = exp(-abs(rayW.y) * 8.0);
    bg += mix(uColor, vec3(1.0, 0.2, 0.7), 0.5) * horizonGlow * 0.85;

    // Perspective Wireframe Grid on Ground (or ceiling)
    if (abs(rayW.y) > 0.02) {
        float planeY = rayW.y < 0.0 ? -1.0 : 1.0;
        float tHit = planeY / rayW.y;
        vec2 hitPos = rayW.xz * tHit;

        hitPos.y += (rayW.y < 0.0 ? time * 2.0 : -time * 1.5);
        vec2 grid = abs(fract(hitPos * (uScale * 0.15)) - 0.5);
        float lineDist = min(grid.x, grid.y);
        float gridLine = smoothstep(0.06, 0.0, lineDist);

        float depthFade = exp(-abs(tHit) * 0.08);
        vec3 gridCol = (rayW.y < 0.0 ? vec3(0.0, 0.95, 1.0) : vec3(1.0, 0.15, 0.65)) * (uIntensity * 120.0);

        bg += gridCol * gridLine * depthFade;
    }

    fragColor = vec4(bg, uAlpha);
}
