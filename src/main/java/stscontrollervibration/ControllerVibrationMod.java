package stscontrollervibration;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stscontrollervibration.localization.LocalizationManager;
import stscontrollervibration.rumble.RumbleManager;
import stscontrollervibration.vibration.VibrationManager;

@SpireInitializer
public class ControllerVibrationMod implements PostInitializeSubscriber, PostUpdateSubscriber {
    public static final String MOD_ID = "sts_controller_vibration";
    private static final Logger logger = LogManager.getLogger(ControllerVibrationMod.class);

    public ControllerVibrationMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new ControllerVibrationMod();
        logger.info(MOD_ID + " initialized");
    }

    @Override
    public void receivePostInitialize() {
        LocalizationManager.initialize();
        VibrationManager.initialize();
        logger.info(
            MOD_ID
                + " ready"
                + " os.name=" + System.getProperty("os.name")
                + " os.version=" + System.getProperty("os.version")
                + " language=" + Settings.language
                + " user.dir=" + System.getProperty("user.dir")
                + " vibrations=" + VibrationManager.describeRegisteredVibrations()
        );
        RumbleManager.logEnvironment();
    }

    @Override
    public void receivePostUpdate() {
        RumbleManager.update();
    }
}
