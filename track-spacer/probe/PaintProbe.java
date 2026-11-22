import com.bitwig.base.geom.ijY;
import com.bitwig.base.geom.mLC;
import com.bitwig.base.geom.uXa;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Paint test with Bitwig's REAL classes (installed bitwig.jar, read only, no UI): builds the box the
 * hook builds around a 100 x 40 stand-in content, lays it out and paints it into a recording graphics
 * object. Checks WHICH rectangles get filled and with WHICH colour.
 * Compiled against bitwig.jar (Bitwig 6.1.7 macOS names; this file is in the default package on purpose).
 *
 * Run: sh tools/probe-layout.sh   (prints PAINT_PROBE_OK or fails)
 */
public final class PaintProbe {
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        dXL content = fixed(100, 40); // stand-in for a track header/row content of 100 x 40

        // Exactly what TrackSpacerHooks.withSpacers builds:
        dXL box = dXL.sSO();
        dZl above = box.sCA(0L);
        box.IvP(content);
        dZl below = box.sCA(0L);
        egG colour = egM.BBg().zzi(); // theme "Grey 1" (in the hook: the widget's own theme, mum().zzi())
        box.beI((ekS) colour);
        resize(above, 16);
        resize(below, 8);

        box.beI(0L, 0L, 100L, 64L);                    // bounds
        call(box, dXL.class, "iK_", new Class<?>[0]);  // layout
        same("content placed below the upper gap", (Long) call(content, dwF.class, "KLw", new Class<?>[0]), 16);

        List<String> painted = paint(box, 100, 64);
        List<String> expected = List.of(
                "colour 0.102,0.102,0.102",   // Grey 1 = 26/255 = #1A1A1A in Bitwig's default theme
                "rect 0,0,100,16", "fill",    // upper gap, full width
                "rect 0,56,100,8", "fill");   // lower gap, full width; nothing under the content (16..56)
        if (!painted.equals(expected)) {
            failures++;
            System.out.println("FAIL: painted " + painted + ", expected " + expected);
        }

        // Group track (buH): the content is the whole group block, header on top of the fold-out
        // container with the group's tracks. "after" must come below the END of the group.
        groupCase("open group", 60, List.of("colour 0.102,0.102,0.102",
                "rect 0,0,100,16", "fill",     // before: above the group header
                "rect 0,106,100,8", "fill"));  // after: below header (30) + tracks (60)
        groupCase("closed group", 0, List.of("colour 0.102,0.102,0.102",
                "rect 0,0,100,16", "fill",
                "rect 0,46,100,8", "fill"));   // closed: right below the header

        if (failures > 0) {
            System.out.println("PAINT_PROBE_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("PAINT_PROBE_OK");
    }

    /** A group block [header 100x30, tracks 100 x tracksHeight] wrapped like the hook does, laid out and painted. */
    private static void groupCase(String what, long tracksHeight, List<String> expected) throws Exception {
        dXL block = dXL.sSO();
        block.byf(0); // stand-in block without inner spacing, so the expected numbers are exact
        block.IvP(fixed(100, 30));
        if (tracksHeight > 0) {
            block.IvP(fixed(100, tracksHeight)); // a closed group's container is 0 high (left out)
        }
        dXL box = dXL.sSO();
        dZl above = box.sCA(0L);
        box.IvP(block);
        dZl below = box.sCA(0L);
        box.beI((ekS) egM.BBg().zzi());
        resize(above, 16);
        resize(below, 8);
        long h = 16 + 30 + tracksHeight + 8;
        box.beI(0L, 0L, 100L, h);
        call(box, dXL.class, "iK_", new Class<?>[0]);
        call(block, dXL.class, "iK_", new Class<?>[0]);
        List<String> painted = paint(box, 100, h);
        if (!painted.equals(expected)) {
            failures++;
            System.out.println("FAIL: " + what + ": painted " + painted + ", expected " + expected);
        }
    }

    /** A widget of a fixed size (a box that reports this size), standing in for Bitwig's content. */
    private static dXL fixed(long w, long h) throws Exception {
        dXL widget = new dXL() {
            @Override protected ijY nj_() { return new ijY(w, h); }
            @Override protected ijY iI_() { return new ijY(w, h); }
            @Override protected ijY mL_() { return new ijY(w, h); }
            @Override protected ijY iH_() { return new ijY(w, h); }
        };
        call(widget, dXL.class, "MgQ", new Class<?>[] {mLC.class}, mLC.beI);
        call(widget, dwF.class, "H_", new Class<?>[0]);
        return widget;
    }

    private static void resize(dZl spacer, long px) {
        spacer.sCA(px);
        spacer.MgQ(px);
    }

    /** Calls the box's own paint (dXL.beI(graphics)) with a graphics object that records what is drawn. */
    private static List<String> paint(dXL box, long w, long h) throws Exception {
        List<String> log = new ArrayList<>();
        Class<?> g = Class.forName("com.bitwig.graphics.ijY");
        Object graphics = Proxy.newProxyInstance(PaintProbe.class.getClassLoader(), new Class<?>[] {g}, (proxy, m, a) -> {
            String n = m.getName();
            if (n.equals("ny_")) {
                return new uXa(0, 0, w, h); // clip: the whole box
            }
            if (n.equals("sCA") && a != null && a.length == 1 && a[0] instanceof SZB c) {
                log.add(String.format(java.util.Locale.ROOT, "colour %.3f,%.3f,%.3f", c.MgQ(), c.BBg(), c.unz()));
            } else if (n.equals("beI") && a != null && a.length == 4 && a[0] instanceof Long) {
                log.add("rect " + a[0] + "," + a[1] + "," + a[2] + "," + a[3]);
            } else if (n.equals("scr") && (a == null || a.length == 0)) {
                log.add("fill");
            }
            Class<?> r = m.getReturnType();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == long.class) return 0L;
            if (r == double.class) return 0d;
            if (r == float.class) return 0f;
            return null;
        });
        call(box, dXL.class, "beI", new Class<?>[] {g}, graphics);
        return log;
    }

    private static Object call(Object target, Class<?> start, String name, Class<?>[] types, Object... values) throws Exception {
        for (Class<?> k = start; k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return m.invoke(target, values);
            } catch (NoSuchMethodException next) {
                // look in the superclass
            }
        }
        throw new NoSuchMethodException(start.getName() + "." + name);
    }

    private static void same(String what, long actual, long expected) {
        if (actual != expected) {
            failures++;
            System.out.println("FAIL: " + what + ": got " + actual + ", expected " + expected);
        }
    }
}
