package bitwigmods.pinnedtracks;

/**
 * The divider's colour: a semi-transparent line colour painted over an opaque background, as one
 * opaque colour. Plain Java (colours as {r, g, b, alpha}, 0..1), so it can be tested alone.
 */
final class DividerColour {
    private DividerColour() {
    }

    /** {@code line} (with its alpha) painted over {@code background}; keeps the background's alpha. */
    static float[] over(float[] background, float[] line) {
        float a = clamp(line[3]);
        return new float[] {
                clamp(background[0] * (1 - a) + line[0] * a),
                clamp(background[1] * (1 - a) + line[1] * a),
                clamp(background[2] * (1 - a) + line[2] * a),
                clamp(background[3])
        };
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
