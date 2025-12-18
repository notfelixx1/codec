package monoton.utils.render.shader;


import monoton.utils.IMinecraft;

public class KawaseBlur implements IMinecraft {
    public static KawaseBlur blur = new KawaseBlur();

    public final CustomFramebuffer BLURRED;
    public final CustomFramebuffer ADDITIONAL;

    public KawaseBlur() {
        BLURRED = new CustomFramebuffer(false).setLinear();
        ADDITIONAL = new CustomFramebuffer(false).setLinear();
    }

    public void updateBlur(float offset, int steps) {
        ADDITIONAL.setup();
        FRAMEBUFFER.bindFramebufferTexture();
        ShaderUtil.light_kawase_down.attach();
        ShaderUtil.light_kawase_down.setUniform("offset", offset);
        ShaderUtil.light_kawase_down.setUniformf("resolution", 1f / FRAMEBUFFER.framebufferTextureWidth, 1f / FRAMEBUFFER.framebufferTextureHeight);
        CustomFramebuffer.drawQuads();
        CustomFramebuffer[] buffers = {ADDITIONAL, BLURRED};
        for (int i = 1; i < steps; ++i) {
            int step = i % 2;
            buffers[step].setup();
            buffers[(step + 1) % 2].draw();
        }
        ShaderUtil.light_kawase_down.detach();
        ShaderUtil.light_kawase_up.attach();
        ShaderUtil.light_kawase_up.setUniform("offset", offset);
        ShaderUtil.light_kawase_up.setUniformf("resolution", 1f / FRAMEBUFFER.framebufferTextureWidth, 1f / FRAMEBUFFER.framebufferTextureHeight);
        for (int i = 0; i < steps; ++i) {
            int step = i % 2;
            buffers[(step + 1) % 2].setup();
            buffers[step].draw();
        }
        ShaderUtil.light_kawase_up.detach();
        FRAMEBUFFER.bindFramebuffer(false);
    }
}