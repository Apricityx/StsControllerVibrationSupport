package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import stscontrollervibration.vibration.VibrationManager;

import java.util.IdentityHashMap;
import java.util.Map;

@SpirePatch2(clz = LargeDialogOptionButton.class, method = "update")
public class EventOptionSelectedPatch {
    private static final Map<LargeDialogOptionButton, Boolean> pressedBeforeUpdate = new IdentityHashMap<>();

    public static void Prefix(LargeDialogOptionButton __instance) {
        pressedBeforeUpdate.put(__instance, __instance.pressed);
    }

    public static void Postfix(LargeDialogOptionButton __instance) {
        if (!isEventSelectionContext() || __instance.isDisabled) {
            pressedBeforeUpdate.remove(__instance);
            return;
        }

        boolean wasPressed = Boolean.TRUE.equals(pressedBeforeUpdate.remove(__instance));
        if (!wasPressed && __instance.pressed) {
            VibrationManager.trigger(VibrationManager.EVENT_OPTION_SELECTED_ID);
        }
    }

    private static boolean isEventSelectionContext() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room instanceof EventRoom || room instanceof NeowRoom;
    }
}
