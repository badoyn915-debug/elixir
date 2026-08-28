#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 TexCoord;

void main() {
    
    TexCoord = Position.xy;
    gl_Position = vec4(Position.x * 2.0 - 1.0, Position.y * 2.0 - 1.0, 0.0, 1.0);
}
