package monoton.utils.render.shader;

public class TimeUtil {
    private long lastMS;
    private final long ms = System.currentTimeMillis();

    public TimeUtil() {
        reset();
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }


    public long getTimePassed() {
        return System.currentTimeMillis() - lastMS;
    }

    public void setTime(long time) {
        lastMS = time;
    }


    public boolean hasTimeElapsed(long time) {
        return getTimePassed() >= time;
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        boolean hasElapsed = getTimePassed() >= time;
        if (hasElapsed && reset) {
            reset();
        }
        return hasElapsed;
    }
    public boolean hasTimeElapsed() {
        return ms < System.currentTimeMillis();
    }

    public boolean hasReached(double milliseconds) {
        return getTimePassed() >= milliseconds;
    }

}