#version 300 es 
precision highp float;

#define QSIZE 6
#define LSIZE 3
#define EPS .0001
#define PI 3.1415926535897932384626433832795

uniform struct {
	mat4 rayDirMatrix;
	vec3 position;
} camera;

uniform struct {
	mat4 surface;
	mat4 clipper;
	vec4 color;
	vec4 reflectance;
} quadrics[QSIZE];

uniform struct {
	vec4 position;
	vec3 powerDensity;
} lights[LSIZE];

uniform struct {
	sampler2D previousFrame;
	float weight;
} program;

in vec2 tex;
in vec4 rayDir;

out vec4 fragmentColor;

float random(vec3 scale, float seed){
	return fract(sin(dot(gl_FragCoord.xyz + seed + program.weight, scale))
	* 43438.1053 / (seed + 1.0));
}

vec4 randomReflect(vec4 d, vec4 n, float seed) {
	float r1 = random((n+d).xyz, seed);
	float r2 = random((n+d).xyz, seed);
	vec4 r = vec4(
		sqrt(1.0 - r2) * cos(2.0 * PI * r1),
		sqrt(1.0 - r2) * sin(2.0 * PI * r1),
		sqrt(r2),
		0.0
	);
	vec4 center = vec4(0.0, 0.0, 1.0, 0.0);
	if(length(n-center)<EPS) return r;
	if(length(n+center)<EPS) return -r;

	vec3 tan = normalize(cross(center.xyz, n.xyz));
	vec3 bit = normalize(cross(n.xyz, tan));
	vec3 rotated = tan*r.x + bit*r.y + n.xyz*r.z;
	/*
	vec3 u = normalize(cross(center.xyz, n.xyz));
	float f = dot(n.xyz, center.xyz);
	float c = cos(f);
	float s = sin(f);
	vec3 rotated = vec3 (
		r.x*(c+u.x*u.x*(1.0-c)) + r.y*u.x*u.y*(1.0-c) + r.z*u.y*s,
		r.x*u.x*u.y*(1.0-c) + r.y*(c+u.y*u.y*(1.0-c)) - r.z*u.x*s,
		-r.x*u.y*s + r.y*u.x*s + r.z*c
	);
	*/
	return vec4(normalize(rotated), 0.0);
}

float intersectClippedQuadric(vec4 e, vec4 d, mat4 coeff, mat4 clipper){
	float a = dot(d * coeff, d);
	float b = dot(e * coeff, d) + dot(d * coeff, e);
	float c = dot(e * coeff, e);

	float disc = b*b - 4.0 *a*c;
	if(disc < 0.0)
	  -1.0;
	float t1 = (-b - sqrt(disc)) / (2.0 * a);
	float t2 = (-b + sqrt(disc)) / (2.0 * a);

	vec4 h1 = e+d*t1;	
	vec4 h2 = e+d*t2;

	if( dot(h1 * clipper, h1) > 0.0 ) t1 = -1.0;
	if( dot(h2 * clipper, h2) > 0.0 ) t2 = -1.0;	

	return (t1<0.0)?t2:(t2<0.0?t1:min(t1, t2));
}

void findBestHit(vec4 e, vec4 d, out float bestT, out int bestIndex) {
	bestT = 10000.0;
	for (int i = 0; i < QSIZE; i++){
		float t = intersectClippedQuadric(e, d, quadrics[i].surface, quadrics[i].clipper);
		if (t > 0.0 && t < bestT) {
			bestT = t;
			bestIndex = i;
		}
	}
	if (bestT == 10000.0) bestT = -1.0;
}

vec4 getNormal(int index, vec4 hit) {
	vec4 gradient = hit * quadrics[index].surface + quadrics[index].surface * hit;
	return vec4(normalize(gradient.xyz), 0.);
}

bool isVisible(vec4 hit, vec4 light) {
	float xt;
	int xi;
	if (light.w < EPS) {
		findBestHit(hit, light, xt, xi);
	} else {
		findBestHit(hit, normalize(light-hit), xt, xi);
	}
	return xt < 0.;
}

vec3 shade(vec4 hit, vec4 normal, vec3 materialColor) {
	vec3 color = vec3(0., 0., 0.);
	for (int i = 0; i < LSIZE; i++){
		if (isVisible(hit + normal*EPS, lights[i].position)){
			vec3 M = lights[i].powerDensity;
			vec3 dist = lights[i].position.xyz - hit.xyz * lights[i].position.w;
			vec3 diff = materialColor * max(dot(normalize(dist), normal.xyz), 0.);
			color += M/length(dist)/length(dist) * diff;
		}
	}
	return color;
}

vec4 refract(vec4 d, vec4 n, float r){
	float c = dot(normalize(-d), n);
	float sr = sqrt(1.0-c*c) / r;
	if(sr >= 1.0) return reflect(d, n);
	float cr = sqrt(1.0-sr*sr);
	vec3 t = normalize(cross(n.xyz, -d.xyz));
	vec3 tr = normalize(cross(n.xyz, t));
	return vec4(normalize(-n.xyz * cr + tr * sr), 0.0);
}

void refractor(vec4 d, vec4 hit, vec4 normal, out vec4 oe, out vec4 od){
	float n = 1.45;
	if(dot(d, normal) < 0.0){
		// Bemegyunk
		vec4 refractD = refract(d, normal, n);
		oe = hit - normal * EPS;
		od = refractD;
	} else {
		// Kimegyunk
		vec4 refractD = refract(d, -normal, n);
		if(dot(normal, refractD) >= 0.0){
			oe = hit + normal * EPS;
			od = refractD;
		} else {
			oe = hit - normal * EPS;
			od = refractD;
		}
	}
}

vec3 pathTrace(vec4 d, vec4 e){
	vec3 w = vec3(1.0, 1.0, 1.0);
	vec3 color = vec3(0.0, 0.0, 0.0);

	for (int i=0; i<4; i++) {
		float t = 0.0;
		int index = 0;
		findBestHit(e, d, t, index);
		vec4 hit = e + d*t;
		vec4 normal = getNormal(index, hit);

		if(t > 0.0) {
			switch(index){
				case 0:
				case 1:
				case 2:
				color += shade(hit, normal, quadrics[index].color.rgb)
					* w.xyz;
				e = hit + normal * EPS;
				d = randomReflect(d, normal, float(i));
				break;
				case 3:
				e = hit + normal * EPS;
				d = reflect(d, normal);
				break;
				case 4:
				vec4 oe;
				vec4 od;
				refractor(d, hit, normal, oe, od);
				e = oe;
				d = od;
				break;
				case 5:
				if(dot(d, normal) > 0.0
				|| random((normal+d).xyz, float(index)) < dot(normal, -d)){
					// Tort
					vec4 oe;
					vec4 od;
					refractor(d, hit, normal, oe, od);
					e = oe;
					d = od;
				} else {
					// Tukor
					e = hit + normal * EPS;
					d = reflect(d, normal);
				}
				break;
			}
			w *= quadrics[index].reflectance.rgb;
			if(length(w) < EPS) break;
		} else {
			color += vec3(.2, .3, .7) * w.rgb;
			break;
		}
	}
	return color;
}

void main(void) {
	vec4 d = vec4(normalize(rayDir.xyz), 0);
	vec4 e = vec4(camera.position, 1);

	vec3 radiance = pathTrace(d, e);

	fragmentColor = vec4(radiance, 1) * program.weight
		+ texture(program.previousFrame, tex) * (1.0 - program.weight);
}