package monoton.utils.font.styled;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.math.vector.Matrix4f;
import monoton.utils.font.common.AbstractFont;

public final class GlyphPage extends AbstractFont {

	private final int italicSpacing;
	private final float stretching, spacing, lifting;

	public GlyphPage(Font font, char[] chars, float stretching, float spacing, float lifting, boolean antialiasing) {
		FontRenderContext fontRenderContext = new FontRenderContext(font.getTransform(), true, true);
		double maxWidth = 0;
		double maxHeight = 0;

		for (char c : chars) {
			Rectangle2D bound = font.getStringBounds(Character.toString(c), fontRenderContext);
			maxWidth = Math.max(maxWidth, bound.getWidth());
			maxHeight = Math.max(maxHeight, bound.getHeight());
		}

		this.italicSpacing = font.isItalic() ? 5 : 0;
		int d = (int) Math.ceil(Math.sqrt((maxHeight + 2) * (maxWidth + 2 + italicSpacing) * chars.length));

		this.fontName = font.getFontName(Locale.ENGLISH);
		this.fontHeight = (float) (maxHeight / 2);
		this.imgHeight = d;
		this.imgWidth = d;
		this.stretching = stretching;
		this.spacing = spacing;
		this.lifting = lifting;
		this.antialiasing = antialiasing;
		BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = setupGraphics(image, font);

		FontMetrics fontMetrics = graphics.getFontMetrics();
		int posX = 1;
		int posY = 2;

		for (char c : chars) {
			Glyph glyph = new Glyph();
			Rectangle2D bounds = fontMetrics.getStringBounds(Character.toString(c), graphics);
			glyph.width = (int) bounds.getWidth() + 1 + italicSpacing;
			glyph.height = (int) bounds.getHeight() + 2;

			if (posX + glyph.width >= imgWidth) {
				posX = 1;
				posY += maxHeight + fontMetrics.getDescent() + 2;
			}

			glyph.x = posX;
			glyph.y = posY;

			graphics.drawString(Character.toString(c), posX, posY + fontMetrics.getAscent());

			posX += glyph.width + 4;
			glyphs.put(c, glyph);
		}

		RenderSystem.recordRenderCall(() -> setTexture(image));
	}

	@Override
	public float renderGlyph(final Matrix4f matrix, final char c, final float x, final float y, final float red, final float green, final float blue, final float alpha) {
		RenderSystem.enableTexture();
		RenderSystem.bindTexture(this.texId);
		final float w = super.renderGlyph(matrix, c, x, y, red, green, blue, alpha) - this.italicSpacing;
		RenderSystem.bindTexture(0);
		return w;
	}

	@Override
	public float getStretching() {
		return this.stretching;
	}

	@Override
	public float getSpacing() {
		return this.spacing;
	}

	public float getLifting() {
		return fontHeight + lifting;
	}
}

