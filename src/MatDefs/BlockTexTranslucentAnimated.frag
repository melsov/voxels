varying vec2 texCoord;
varying vec2 texMapOffset;

uniform sampler2D m_ColorMap;
uniform vec4 m_Color;

varying float shadow;

void main(){
    vec4 texColor = texture2D(m_ColorMap, texCoord * .25 + texMapOffset);
    gl_FragColor =  vec4(texColor.rgb, .5); // * shadow;
}