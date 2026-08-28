#version 150

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;
uniform float BlurRadius;
uniform float Threshold;
uniform vec4  TintColor;
uniform vec4  TintColor2;
uniform int   Mode;
uniform float Time;
uniform float ShaderScale;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i+vec2(1,0)), f.x),
               mix(hash(i+vec2(0,1)), hash(i+vec2(1,1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += noise(p)*a; p = p*2.02+vec2(8.4,5.7); a *= 0.5; }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0, a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p)*2.0-1.0);
        v += r*a; p = p*2.18+vec2(3.1,9.2); a *= 0.52;
    }
    return v;
}

vec4 plasmaColor(vec2 uv, vec3 tint, float opacity) {
    vec2 p = uv * 6.0;
    float t = Time * 1.5;

    float v = 0.0;
    v += sin(p.x + t);
    v += sin((p.y + t) * 0.5);
    v += sin((p.x + p.y + t) * 0.5);
    vec2 c = p + vec2(sin(t * 0.33), cos(t * 0.5)) * 3.0;
    v += sin(sqrt(c.x * c.x + c.y * c.y + 1.0) + t);
    v *= 0.25;

    float intensity = 0.5 + 0.5 * sin(v * 3.14159265);
    float swirl = 0.5 + 0.5 * cos(v * 6.2831853 + t);

    vec3 col = mix(tint * 0.45, mix(tint, vec3(1.0), 0.55), intensity);
    col += swirl * tint * 0.25;

    float fill = clamp(0.35 + intensity * 0.8, 0.0, 1.0);
    float alpha = clamp(opacity * fill, 0.0, 1.0);
    return vec4(col, alpha);
}

float handDepth(vec2 uv) {
    return texture(DepthSampler, uv).r;
}

bool isHandPixel(vec2 uv) {
    float depth = handDepth(uv);
    if (depth < 0.001) {
        return false;
    }
    return depth < 0.9999;
}

float nearbyHand(vec2 uv, float radiusPx) {
    vec2 texel = radiusPx / textureSize(DepthSampler, 0);
    float v = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 dir = vec2(cos(a), sin(a));
        if (isHandPixel(uv + dir * texel)) {
            v += 1.0;
        }
    }
    return v / 8.0;
}

void main() {
    vec2 uv = TexCoord;
    vec3 src = texture(Sampler0, uv).rgb;
    bool hand = isHandPixel(uv);

    
    if (!hand && Mode != 1) {
        OutColor = vec4(src, 1.0);
        return;
    }

    if (Mode == 0) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec3 effect = mix(src, TintColor.rgb, TintColor.a * 0.65);
        OutColor = vec4(effect, 1.0);

    } else if (Mode == 1) {
        float near = nearbyHand(uv, BlurRadius * 0.35);
        if (hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float glow = smoothstep(Threshold, Threshold + 0.4, near);
        if (glow < Threshold) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float blend = clamp(pow(glow, 0.6) * TintColor.a * 1.2, 0.0, 1.0);
        OutColor = vec4(mix(src, TintColor.rgb, blend), 1.0);

    } else if (Mode == 2) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        float t = Time;
        vec2 flow = uv * (3.0 * ShaderScale), drift = vec2(t * 0.20, -t * 0.15);
        vec2 warp = vec2(fbm(flow * 0.90 + drift * 0.75 + vec2(0, 4.1)), fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8)));
        vec2 q = flow + (warp - 0.5) * 1.8;
        float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
        float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
        float sA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + t * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
        float sB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - t * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);
        float energy = clamp(mist * 0.22 + veins * 0.88 + sA * 0.55 + sB * 0.32, 0.0, 1.0);
        float core = smoothstep(0.18, 0.98, energy);
        float accent = pow(clamp(max(veins, sA), 0.0, 1.0), 1.25);
        vec3 effect = mix(TintColor.rgb, mix(TintColor.rgb, vec3(1.0), 0.4), clamp(core * 0.75 + sB * 0.25, 0.0, 1.0));
        effect *= 0.35 + core * 0.75 + accent * 0.22;
        float blend = clamp(TintColor.a * (0.35 + core * 0.75 + accent * 0.22) * 0.92, 0.0, 1.0);
        OutColor = vec4(mix(src, effect, blend), 1.0);

    } else if (Mode == 3) {
        if (!hand) {
            OutColor = vec4(src, 1.0);
            return;
        }
        vec4 plasma = plasmaColor(uv * (4.0 * ShaderScale), TintColor.rgb, TintColor.a);
        float blend = clamp(plasma.a, 0.0, 1.0);
        OutColor = vec4(mix(src, plasma.rgb, blend), 1.0);

    } else {
        OutColor = vec4(src, 1.0);
    }
}
