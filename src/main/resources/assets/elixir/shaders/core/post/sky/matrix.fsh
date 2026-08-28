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

float random(float x) {
    return fract(sin(x) * 43758.5453);
}
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

// Function to generate Matrix-like characters
float text(vec2 coord, float time) {
    vec2 id = floor(coord);
    vec2 gv = fract(coord);
    
    // Random changing seed for the character
    float seed = random(id + floor(time * 5.0));
    
    // Divide the cell into a 3x5 pixel grid for the character
    vec2 pixel = floor(gv * vec2(3.0, 5.0));
    
    // Generate noise for each pixel in the 3x5 grid
    float n = random(id + pixel + seed);
    float char = step(0.5, n);
    
    // Add margins around characters so they don't touch
    // Margin of 10% on left, 20% on right, 10% on top, 20% on bottom
    char *= step(0.1, gv.x) * step(gv.x, 0.8) * step(0.1, gv.y) * step(gv.y, 0.8);
    
    return char;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
    
    vec3 dir = rayW;
    
    // Pure Cylindrical mapping for the walls
    float theta = atan(dir.x, dir.z); // -pi to pi
    // Using simple y coordinate for vertical height
    // We want the text to look uniform in size, so we use dir.y / length(dir.xz)
    float lenXZ = max(length(dir.xz), 0.001);
    float h = dir.y / lenXZ; 
    
    // Map to 2D grid
    // Scale up the coordinates significantly so the characters are clear and blocky.
    // uScale default is 5.0, so this gives a nicely dense grid.
    vec2 st = vec2(theta * 15.0, h * 15.0) * (uScale * 0.5);
    
    float t = uTime * uSpeed;
    
    // Column logic
    float colIndex = floor(st.x);
    float colSpeed = 1.0 + random(colIndex) * 2.0; // Random speed per column
    float colOffset = random(colIndex * 13.37) * 100.0;
    
    // Move the column downwards
    st.y += t * colSpeed * 5.0 + colOffset;
    
    // Get cell ID and UV
    vec2 id = floor(st);
    
    // Generate character
    float charMask = text(st, t);
    
    // Trail calculation (head is bright, tail fades)
    float dropLength = 10.0 + random(colIndex * 2.1) * 20.0;
    float dropPos = fract(st.y / dropLength - t * colSpeed * 0.05);
    
    // Drop pos goes 0..1. The head is at dropPos = 0.
    float brightness = 1.0 - dropPos;
    brightness = pow(brightness, 3.0); // Exponential fade for the tail
    
    // Final color
    vec3 baseCol = uColor;
    // If the user's color is white, give it a Matrix tint
    if (length(baseCol) > 1.6) {
        baseCol = vec3(0.2, 1.0, 0.3);
    }
    
    vec3 col = baseCol * charMask * brightness * uIntensity * 100.0;
    
    // The very tip of the drop should be white
    if (brightness > 0.8) {
        col = mix(col, vec3(1.0, 1.0, 1.0), (brightness - 0.8) * 5.0 * charMask);
    }
    
    // Flicker effect
    if (random(id + floor(t * 10.0)) > 0.95) {
        col *= 2.0;
    }
    
    // Fade out near the zenith and nadir to hide the cylindrical distortion
    float fade = smoothstep(2.5, 1.0, abs(h));
    col *= fade;
    
    // Background color (very dark green/black)
    vec3 bg = baseCol * 0.05 * fade;
    vec3 finalColor = bg + col;
    
    fragColor = vec4(finalColor, uAlpha);
}
