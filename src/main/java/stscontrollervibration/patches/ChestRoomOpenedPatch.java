package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;
import stscontrollervibration.vibration.VibrationManager;

import java.util.IdentityHashMap;
import java.util.Map;

public class ChestRoomOpenedPatch {
    private static final Map<TreasureRoom, Boolean> treasureRoomState = new IdentityHashMap<>();
    private static final Map<TreasureRoomBoss, Boolean> bossTreasureRoomState = new IdentityHashMap<>();

    @SpirePatch2(clz = TreasureRoom.class, method = "update")
    public static class TreasureRoomPatch {
        public static void Postfix(TreasureRoom __instance) {
            updateState(treasureRoomState, __instance, __instance.chest != null && __instance.chest.isOpen);
        }
    }

    @SpirePatch2(clz = TreasureRoomBoss.class, method = "update")
    public static class BossTreasureRoomPatch {
        public static void Postfix(TreasureRoomBoss __instance) {
            updateState(bossTreasureRoomState, __instance, __instance.chest != null && __instance.chest.isOpen);
        }
    }

    private static <T> void updateState(Map<T, Boolean> stateMap, T room, boolean isOpen) {
        boolean wasOpen = Boolean.TRUE.equals(stateMap.get(room));
        if (!wasOpen && isOpen) {
            VibrationManager.trigger(VibrationManager.CHEST_OPENED_ID);
        }
        stateMap.put(room, isOpen);
    }
}
