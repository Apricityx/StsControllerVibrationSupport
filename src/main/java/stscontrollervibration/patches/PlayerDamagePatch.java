package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = AbstractPlayer.class, method = "damage")
public class PlayerDamagePatch {
    public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
        if (__instance.lastDamageTaken > 0) {
            VibrationManager.trigger(VibrationManager.PLAYER_DAMAGED_ID, __instance.lastDamageTaken);
        }
    }
}
