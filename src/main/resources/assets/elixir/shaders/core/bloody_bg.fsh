#version 150

#moj_import <minecraft:common.glsl>

in vec2 texCoord0;
in vec4 vertexColor;

uniform float Time;
uniform vec2 Resolution;
uniform vec2 Mouse;
uniform float Alpha;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0,0.0)), hash(i + vec2(1.0,0.0)), u.x),
               mix(hash(i + vec2(0.0,1.0)), hash(i + vec2(1.0,1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = rot * p * 2.0 + vec2(100.0, 100.0);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 p = uv * 3.0 - vec2(1.5);
    p.x *= Resolution.x / Resolution.y;

    // React to mouse
    vec2 mouseNorm = Mouse / Resolution.xy;
    mouseNorm = (mouseNorm - 0.5) * 2.0;
    p -= mouseNorm * 0.2;

    float t = Time * 0.5;
    
    vec2 q = vec2(0.0);
    q.x = fbm(p + vec2(t * 0.1, t * 0.2));
    q.y = fbm(p + vec2(t * -0.15, t * 0.1));

    vec2 r = vec2(0.0);
    r.x = fbm(p + 4.0 * q + vec2(1.7, 9.2) + 0.15 * t);
    r.y = fbm(p + 4.0 * q + vec2(8.3, 2.8) + 0.126 * t);

    float f = fbm(p + 4.0 * r);
    
    vec3 col = vec3(0.0);
    vec3 color1 = vec3(0.02, 0.0, 0.0); // very dark red/black
    vec3 color2 = vec3(0.3, 0.0, 0.0);  // dark red
    vec3 color3 = vec3(0.6, 0.05, 0.05); // bright blood red

    col = mix(color1, color2, clamp(f * f * 4.0, 0.0, 1.0));
    col = mix(col, color3, clamp(length(q), 0.0, 1.0));
    col = mix(col, vec3(0.9, 0.1, 0.1), clamp(length(r.x), 0.0, 1.0) * 0.5); // highlight

    float alphaVal = clamp(Alpha, 0.0, 1.0);
    fragColor = vec4(col * (f * f * 1.5 + 0.5), alphaVal);
}
