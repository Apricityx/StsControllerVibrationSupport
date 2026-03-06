package stscontrollervibration.rumble;

import com.codedisaster.steamworks.SteamControllerHandle;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.steamInput.SteamInputHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RumbleManager {
    private static final Logger logger = LogManager.getLogger(RumbleManager.class);
    private static final float UNAPPLIED = -1.0f;
    private static final int UINT16_MAX = 65535;

    private static final List<RumbleEffect> queuedEffects = new ArrayList<>();
    private static float appliedLeft = UNAPPLIED;
    private static float appliedRight = UNAPPLIED;
    private static boolean outputFailureLogged = false;
    private static boolean environmentLogged = false;
    private static boolean noBackendLogged = false;

    private RumbleManager() {
    }

    public static void logEnvironment() {
        if (environmentLogged) {
            return;
        }
        environmentLogged = true;

        logger.info(
            "Rumble environment: preferredController=\""
                + getPreferredControllerName()
                + "\" steamAlive=" + SteamInputHelper.alive
                + " steamHandlePresent=" + (SteamInputHelper.handle != null)
                + " xinput=" + XInputManager.getStatusSummary()
                + " os.name=" + System.getProperty("os.name")
                + " os.version=" + System.getProperty("os.version")
        );
    }

    public static void queue(float left, float right, long durationMs) {
        queue(left, right, durationMs, 0L);
    }

    public static void queue(float left, float right, long durationMs, long delayMs) {
        if (durationMs <= 0L) {
            return;
        }
        if (left <= 0.0f && right <= 0.0f) {
            return;
        }

        long startTimeMs = System.currentTimeMillis() + Math.max(0L, delayMs);
        queue(new RumbleEffect(left, right, startTimeMs, startTimeMs + durationMs));
    }

    public static void update() {
        long now = System.currentTimeMillis();
        float targetLeft = 0.0f;
        float targetRight = 0.0f;
        XInputManager.update();

        Iterator<RumbleEffect> iterator = queuedEffects.iterator();
        while (iterator.hasNext()) {
            RumbleEffect effect = iterator.next();
            if (now >= effect.endTimeMs) {
                iterator.remove();
                continue;
            }
            if (now < effect.startTimeMs) {
                continue;
            }

            targetLeft = Math.max(targetLeft, effect.left);
            targetRight = Math.max(targetRight, effect.right);
        }

        applyIfNeeded(targetLeft, targetRight);
    }

    private static void queue(RumbleEffect effect) {
        queuedEffects.add(effect);
    }

    private static void applyIfNeeded(float left, float right) {
        left = clamp(left, 0.0f, 1.0f);
        right = clamp(right, 0.0f, 1.0f);

        if (Float.compare(left, appliedLeft) == 0 && Float.compare(right, appliedRight) == 0) {
            return;
        }

        boolean applied = applySteam(left, right);
        if (!applied) {
            applied = applyXInput(left, right);
        }

        if (applied || (left == 0.0f && right == 0.0f)) {
            appliedLeft = left;
            appliedRight = right;
            outputFailureLogged = false;
            if (left == 0.0f && right == 0.0f) {
                noBackendLogged = false;
            }
            return;
        }

        appliedLeft = UNAPPLIED;
        appliedRight = UNAPPLIED;
        if ((left > 0.0f || right > 0.0f) && !noBackendLogged) {
            logger.warn(
                "No rumble backend could handle vibration"
                    + " preferredController=\"" + getPreferredControllerName() + "\""
                    + " steamAlive=" + SteamInputHelper.alive
                    + " steamHandlePresent=" + (SteamInputHelper.handle != null)
                    + " xinput=" + XInputManager.getStatusSummary()
            );
            noBackendLogged = true;
        }
    }

    private static boolean applySteam(float left, float right) {
        if (!SteamInputHelper.alive || SteamInputHelper.controller == null) {
            return false;
        }

        SteamControllerHandle handle = SteamInputHelper.handle;
        if (handle == null && SteamInputHelper.controllerHandles != null && SteamInputHelper.numControllers > 0) {
            handle = SteamInputHelper.controllerHandles[0];
        }
        if (handle == null) {
            return false;
        }

        try {
            SteamInputHelper.controller.triggerVibration(handle, toSteamMotorValue(left), toSteamMotorValue(right));
            return true;
        } catch (Exception ex) {
            if (!outputFailureLogged) {
                logger.warn("Failed to send Steam Input vibration", ex);
                outputFailureLogged = true;
            }
            return false;
        }
    }

    private static boolean applyXInput(float left, float right) {
        try {
            return XInputManager.apply(left, right);
        } catch (Exception ex) {
            if (!outputFailureLogged) {
                logger.warn("Failed to send XInput vibration", ex);
                outputFailureLogged = true;
            }
            return false;
        }
    }

    private static String getPreferredControllerName() {
        if (CInputHelper.controller != null && CInputHelper.controller.getName() != null) {
            return CInputHelper.controller.getName();
        }
        return "";
    }

    private static short toSteamMotorValue(float intensity) {
        // Steam Input expects an unsigned short in the full 0-65535 range.
        return (short) Math.round(clamp(intensity, 0.0f, 1.0f) * UINT16_MAX);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class RumbleEffect {
        private final float left;
        private final float right;
        private final long startTimeMs;
        private final long endTimeMs;

        private RumbleEffect(float left, float right, long startTimeMs, long endTimeMs) {
            this.left = left;
            this.right = right;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
        }
    }
}
