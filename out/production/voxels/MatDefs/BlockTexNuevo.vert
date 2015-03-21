uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inColor;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying float shadow;
varying float distToCamera;

void main(){
	shadow = max(inColor.z, inColor.w);
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
}