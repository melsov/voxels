uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;
attribute vec4 inColor;

attribute vec2 inTexCoord2; //TEX MAP OFFSE.

varying vec2 texCoord;
varying vec2 texMapOffset;
varying float shadow;
//varying vec4 vertColor;

void main(){

// inNormal.x + inNormal.y + inNormal.z;

	shadow = inColor.w; // inColor.x + inColor.y + inColor.z; 
	shadow = max(shadow, 0.0);
	shadow = min(shadow, 1.0);
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
    texMapOffset = inTexCoord2;
 //   vertColor = inColor;
}