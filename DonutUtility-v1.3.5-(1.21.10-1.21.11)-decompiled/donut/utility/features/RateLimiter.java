package donut.utility.features;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class RateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 250;
    private static final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private static long minuteStart = System.currentTimeMillis();

    private RateLimiter() {
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - minuteStart > 60000L) {
            minuteStart = now;
            requestsThisMinute.set(0);
        }
    }

    public static boolean tryAcquire() {
        if (requestsThisMinute.get() >= 250) {
            return false;
        }
        requestsThisMinute.incrementAndGet();
        return true;
    }

    public static boolean hasCapacity() {
        return requestsThisMinute.get() < 250;
    }

    public static boolean isRateLimited() {
        return requestsThisMinute.get() >= 250;
    }

    public static int getSecondsUntilReset() {
        if (!RateLimiter.isRateLimited()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - minuteStart;
        long remaining = 60000L - elapsed;
        return Math.max(0, (int)(remaining / 1000L));
    }

    public static int getRemaining() {
        return 250 - requestsThisMinute.get();
    }

    public static int getUsed() {
        return requestsThisMinute.get();
    }
}
