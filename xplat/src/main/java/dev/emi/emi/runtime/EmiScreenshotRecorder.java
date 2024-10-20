package dev.emi.emi.runtime;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEventAction;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;

public class EmiScreenshotRecorder {
	private static final String SCREENSHOTS_DIRNAME = "screenshots";
	private static IntBuffer intBuffer;
	private static int[] data;

	/**
	 * Saves a screenshot to the game's `screenshots` directory, doing the appropriate setup so that anything rendered in renderer will be captured
	 * and saved.
	 * <p>
	 * <b>Note:</b> the path can have <code>/</code> characters, indicating subdirectories. Java handles these correctly on Windows. The path should
	 * <b>not</b> contain the <code>.png</code> extension, as that will be added after checking for duplicates. If a file with this path already
	 * exists, then path will be suffixed with a <code>_#</code>, before adding the <code>.png</code> extension, where <code>#</code> represents an
	 * increasing number to avoid conflicts.
	 * <p>
	 * <b>Note 2:</b> The width and height parameters are reflected in the viewport when rendering. But the EMI-config
	 * <code>ui.recipe-screenshot-scale</code> value causes the resulting image to be scaled.
	 *
	 * @param path     the path to save the screenshot to, without extension.
	 * @param width    the width of the screenshot, not counting EMI-config scale.
	 * @param height   the height of the screenshot, not counting EMI-config scale.
	 * @param renderer a function to render the things being screenshotted.
	 */
	public static void saveScreenshot(String path, int width, int height, Runnable renderer) {
		saveScreenshotInner(path, width, height, renderer);
	}

	private static void saveScreenshotInner(String path, int width, int height, Runnable renderer) {
		MinecraftClient client = MinecraftClient.getInstance();

		int scale;
		if (EmiConfig.recipeScreenshotScale < 1) {
			scale = EmiPort.getGuiScale(client);
		} else {
			scale = EmiConfig.recipeScreenshotScale;
		}

		Framebuffer framebuffer = new Framebuffer(width * scale, height * scale, true);
		framebuffer.setClearColor(0f, 0f, 0f, 0f);
		framebuffer.attachTexture(width * scale, height * scale);

		framebuffer.bind(true);

		MatrixStack view = RenderSystem.getModelViewStack();
		view.push();
		//view.translate(-1.0, 1.0, 0.0);
		//todo find right scale
		view.scale(scale + 2, scale + 2, 0);
		//view.translate(0.0, 0.0, 10.0);
		RenderSystem.applyModelViewMatrix();
		renderer.run();

		view.pop();
		RenderSystem.applyModelViewMatrix();

		framebuffer.unbind();
		client.getFramebuffer().bind(true);

		client.inGameHud.getChatHud().addMessage(ScreenshotUtils.saveScreenshot(client.runDirectory, width * scale, height * scale, framebuffer));
		/*saveScreenshotInner(client.runDirectory, path, framebuffer,
			message -> client.method_6635(() -> client.inGameHud.getChatHud().addMessage(message)));*/
	}

	private static void saveScreenshotInner(File gameDirectory, String suggestedPath, Framebuffer framebuffer, Consumer<Text> messageReceiver) {
		BufferedImage image = takeScreenshot(framebuffer);

		File screenshots = new File(gameDirectory, SCREENSHOTS_DIRNAME);
		screenshots.mkdir();

		String filename = getScreenshotFilename(screenshots, suggestedPath);
		File file = new File(screenshots, filename);

		// Make sure the parent file exists. Note: `/`s in suggestedPath are valid, as they indicate subdirectories. Java even translates this
		// correctly on Windows.
		File parent = file.getParentFile();
		parent.mkdirs();

		try {
			ImageIO.write(image, "png", file);

			Text text = EmiPort.literal(filename,
				new Style().setUnderline(true).setClickEvent(new ClickEvent(ClickEventAction.OPEN_FILE, file.getAbsolutePath())));
			messageReceiver.accept(EmiPort.translatable("screenshot.success", text));
		} catch (Throwable e) {
			EmiLog.error("Failed to write screenshot");
			e.printStackTrace();
			messageReceiver.accept(EmiPort.translatable("screenshot.failure", e.getMessage()));
		} finally {
			image.flush();
		}
	}

	private static BufferedImage takeScreenshot(Framebuffer framebuffer) {
		int width = framebuffer.textureWidth;
		int height = framebuffer.textureHeight;

		int size = width * height;
		if (intBuffer == null || intBuffer.capacity() < size) {
			intBuffer = BufferUtils.createIntBuffer(size);
			data = new int[size];
		}

		GL11.glPixelStorei(3333, 1);
		GL11.glPixelStorei(3317, 1);
		intBuffer.clear();

		GL11.glBindTexture(3553, framebuffer.colorAttachment);
		GL11.glGetTexImage(3553, 0, 32993, 33639, intBuffer);
		intBuffer.get(data);

		BufferedImage image = new BufferedImage(framebuffer.viewportWidth, framebuffer.viewportHeight, 1);
		int i = (height - framebuffer.viewportHeight);
		for (int y = i; y < height; y++) {
			for (int x = 0; x < framebuffer.viewportWidth; x++) {
				image.setRGB(x, y - i, data[y * y + x]);
			}
		}

		return image;
	}

	private static String getScreenshotFilename(File directory, String path) {
		int i = 1;
		while ((new File(directory, path + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
			++i;
		}
		return path + (i == 1 ? "" : "_" + i) + ".png";
	}
}
