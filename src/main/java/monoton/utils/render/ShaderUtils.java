package monoton.utils.render;

import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import monoton.utils.IMinecraft;
import monoton.utils.misc.FileUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL20.*;

public class ShaderUtils implements IMinecraft {
    private final int programID;
    public static ShaderUtils
            CORNER_ROUND_SHADER,
            TEXTURE_ROUND_SHADER,
            ROUND_SHADER_OUTLINE,
            ROUND_SHADER;

    public ShaderUtils(String fragmentShaderLoc) {
        programID = ARBShaderObjects.glCreateProgramObjectARB();

        try {
            int fragmentShaderID = switch (fragmentShaderLoc) {
                case "roundedGradient" -> createShader(new ByteArrayInputStream(roundedGradient.getBytes()), GL_FRAGMENT_SHADER);
                case "corner" -> createShader(new ByteArrayInputStream(roundedCornerRect.getBytes()), GL_FRAGMENT_SHADER);
                case "cornerGradient" -> createShader(new ByteArrayInputStream(roundedCornerRectGradient.getBytes()), GL_FRAGMENT_SHADER);
                case "round" -> createShader(new ByteArrayInputStream(roundedRect.getBytes()), GL_FRAGMENT_SHADER);
                case "out" -> createShader(new ByteArrayInputStream(roundedOut.getBytes()), GL_FRAGMENT_SHADER);
                case "shadow" -> createShader(new ByteArrayInputStream("""
                        #version 120
                        
                        uniform sampler2D sampler1;
                        uniform sampler2D sampler2;
                        uniform vec2 texelSize;
                        uniform vec2 direction;
                        uniform float radius;
                        uniform float kernel[256];
                        
                        void main(void)
                        {
                            vec2 uv = gl_TexCoord[0].st;                      
                            vec4 pixel_color = texture2D(sampler1, uv);
                            pixel_color.rgb *= pixel_color.a;
                            pixel_color *= kernel[0];
                        
                            for (float f = 1; f <= radius; f++) {
                                vec2 offset = f * texelSize * direction;
                                vec4 left = texture2D(sampler1, uv - offset);
                                vec4 right = texture2D(sampler1, uv + offset);
                                left.rgb *= left.a;
                                right.rgb *= right.a;
                                pixel_color += (left + right) * kernel[int(f)];
                            }
                        
                            gl_FragColor = vec4(pixel_color.rgb / pixel_color.a, pixel_color.a);
                        }
                        """.getBytes()), GL_FRAGMENT_SHADER);
                case "texture" -> createShader(new ByteArrayInputStream(texture.getBytes()), GL_FRAGMENT_SHADER);
                case "customRoundedGradient" -> createShader(new ByteArrayInputStream(customRoundedGradient.getBytes()), GL_FRAGMENT_SHADER);
                case "blur" -> createShader(new ByteArrayInputStream("""
                        #version 120
                        
                        uniform sampler2D textureIn;
                        uniform sampler2D textureOut;
                        uniform vec2 texelSize, direction;
                        uniform float radius, weights[256];
                        
                        #define offset texelSize * direction
                        
                        void main() {
                            vec2 uv = gl_TexCoord[0].st;
                            uv.y = 1.0f - uv.y;
                        
                            float alpha = texture2D(textureOut, uv).a;
                            if (direction.x == 0.0 && alpha == 0.0) {
                                discard;
                            }
                        
                            vec3 color = texture2D(textureIn, gl_TexCoord[0].st).rgb * weights[0];
                            float totalWeight = weights[0];
                        
                            for (float f = 1.0; f <= radius; f++) {
                                color += texture2D(textureIn, gl_TexCoord[0].st + f * offset).rgb * (weights[int(abs(f))]);
                                color += texture2D(textureIn, gl_TexCoord[0].st - f * offset).rgb * (weights[int(abs(f))]);
                        
                                totalWeight += (weights[int(abs(f))]) * 2.0;
                            }
                        
                            gl_FragColor = vec4(color / totalWeight, 1.0);
                        }
                        """.getBytes()), GL_FRAGMENT_SHADER);

                case "roundRectOutline" -> createShader(new ByteArrayInputStream(roundRectOutline.getBytes()), GL_FRAGMENT_SHADER);
                default -> createShader(mc.getResourceManager().getResource(new ResourceLocation("monoton/shaders/" + fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
            };
            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderID);

            ARBShaderObjects.glAttachObjectARB(programID, createShader(new ByteArrayInputStream(vertex.getBytes()), GL_VERTEX_SHADER));

            ARBShaderObjects.glLinkProgramARB(programID);
        } catch (IOException exception) {
            exception.fillInStackTrace();
            System.out.println("Ошибка при загрузке: " + fragmentShaderLoc);
        }
    }


    /**
     * Инициализация шейдеров при запуске клиента
     */

    @Compile
    @VMProtect(type = VMProtectType.VIRTUALIZATION)
    public static void init() {
        CORNER_ROUND_SHADER = new ShaderUtils("corner");
        TEXTURE_ROUND_SHADER = new ShaderUtils("texture");
        ROUND_SHADER = new ShaderUtils("round");
        ROUND_SHADER_OUTLINE = new ShaderUtils("roundRectOutline");
    }

    public int getUniform(final String name) {
        return ARBShaderObjects.glGetUniformLocationARB(this.programID, (CharSequence) name);
    }

    public void attach() {
        ARBShaderObjects.glUseProgramObjectARB(this.programID);
    }

    public void detach() {
        GL20.glUseProgram(0);
    }

    public void setUniform(final String name, final float... args) {
        final int loc = ARBShaderObjects.glGetUniformLocationARB(this.programID, (CharSequence) name);
        switch (args.length) {
            case 1: {
                ARBShaderObjects.glUniform1fARB(loc, args[0]);
                break;
            }
            case 2: {
                ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
                break;
            }
            case 3: {
                ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
                break;
            }
            case 4: {
                ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
                break;
            }
        }
    }

    public void setUniform(final String name, final int... args) {
        final int loc = ARBShaderObjects.glGetUniformLocationARB(this.programID, (CharSequence) name);
        switch (args.length) {
            case 1: {
                ARBShaderObjects.glUniform1iARB(loc, args[0]);
                break;
            }
            case 2: {
                ARBShaderObjects.glUniform2iARB(loc, args[0], args[1]);
            }
            case 3: {
                ARBShaderObjects.glUniform3iARB(loc, args[0], args[1], args[2]);
                break;
            }
            case 4: {
                ARBShaderObjects.glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
                break;
            }
        }
    }

    public void setUniformf(final String var1, final float... args) {
        final int var2 = ARBShaderObjects.glGetUniformLocationARB(this.programID, (CharSequence) var1);
        switch (args.length) {
            case 1: {
                ARBShaderObjects.glUniform1fARB(var2, args[0]);
                break;
            }
            case 2: {
                ARBShaderObjects.glUniform2fARB(var2, args[0], args[1]);
                break;
            }
            case 3: {
                ARBShaderObjects.glUniform3fARB(var2, args[0], args[1], args[2]);
                break;
            }
            case 4: {
                ARBShaderObjects.glUniform4fARB(var2, args[0], args[1], args[2], args[3]);
                break;
            }
        }
    }

    public static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtils roundedTexturedShader) {

        roundedTexturedShader.setUniform("location", (float) (x * 2),
                (float) ((mc.getMainWindow().getHeight() - (height * 2)) - (y * 2)));
        roundedTexturedShader.setUniform("rectSize", (float) (width * 2), (float) (height * 2));
        roundedTexturedShader.setUniform("radius", (float) (radius * 2));
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.getMainWindow().getWidth() || framebuffer.framebufferHeight != mc.getMainWindow().getHeight()) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(Math.max(mc.getMainWindow().getWidth(), 1), Math.max(mc.getMainWindow().getHeight(), 1), true, false);
        }
        return framebuffer;
    }

    public static void update(Framebuffer framebuffer) {
        if (framebuffer.framebufferWidth != mc.getMainWindow().getWidth() || framebuffer.framebufferHeight != mc.getMainWindow().getHeight()) {
            framebuffer.createBuffers(mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), false);
        }
    }

    /**
     * Отрисовка квадрата
     */
    public void drawQuads(final float x,
                          final float y,
                          final float width,
                          final float height) {

        RenderUtilka.Render2D.quadsBegin(x, y, width, height, GL_QUADS);
    }

    public static ShaderUtils create(String fragmentShaderLoc) {
        return new ShaderUtils(fragmentShaderLoc);
    }


    public static void drawQuads() {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, (float) Math.max(mc.getMainWindow().getScaledHeight(), 1));
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f((float) Math.max(mc.getMainWindow().getScaledWidth(), 1), (float) Math.max(mc.getMainWindow().getScaledHeight(), 1));
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f((float) Math.max(mc.getMainWindow().getScaledWidth(), 1), 0.0F);
        GL11.glEnd();
    }

    private int createShader(InputStream inputStream, int shaderType) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        if (shaderType != GL20.GL_VERTEX_SHADER && shaderType != GL20.GL_FRAGMENT_SHADER) {
            throw new IllegalArgumentException("Invalid shader type: " + shaderType);
        }

        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        if (shader == 0) {
            throw new IllegalStateException("Failed to create shader object for type: " + shaderType);
        }

        try {
            String shaderSource = FileUtil.readInputStream(inputStream);
            ARBShaderObjects.glShaderSourceARB(shader, shaderSource);
            ARBShaderObjects.glCompileShaderARB(shader);

            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
                String errorLog = GL20.glGetShaderInfoLog(shader, 4096);
                ARBShaderObjects.glDeleteObjectARB(shader);
                throw new IllegalStateException(
                        String.format("Shader (type %d) compilation failed: %s", shaderType, errorLog));
            }

            return shader;
        } catch (IOException e) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw e;
        }
    }

    private final String roundedGradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color1, color2, color3, color4;
            uniform float radius;
            
            #define NOISE .5/255.0
            
            float roundSDF(vec2 p, vec2 b, float r) {
                return length(max(abs(p) - b , 0.0)) - r;
            }
            
            vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){
                vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
                // Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 st = gl_TexCoord[0].st;
                vec2 halfSize = rectSize * .5;
            
                float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - radius - 1., radius))) * color1.a;
                gl_FragColor = vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), smoothedAlpha);
            }""";

    private final String customRoundedGradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color1, color2, color3, color4;
            uniform vec4 radius;
            
            #define NOISE .5/255.0
            
            float roundSDF(vec2 p, vec2 b, vec4 r) {
                    r.xy = (p.x > 0.0) ? r.xy : r.zw;
                    r.x  = (p.y > 0.0) ? r.x  : r.y;
                    vec2 l = abs(p) - b + r.x;
                    return min(max(l.x, l.y), 0.0) + length(max(l, .0f)) - r.x;
            }
            
            vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){
                vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
                // Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 st = gl_TexCoord[0].st;
                vec2 halfSize = rectSize * .5;
            
                float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - 1., radius))) * color1.a;
                gl_FragColor = vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), smoothedAlpha);
            }""";

    private final String roundedOut = """
            #version 120
            
            // объявление переменных
            uniform vec2 size; // размер прямоугольника
            uniform vec4 round; // коэффициенты скругления углов
            uniform vec2 smoothness; // плавность перехода от цвета к прозрачности
            uniform float value; // значение, используемое для расчета расстояния до границы
            uniform vec4 color; // цвет прямоугольника
            uniform float outlineSize; // размер обводки
            uniform vec4 outlineColor; // цвет обводки
            
            // функция для расчета расстояния до границы
            float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
            }
            
            void main() {
                vec2 st = gl_TexCoord[0].st * size; // координаты текущего пикселя
                vec2 halfSize = 0.5 * size; // половина размера прямоугольника
                float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));
                // рассчитываем прозрачность в зависимости от расстояния до границы
                gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa); // устанавливаем цвет прямоугольника с прозрачностью sa
              \s
                // добавляем обводку
                vec2 outlineSizeVec = size + vec2(outlineSize);
                float outlineDist = test(halfSize - st, halfSize - value, round);
                float outline = smoothstep(smoothness.x, smoothness.y, outlineDist) - smoothstep(smoothness.x, smoothness.y, outlineDist - outlineSize);
                if (outlineDist < outlineSize)
                    gl_FragColor = mix(gl_FragColor, outlineColor, outline);
            }
            """;

    private final String roundRectOutline = """
            #version 120
                       \s
            uniform vec2 location, rectSize;
            uniform vec4 color, outlineColor1,outlineColor2,outlineColor3,outlineColor4;
            uniform float radius, outlineThickness;
            #define NOISE .5/255.0
            
            float roundedSDF(vec2 centerPos, vec2 size, float radius) {
                return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
            }
            
            vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4)
            {
                vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
                //Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                float distance = roundedSDF(gl_FragCoord.xy - location - (rectSize * .5), (rectSize * .5) + (outlineThickness * 0.5) - 1.0, radius);
            
                float blendAmount = smoothstep(0., 2., abs(distance) - (outlineThickness * 0.5));
                vec4 outlineColor = vec4(createGradient(gl_TexCoord[0].st, outlineColor1.rgb, outlineColor2.rgb, outlineColor3.rgb, outlineColor4.rgb), outlineColor1.a);
                vec4 insideColor = (distance < 0.) ? color : vec4(outlineColor.rgb,  0.0);
                gl_FragColor = mix(outlineColor, insideColor, blendAmount);
            }
            """;

    String roundedRect = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color;\n" +
            "uniform float radius;\n" +
            "uniform bool blur;\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b, 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 rectHalf = rectSize * 0.5;\n" +
            "    // Smooth the result (free antialiasing).\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1., radius))) * color.a;\n" +
            "    gl_FragColor = vec4(color.rgb, smoothedAlpha);// mix(quadColor, shadowColor, 0.0);\n" +
            "\n" +
            "}";
    String vertex = """
            #version 120      \s
            void main() {
                // Выборка данных из текстуры во фрагментном шейдере (координаты)
                gl_TexCoord[0] = gl_MultiTexCoord0;
                gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
            }
            """;


    String texture = """
            uniform vec2 rectSize; // Координаты и размер прямоугольника
            uniform sampler2D textureIn; // Входная текстура
            uniform float radius, alpha; // Радиус закругления углов прямоугольника и прозрачность
            
            // Создаем функцию для определения расстояния от текущей позиции до края прямоугольника
            float roundedSDF(vec2 centerPos, vec2 size, float radius) {
                return length(max(abs(centerPos) - size, 0.)) - radius;
            }
            
            void main() {
                // Определяем расстояние от текущей позиции до края прямоугольника
                float distance = roundedSDF((rectSize * .5) - (gl_TexCoord[0].st * rectSize), (rectSize * .5) - radius - 1., radius);
               \s
                // Создаем плавный переход от границы прямоугольника к прозрачной области
                float smoothedAlpha = (1.0 - smoothstep(0.0, 2.0, distance)) * alpha;
            
                // Создаем окончательный цвет пикселя, используя цвет из входной текстуры и плавный переход между границей прямоугольника и прозрачной областью
                gl_FragColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, smoothedAlpha);
            }
            """;

    String roundedCornerRect = """
            #version 120
                // объявление переменных
                uniform vec2 size; // размер прямоугольника
                uniform vec4 round; // коэффициенты скругления углов
                uniform vec2 smoothness; // плавность перехода от цвета к прозрачности
                uniform float value; // значение, используемое для расчета расстояния до границы
                uniform vec4 color; // цвет прямоугольника
            
                // функция для расчета расстояния до границы
                float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                    vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                    vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                    vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                    return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                }
            
            
                void main() {
                    vec2 st = gl_TexCoord[0].st * size; // координаты текущего пикселя
                    vec2 halfSize = 0.5 * size; // половина размера прямоугольника
                    float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));
                    // рассчитываем прозрачность в зависимости от расстояния до границы
                    gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa); // устанавливаем цвет прямоугольника с прозрачностью sa
                }""";

    String roundedCornerRectGradient = """
            #version 120
                // объявление переменных
                uniform vec2 size; // размер прямоугольника
                uniform vec4 round; // коэффициенты скругления углов
                uniform vec2 smoothness; // плавность перехода от цвета к прозрачности
                uniform float value; // значение, используемое для расчета расстояния до границы
                uniform vec4 color1; // цвет прямоугольника
                uniform vec4 color2; // цвет прямоугольника
                uniform vec4 color3; // цвет прямоугольника
                uniform vec4 color4; // цвет прямоугольника
                #define NOISE .5/255.0
                // функция для расчета расстояния до границы
                float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                    vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                    vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                    vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                    return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                }
            
                vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4)
            {
                vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
                //Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
                void main() {
                    vec2 st = gl_TexCoord[0].st * size; // координаты текущего пикселя
                    vec2 halfSize = 0.5 * size; // половина размера прямоугольника
                    float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));
                    // рассчитываем прозрачность в зависимости от расстояния до границы
                    vec4 color = createGradient(gl_TexCoord[0].st, color1, color2,color3,color4);
                    gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa); // устанавливаем цвет прямоугольника с прозрачностью sa
                }""";


}