varying vec2 texCoord;
varying float shadow;
varying float distToCamera;

uniform sampler2D m_ColorMap;
uniform vec4 m_Color;


void main(){
    gl_FragColor = vec4(texture2D(m_ColorMap, texCoord).rgb, 0.1);// * shadow;
}