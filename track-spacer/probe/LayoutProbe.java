import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Flow test with Bitwig's REAL widget classes (loaded from the installed bitwig.jar, read only, no UI):
 * builds the box the hook builds and checks the gap height, also when the size changes live.
 * Uses Bitwig 6.1.7 (macOS) names via reflection, so it compiles without Bitwig on the class path.
 *
 * Run: sh tools/probe-layout.sh   (prints LAYOUT_PROBE_OK or fails)
 */
public final class LayoutProbe {
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        // 0.2.2 (and 0.2.0): spacers are direct children of the row box.
        Object box = box();
        Object above = spacer(box, 0);
        Object below = spacer(box, 0);
        setBackground(box);                                  // as the hook does (theme colour)
        same("no tag: box adds nothing", height(box), 0);
        size(above, 16);                                     // live: comment got "#spacer-before"
        same("live gap above", height(box), 16);
        size(below, 30);
        same("live gap above + below", height(box), 46);
        size(above, 0);
        size(below, 0);
        same("tag removed: back to 0", height(box), 0);

        // 0.2.1 (the bug): each spacer inside a box of its own. Such a box is 0 wide, and a row box
        // only lays out children that are spacers or larger than 0 in both directions -> dropped.
        Object outer = box();
        Object holder = box();
        Object inner = spacer(holder, 0);
        add(outer, holder);
        size(inner, 16);
        same("0.2.1 holder itself grows", height(holder), 16);
        same("0.2.1 bug reproduced: row box stays 0", height(outer), 0);

        if (failures > 0) {
            System.out.println("LAYOUT_PROBE_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("LAYOUT_PROBE_OK");
    }

    private static Object box() throws Exception {
        return call(null, "dXL", "sSO", new Class<?>[0]);                 // dXL.sSO(): top-to-bottom box
    }

    private static Object spacer(Object box, long px) throws Exception {
        return call(box, "dXL", "sCA", new Class<?>[] {long.class}, px);  // dXL.sCA(long): fixed spacer
    }

    private static void add(Object parent, Object child) throws Exception {
        call(parent, "dxu", "IvP", new Class<?>[] {Class.forName("dwF")}, child);
    }

    private static void size(Object spacer, long px) throws Exception {   // what TrackSpacerHooks.resize does
        call(spacer, "dZl", "sCA", new Class<?>[] {long.class}, px);
        call(spacer, "dZl", "MgQ", new Class<?>[] {long.class}, px);
    }

    private static void setBackground(Object box) throws Exception {      // dXM.beI(ekS) with a theme colour
        Field f = Class.forName("egG").getField("beI");
        call(box, "dXM", "beI", new Class<?>[] {Class.forName("ekS")}, f.get(null));
    }

    private static long height(Object widget) throws Exception {
        Object size = call(widget, "dwF", "jzF", new Class<?>[0]);         // preferred size
        String s = size.toString();                                       // LongSize[width=..,height=..]
        return Long.parseLong(s.substring(s.indexOf("height=") + 7, s.indexOf(']')));
    }

    private static Object call(Object target, String cls, String name, Class<?>[] types, Object... values) throws Exception {
        for (Class<?> k = Class.forName(cls); k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return m.invoke(target, values);
            } catch (NoSuchMethodException next) {
                // look in the superclass
            }
        }
        throw new NoSuchMethodException(cls + "." + name);
    }

    private static void same(String what, long actual, long expected) {
        if (actual != expected) {
            failures++;
            System.out.println("FAIL: " + what + ": got " + actual + ", expected " + expected);
        }
    }
}
