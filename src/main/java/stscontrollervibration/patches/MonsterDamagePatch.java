package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = AbstractMonster.class, method = "damage")
public class MonsterDamagePatch {
    public static void Postfix(AbstractMonster __instance, DamageInfo info) {
        if (__instance.lastDamageTaken <= 0) {
            return;
        }
        if (info == null || !(info.owner instanceof AbstractPlayer) || info.owner == __instance) {
            return;
        }

        VibrationManager.trigger(VibrationManager.ENEMY_HIT_ID, __instance.lastDamageTaken);
    }
}
