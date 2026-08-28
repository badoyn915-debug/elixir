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
    float f = 0.0;
    float w = 0.5;
    for (int i=0; i<4; i++) {
        f += w * noise(p);
        p *= 2.0;
        w *= 0.5;
    }
    return f;
}

vec3 diskColor(float r_disk, float angle, float time, float dopplerX) {
    float inner = 0.55;
    float outer = 2.2;
    if (r_disk < inner || r_disk > outer) return vec3(0.0);
    
    // Smooth ring
    float mask = smoothstep(inner, inner + 0.1, r_disk) * smoothstep(outer, outer - 0.5, r_disk);
    
    // Texture
    float swirl = angle - r_disk * 2.0 - time * 1.5;
    float n = fbm(vec2(swirl * 3.0, r_disk * 5.0));
    mask *= (0.4 + 0.6 * n);
    
    // Color (white-hot inside, orange/brown outside)
    float t = clamp((r_disk - inner) / (outer - inner), 0.0, 1.0);
    vec3 col = mix(vec3(1.0, 0.95, 0.8), vec3(0.8, 0.4, 0.1), pow(t, 0.7));
    
    // Doppler effect based on actual X position
    float doppler = clamp(dopplerX / outer, -1.0, 1.0);
    // Left side (positive X in our setup) is blueshifted and brighter
    float boost = 1.0 + doppler * 0.8; 
    
    return col * mask * boost * boost * 2.5; // Base brightness
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
    
    vec3 bhDir = normalize(vec3(0.0, 0.4, 1.0)); 
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), bhDir));
    vec3 up = cross(bhDir, right);
    
    float dotP = dot(rayW, bhDir);
    float theta = acos(clamp(dotP, -1.0, 1.0));
    float phi = atan(dot(rayW, up), dot(rayW, right));
    
    // p is the 2D coordinate on the sky centered at the black hole
    vec2 p = vec2(theta * cos(phi), theta * sin(phi)) * 6.0; 
    
    float r = length(p);
    float angle = atan(p.y, p.x);
    float bhRadius = 0.5;
    
    float time = uTime * uSpeed;
    vec3 col = vec3(0.0);
    
    if (r < bhRadius) {
        // Event horizon is pure black
        col = vec3(0.0);
    } else {
        // --- 1. Halo (Lensed Disk over and under) ---
        float dist = r - bhRadius;
        float inner = 0.55;
        
        // Map distance from event horizon to disk radius
        float mappedR = inner + dist * 8.0; 
        
        // The halo shouldn't be a perfect circle, it pinches slightly at the sides
        vec2 p_halo = normalize(p) * mappedR;
        p_halo.x *= 1.2; // slight horizontal squash
        
        float r_halo = length(p_halo);
        
        // Render halo
        vec3 haloColor = diskColor(r_halo, angle, time, p.x) * 0.7;
        
        // Halo is brighter at top and bottom, dimmer on the sides where it merges with direct disk
        float haloMask = smoothstep(0.0, 0.5, abs(p.y)); 
        col += haloColor * haloMask;
        
        // Photon ring glow (intense white/orange ring right at the edge)
        float photonRing = exp(-dist * 25.0);
        col += vec3(1.0, 0.9, 0.8) * photonRing * 1.5;
    }
    
    // --- 2. Direct Disk (Front) ---
    float tilt = 6.0; // The larger the tilt, the flatter the disk. 6.0 is good.
    vec2 p_direct = vec2(p.x, p.y * tilt);
    float r_direct = length(p_direct);
    
    // The front of the disk is the part where p.y < 0.
    bool isVisible = true;
    if (p.y > 0.0 && r < bhRadius) {
        isVisible = false; // Hidden behind the black hole
    }
    
    if (isVisible) {
        vec3 directCol = diskColor(r_direct, angle, time, p.x);
        float diskAlpha = clamp(length(directCol), 0.0, 1.0);
        
        if (diskAlpha > 0.0) {
            col = mix(col, directCol, diskAlpha);
        }
    }
    
    // --- 3. Starfield ---
    if (r > bhRadius && length(col) < 0.05) { 
        float starBend = 0.1 / (r - bhRadius * 0.9);
        vec2 p_star = p * (1.0 + starBend);
        
        float sTheta = length(p_star) / 6.0;
        float sPhi = atan(p_star.y, p_star.x);
        vec3 starDir = bhDir * cos(sTheta) + right * (sin(sTheta) * cos(sPhi)) + up * (sin(sTheta) * sin(sPhi));
        
        vec3 bgColor = vec3(0.002, 0.002, 0.005);
        
        vec2 starUv = vec2(atan(starDir.z, starDir.x) * sqrt(1.0 - starDir.y * starDir.y), asin(starDir.y)) * 150.0;
        vec2 grid = fract(starUv) - 0.5;
        vec2 id = floor(starUv);
        
        float h = hash(id);
        if (h > 0.97) {
            vec2 offset = vec2(hash(id + 1.0), hash(id + 2.0)) - 0.5;
            grid -= offset * 0.5;
            float size = (h - 0.97) * 33.3; 
            float d = length(grid);
            float star = smoothstep(0.15 * size, 0.03 * size, d);
            bgColor += vec3(1.0, 0.95, 0.9) * star;
        }
        
        col += bgColor * smoothstep(bhRadius, bhRadius + 0.1, r);
    }
    
    // Multiply by intensity setting
    col *= uIntensity * 100.0;
    
    // ACES Tonemapping
    col = (col * (2.51 * col + 0.03)) / (col * (2.43 * col + 0.59) + 0.14);
    
    // Apply user color tint softly
    vec3 userTint = mix(vec3(1.0), uColor * 2.0, 0.2);
    col *= userTint;
    
    fragColor = vec4(col, uAlpha);
}

