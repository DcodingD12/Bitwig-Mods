package bitwigmods.scalingdrag;

import bitwig.obf.dPn; // the number field widget: the "100%" value in Settings > Display > Scaling
import bitwig.obf.dSt; // builds the Scaling row in Settings > Display: "-", the value, "+"
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;

/**
 * Settings: Drag Display Scaling.
 *
 * Bitwig builds the scaling value as a normal number field, but switches off its
 * "can_adjust_value_with_mouse" property on purpose (the call tWW(false) in the row builder).
 * This mod replaces exactly that one call and passes true, so the value can be dragged
 * up and down with the mouse like any other number field.
 *
 * Important: Bitwig itself rounds the scale to quarter steps (25%) in its core
 * (com.bitwig.base.geom.YEs: round(scale * 4)). Dragging therefore still moves in 25% steps;
 * it only replaces clicking "-" / "+". Finer steps are NOT possible with this hook.
 *
 * Names are Bitwig 6.1.7 (macOS). In 6.1.4 names: dSt = dQL, dPn = dNN, tWW(Z)V = LaE(Z)V.
 */
public final class ScalingDragHooks {
    private static final Bml.Mod MOD = Bml.mod("settings-scaling-drag");

    private ScalingDragHooks() {
    }

    // dSt.Gef(LdSp;)V: builds the row for one display (dSp = that display's scaling model).
    // Its first call to tWW(Z)V on the number field is the "can adjust value with mouse" switch.
    @Hook(id = "allow-mouse-drag", target = dSt.class, method = "Gef(LdSp;)V", at = At.REPLACE_CALL,
            callOwner = dPn.class, call = "tWW(Z)V", occurrence = 1)
    public static void allowMouseDrag(dPn valueField, boolean bitwigValue) {
        try {
            valueField.tWW(true); // can adjust value with mouse: on (Bitwig passes false here)
        } catch (Throwable t) {
            MOD.log("could not enable mouse drag on the scaling field (Bitwig wanted " + bitwigValue + ")", t);
        }
    }
}
