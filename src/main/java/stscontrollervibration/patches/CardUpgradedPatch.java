package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = UpgradeShineEffect.class, method = SpirePatch.CONSTRUCTOR)
public class CardUpgradedPatch {
    public static void Postfix(UpgradeShineEffect __instance, float x, float y) {
        VibrationManager.trigger(VibrationManager.CARD_UPGRADED_ID);
    }
}
