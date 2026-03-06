package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = AbstractCreature.class, method = "decrementBlock")
public class DamageBlockedPatch {
    public static void Postfix(AbstractCreature __instance, DamageInfo info, int damageAmount, int __result) {
        int blockedAmount = Math.max(0, damageAmount - __result);
        if (blockedAmount <= 0) {
            return;
        }
        if (__instance.isPlayer) {
            VibrationManager.trigger(VibrationManager.DAMAGE_BLOCKED_ID, blockedAmount);
            return;
        }
        if (info != null && info.owner instanceof AbstractPlayer && info.owner != __instance) {
            VibrationManager.trigger(VibrationManager.DAMAGE_BLOCKED_ID, blockedAmount);
        }
    }
}
