package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.core.AbstractCreature;
import stscontrollervibration.vibration.VibrationManager;

import java.util.IdentityHashMap;
import java.util.Map;

@SpirePatch2(clz = AbstractCreature.class, method = "addBlock")
public class PlayerBlockGainedPatch {
    private static final Map<AbstractCreature, Integer> blockBeforeGain = new IdentityHashMap<>();

    public static void Prefix(AbstractCreature __instance, int blockAmount) {
        blockBeforeGain.put(__instance, __instance.currentBlock);
    }

    public static void Postfix(AbstractCreature __instance, int blockAmount) {
        Integer previousBlock = blockBeforeGain.remove(__instance);
        int blockBefore = previousBlock == null ? __instance.currentBlock : previousBlock;
        int blockGained = __instance.currentBlock - blockBefore;

        if (__instance.isPlayer && blockGained > 0) {
            VibrationManager.trigger(VibrationManager.PLAYER_BLOCK_GAINED_ID, blockGained);
        }
    }
}
