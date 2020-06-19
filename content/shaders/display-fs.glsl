#version 300 es 
precision highp float;

uniform struct {
	sampler2D frame;
} program;

in vec2 tex;
in vec4 rayDir;

out vec4 fragmentColor;

void main(void) {
	fragmentColor = texture(program.frame, tex);
}