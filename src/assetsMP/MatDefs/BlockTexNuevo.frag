varying vec2 texCoord;
varying vec2 texMapOffset;

uniform sampler2D m_ColorMap;
uniform vec4 m_Color;

varying float shadow;
//varying vec4 vertColor;

void main(){
    //vec4 texColor = texture2D(m_ColorMap, mod(texCoord * .25, .25) + texMapOffset);
    vec2 co = (texCoord * .25 + texMapOffset);
    vec4 texColor = texture2D(m_ColorMap, co);
    gl_FragColor = vec4(mix(m_Color.rgb, texColor.rgb, texColor.a), 1.0) * shadow; 

    //vec4 texColor = texture2D(m_ColorMap, texCoord);
    //texColor; 
    //gl_FragColor =  vec4(1.,1.,0.,1.); 
    
}