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
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float getCurtain(float angle, float h, float time, float offset, float speed) {
    float a = angle + offset;
    float t = time * speed;
    
    // Base shape of the curtain
    float baseH = sin(a * 1.5 + t * 0.4) * 0.5 + 
                  sin(a * 3.0 - t * 0.6) * 0.25;
                  
    // Relative height from the bottom edge
    float relH = h - baseH - offset * 0.5;
    
    // Sharp bottom edge, smooth top fade
    float verticalMask = 0.0;
    if (relH > 0.0) {
        verticalMask = exp(-relH * 3.0); // Smooth fade up
    } else {
        verticalMask = exp(relH * 15.0); // Sharp cut off at bottom
    }
    
    // Vertical rays
    float rays = sin(a * 100.0 + t * 2.0) * 0.5 + 0.5;
    rays *= sin(a * 60.0 - t * 1.5) * 0.5 + 0.5;
    rays *= sin(a * 30.0 + t * 0.5) * 0.5 + 0.5;
    
    // Add broad fade to parts of the curtain
    float broadFade = smoothstep(0.0, 0.8, sin(a * 2.5 + t));
    
    return verticalMask * rays * broadFade;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
    
    float time = uTime * uSpeed * 0.5;
    
    // Background sky color (dark night sky)
    float upAmount = clamp(rayW.y, 0.0, 1.0);
    vec3 skyColor = mix(vec3(0.01, 0.02, 0.04), vec3(0.0, 0.0, 0.01), upAmount);
    
    // Smooth, round stars
    if (rayW.y > 0.0) {
        vec2 starUv = rayW.xz / rayW.y * 100.0;
        vec2 grid = fract(starUv) - 0.5;
        vec2 id = floor(starUv);
        
        float r = hash(id);
        if (r > 0.97) {
            // Random offset so they aren't in a perfect grid
            vec2 offset = vec2(hash(id + 1.0), hash(id + 2.0)) - 0.5;
            grid -= offset * 0.5;
            
            float size = (r - 0.97) * 33.3; // 0.0 to 1.0
            float d = length(grid);
            // Draw a soft circle
            float star = smoothstep(0.1 * size, 0.02 * size, d);
            
            float twinkle = sin(time * 4.0 + r * 100.0) * 0.4 + 0.6;
            skyColor += vec3(1.0, 0.98, 0.95) * star * twinkle * upAmount;
        }
    }
    
    // Cylinder projection for aurora
    float a = rayW.x * rayW.x + rayW.z * rayW.z;
    if (a < 0.0001) {
        fragColor = vec4(skyColor, uAlpha);
        return;
    }
    
    float dist = 1.0 / sqrt(a);
    vec3 p = rayW * dist;
    
    float angle = atan(p.z, p.x);
    float h = p.y * uScale * 0.5;
    
    // Add multiple curtains
    float curtain1 = getCurtain(angle, h, time, 0.0, 1.0);
    float curtain2 = getCurtain(angle, h, time, 2.3, 0.8);
    float curtain3 = getCurtain(angle, h, time, 4.1, 1.2);
    
    float totalAurora = curtain1 + curtain2 * 0.7 + curtain3 * 0.5;
    
    // Enhance brightness based on intensity setting
    float finalAlpha = totalAurora * (uIntensity * 100.0);
    
    // Smooth color variation based on angle and time
    float colorVar = sin(angle * 2.0 + time * 0.5) * 0.5 + 0.5;
    // Mix uColor with a complementary aurora color (neon green/cyan)
    vec3 auroraCol = mix(uColor, vec3(0.0, 1.0, 0.6), colorVar);
    
    // Overbright for glow effect
    auroraCol *= 1.5;
    
    finalAlpha = clamp(finalAlpha, 0.0, 1.0) * uAlpha;
    
    vec3 col = mix(skyColor, auroraCol, finalAlpha);
    
    fragColor = vec4(col, uAlpha);
}
