package bitwigmods.pinnedtracks;

/**
 * Tests for DividerColour. Run: sh tools/test-java.sh pinned-tracks   (prints DIVIDER_COLOUR_TESTS_OK or fails)
 */
public final class DividerColourTest {
    private static int failures = 0;

    public static void main(String[] args) {
        float grey2 = 0.2f;
        // fully transparent line: the background stays
        near("alpha 0", DividerColour.over(rgba(grey2, grey2, grey2, 1), rgba(0, 0, 0, 0)), rgba(grey2, grey2, grey2, 1));
        // opaque black line: black
        near("alpha 1", DividerColour.over(rgba(grey2, grey2, grey2, 1), rgba(0, 0, 0, 1)), rgba(0, 0, 0, 1));
        // half-transparent black: half as bright, still opaque
        near("black 50%", DividerColour.over(rgba(0.4f, 0.4f, 0.4f, 1), rgba(0, 0, 0, 0.5f)), rgba(0.2f, 0.2f, 0.2f, 1));
        // white line at 25% (light grid setting)
        near("white 25%", DividerColour.over(rgba(0.2f, 0.2f, 0.2f, 1), rgba(1, 1, 1, 0.25f)), rgba(0.4f, 0.4f, 0.4f, 1));
        // out-of-range alpha is clamped
        near("alpha > 1", DividerColour.over(rgba(0.5f, 0.5f, 0.5f, 1), rgba(0, 0, 0, 3)), rgba(0, 0, 0, 1));

        if (failures > 0) {
            System.out.println("DIVIDER_COLOUR_TESTS_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("DIVIDER_COLOUR_TESTS_OK");
    }

    private static float[] rgba(float r, float g, float b, float a) {
        return new float[] {r, g, b, a};
    }

    private static void near(String name, float[] actual, float[] expected) {
        for (int i = 0; i < 4; i++) {
            if (Math.abs(actual[i] - expected[i]) > 1e-5) {
                failures++;
                System.out.println("FAIL: " + name + " channel " + i + ": expected " + expected[i] + " got " + actual[i]);
                return;
            }
        }
    }
}
