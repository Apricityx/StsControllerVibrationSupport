package stscontrollervibration.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import stscontrollervibration.vibration.VibrationManager;

@SpirePatch2(clz = CardGroup.class, method = "moveToDiscardPile")
public class CardDiscardedPatch {
    public static void Postfix(CardGroup __instance, AbstractCard c) {
        if (c == null) {
            return;
        }

        VibrationManager.trigger(VibrationManager.CARD_DISCARDED_ID);
    }
}
