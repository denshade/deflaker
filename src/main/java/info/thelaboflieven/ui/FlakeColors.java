package info.thelaboflieven.ui;

import java.awt.Color;

/** Distinct colors for unique flake patterns (pass uses green elsewhere). */
public final class FlakeColors {

    private static final Color[] PALETTE = {
        new Color(220, 53, 69),
        new Color(255, 127, 14),
        new Color(147, 51, 234),
        new Color(236, 72, 153),
        new Color(202, 138, 4),
        new Color(14, 165, 233),
        new Color(20, 184, 166),
        new Color(239, 68, 68),
        new Color(168, 85, 247),
        new Color(244, 63, 94),
    };

    private FlakeColors() {}

    public static Color forIndex(int index) {
        if (index < 0) {
            return new Color(220, 53, 69);
        }
        return PALETTE[Math.floorMod(index, PALETTE.length)];
    }
}
