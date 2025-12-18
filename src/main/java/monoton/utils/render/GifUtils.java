package monoton.utils.render;

/**
 * Utility class for calculating the current frame index of a GIF animation.
 */
public class GifUtils {

    private static final int MIN_TOTAL_FRAMES = 1;
    private static final int MIN_FRAME_DELAY = 1;

    /**
     * Calculates the current frame index for a GIF animation based on the current time.
     *
     * @param totalFrames   the total number of frames in the GIF (must be positive)
     * @param frameDelay    the delay per frame in milliseconds (must be positive)
     * @param countFromZero whether to return a zero-based index (true) or one-based index (false)
     * @return the current frame index (zero-based or one-based depending on countFromZero)
     * @throws IllegalArgumentException if totalFrames or frameDelay is less than 1
     */
    public int getFrame(int totalFrames, int frameDelay, boolean countFromZero) {
        if (totalFrames < MIN_TOTAL_FRAMES) {
            throw new IllegalArgumentException("totalFrames must be at least " + MIN_TOTAL_FRAMES);
        }
        if (frameDelay < MIN_FRAME_DELAY) {
            throw new IllegalArgumentException("frameDelay must be at least " + MIN_FRAME_DELAY);
        }

        long currentTime = System.currentTimeMillis();
        int frameIndex = (int) ((currentTime / frameDelay) % totalFrames);
        return countFromZero ? frameIndex : frameIndex + 1;
    }
}