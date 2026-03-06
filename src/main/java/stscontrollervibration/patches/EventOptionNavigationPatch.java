package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = RoomEventDialog.class, method = "update")
public class EventOptionNavigationPatch {
    public static void Postfix(RoomEventDialog __instance) {
        if (shouldTrigger(RoomEventDialog.waitForInput, RoomEventDialog.optionList.size())) {
            VibrationManager.trigger(VibrationManager.EVENT_OPTION_NAVIGATED_ID);
        }
    }

    @SpirePatch2(clz = GenericEventDialog.class, method = "update")
    public static class GenericDialogPatch {
        public static void Postfix(GenericEventDialog __instance) {
            if (shouldTrigger(GenericEventDialog.waitForInput, __instance.optionList.size())) {
                VibrationManager.trigger(VibrationManager.EVENT_OPTION_NAVIGATED_ID);
            }
        }
    }

    private static boolean shouldTrigger(boolean waitForInput, int optionCount) {
        return Settings.isControllerMode
            && waitForInput
            && optionCount > 1
            && isEventSelectionContext()
            && isNavigationJustPressed();
    }

    private static boolean isEventSelectionContext() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room instanceof EventRoom || room instanceof NeowRoom;
    }

    private static boolean isNavigationJustPressed() {
        return CInputActionSet.up.isJustPressed()
            || CInputActionSet.down.isJustPressed()
            || CInputActionSet.altUp.isJustPressed()
            || CInputActionSet.altDown.isJustPressed();
    }
}
