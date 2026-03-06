package stscontrollervibration.rumble;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class XInputManager {
    private static final Logger logger = LogManager.getLogger(XInputManager.class);
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_DEVICE_NOT_CONNECTED = 1167;
    private static final int MAX_USERS = 4;
    private static final long SCAN_INTERVAL_MS = 3000L;
    private static final String[] LIBRARIES = {"xinput1_4", "xinput1_3", "xinput1_2", "xinput1_1"};

    private static XInputLibrary library;
    private static String loadedLibrary = "";
    private static boolean loadAttempted = false;
    private static boolean loadFailedLogged = false;
    private static long lastScanMs = 0L;
    private static int[] connectedSlots = new int[0];
    private static String lastSlotSummary = "";

    private XInputManager() {
    }

    static void update() {
        scanConnectedSlots(false);
    }

    static boolean apply(float left, float right) {
        if (!ensureLoaded()) {
            return false;
        }

        scanConnectedSlots(false);
        if (connectedSlots.length == 0) {
            return false;
        }

        XInputVibration vibration = new XInputVibration(toWord(left), toWord(right));
        int targetSlot = connectedSlots[0];
        int result = library.XInputSetState(targetSlot, vibration);
        if (result == ERROR_SUCCESS) {
            return true;
        }
        if (result == ERROR_DEVICE_NOT_CONNECTED) {
            scanConnectedSlots(true);
            if (connectedSlots.length == 0) {
                return false;
            }

            targetSlot = connectedSlots[0];
            return library.XInputSetState(targetSlot, vibration) == ERROR_SUCCESS;
        }

        logger.warn("XInputSetState failed for slot {} with code {}", targetSlot, result);
        return false;
    }

    static String getStatusSummary() {
        ensureLoaded();
        scanConnectedSlots(false);

        if (library == null) {
            return "unavailable";
        }
        return loadedLibrary + " slots=" + Arrays.toString(connectedSlots);
    }

    private static boolean ensureLoaded() {
        if (library != null) {
            return true;
        }
        if (loadAttempted) {
            return false;
        }

        loadAttempted = true;
        Throwable lastFailure = null;
        for (String libraryName : LIBRARIES) {
            try {
                library = Native.loadLibrary(libraryName, XInputLibrary.class, W32APIOptions.DEFAULT_OPTIONS);
                loadedLibrary = libraryName;
                logger.info("Loaded XInput backend from {}", libraryName);
                scanConnectedSlots(true);
                return true;
            } catch (Throwable failure) {
                lastFailure = failure;
            }
        }

        if (!loadFailedLogged) {
            logger.warn("Failed to load any XInput backend", lastFailure);
            loadFailedLogged = true;
        }
        return false;
    }

    private static void scanConnectedSlots(boolean force) {
        if (!ensureLoaded()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!force && now - lastScanMs < SCAN_INTERVAL_MS) {
            return;
        }

        lastScanMs = now;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < MAX_USERS; i++) {
            XInputState state = new XInputState();
            int result = library.XInputGetState(i, state);
            if (result == ERROR_SUCCESS) {
                slots.add(i);
            }
        }

        connectedSlots = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            connectedSlots[i] = slots.get(i);
        }

        String summary = loadedLibrary + " slots=" + Arrays.toString(connectedSlots);
        if (!summary.equals(lastSlotSummary)) {
            lastSlotSummary = summary;
            logger.info("XInput scan result: {}", summary);
        }
    }

    private static short toWord(float intensity) {
        return (short) Math.round(Math.max(0.0f, Math.min(intensity, 1.0f)) * 65535.0f);
    }

    private interface XInputLibrary extends StdCallLibrary {
        int XInputGetState(int dwUserIndex, XInputState state);

        int XInputSetState(int dwUserIndex, XInputVibration vibration);
    }

    public static class XInputGamepad extends Structure {
        public short wButtons;
        public byte bLeftTrigger;
        public byte bRightTrigger;
        public short sThumbLX;
        public short sThumbLY;
        public short sThumbRX;
        public short sThumbRY;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                "wButtons",
                "bLeftTrigger",
                "bRightTrigger",
                "sThumbLX",
                "sThumbLY",
                "sThumbRX",
                "sThumbRY"
            );
        }
    }

    public static class XInputState extends Structure {
        public int dwPacketNumber;
        public XInputGamepad Gamepad = new XInputGamepad();

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwPacketNumber", "Gamepad");
        }
    }

    public static class XInputVibration extends Structure {
        public short wLeftMotorSpeed;
        public short wRightMotorSpeed;

        public XInputVibration() {
        }

        public XInputVibration(short left, short right) {
            this.wLeftMotorSpeed = left;
            this.wRightMotorSpeed = right;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("wLeftMotorSpeed", "wRightMotorSpeed");
        }
    }
}
