package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = CharacterSelectScreen.class, method = "justSelected")
public class CharacterSelectedPatch {
    public static void Postfix(CharacterSelectScreen __instance) {
        VibrationManager.trigger(VibrationManager.CHARACTER_SELECTED_ID);
    }
}
