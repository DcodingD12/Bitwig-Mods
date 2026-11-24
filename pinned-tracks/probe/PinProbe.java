import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.zip.ZipFile;

/**
 * Flow test of pinned-tracks' building blocks with Bitwig's REAL classes (installed bitwig.jar, read only, no UI).
 * Compiled against bitwig.jar (default package, Bitwig 6.1.7 names).
 *
 * 1. Visibility rules as the hook builds them (row: "Bitwig's rule AND pinned" / "... AND NOT pinned",
 *    area: "any pinned row shown"), driven by a stand-in for a track comment: values and change events.
 * 2. The pinned area box [content, divider] bound to "any pinned row shown": hidden = no space in its parent,
 *    shown = content + divider height.
 * 3. The divider colour object (egG.beI(name, SZB)) gives back exactly the mixed colour.
 * 4. Every concrete arranger column (subclass of aGj) is one the mod handles (0.1.0-dev.1 missed the timeline
 *    and launcher columns, so the clips of pinned tracks still scrolled).
 * 5. Outside its builder, bCf reads the effect track box (Anf, missing in pinned lists) only in the two drop
 *    methods the mod guards (0.1.0-dev.2 crashed Bitwig there when dragging over the pinned area).
 * 6. The columns build top-level track lists outside kwH() only where the mod handles it (aGa.Gef -> bDO;
 *    0.1.0-dev.2 missed it: empty slot rows of pinned tracks in the lower clip launcher grid).
 *
 * Run: sh tools/probe-pinned.sh   (prints PIN_PROBE_OK or fails)
 */
public final class PinProbe {
    private static int failures = 0;

    /** Stand-in for a track's comment value (Bitwig's is the document attribute behind sJU.GKF()). */
    static final class Comment extends uLF implements uB1 {
        private Object value;

        Comment(String v) {
            value = v;
        }

        public Object getValue() {
            return value;
        }

        public boolean isSettable() {
            return true;
        }

        public void setValue(Object v) {
            Object old = value;
            value = v;
            fireValueChanged(this, v, old);
        }

        public boolean isObservable() {
            return true;
        }
    }

    /** Counts change events of a value (like a bound widget property would get them). */
    static final class Events implements NVD {
        int count;

        public void eventOccurred(Object e) {
            count++;
        }
    }

    public static void main(String[] args) throws Exception {
        rules();
        layout();
        colour();
        columns(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null);
        if (failures > 0) {
            System.out.println("PIN_PROBE_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("PIN_PROBE_OK");
    }

    private static boolean yes(uB1 v) {
        return (Boolean) v.getValue();
    }

    private static void rules() {
        Function<Object, Object> pinned = c -> bitwigmods_PinRule_isPinned(c instanceof String s ? s : null);
        Comment comment = new Comment("lead");
        Comment shownByBitwig = new Comment(null); // Bitwig's rule stand-in: "active OR show deactivated"
        m32 bitwigRule = new m32(new ccS()); // OR of one AND-of-nothing (= yes)
        KNG isPinned = new KNG(comment, pinned);
        ccS mainRow = new ccS(bitwigRule, new sFY(isPinned));
        ccS pinnedRow = new ccS(bitwigRule, isPinned);
        m32 anyShown = new m32();
        Events areaEvents = new Events();
        Events mainEvents = new Events();
        anyShown.addListener(areaEvents);
        mainRow.addListener(mainEvents);

        check("no pinned rows yet: area hidden", !yes(anyShown));
        anyShown.beI((uB1) pinnedRow); // typed uB1: an lIU argument would pick lIU.beI(lIU)
        check("unpinned: main row shown", yes(mainRow));
        check("unpinned: pinned row hidden", !yes(pinnedRow));
        check("unpinned: area hidden", !yes(anyShown));

        comment.setValue("lead #pin");
        check("pinned: main row hidden", !yes(mainRow));
        check("pinned: pinned row shown", yes(pinnedRow));
        check("pinned: area shown", yes(anyShown));
        check("pinned: main row got a change event", mainEvents.count > 0);
        check("pinned: area got a change event", areaEvents.count > 0);

        int before = areaEvents.count;
        comment.setValue("lead #pinned");
        check("#pinned: area hidden again", !yes(anyShown));
        check("#pinned: main row shown again", yes(mainRow));
        check("unpinning fires an event", areaEvents.count > before);

        comment.setValue("#pin");
        anyShown.Gef((uB1) pinnedRow); // row removed (track deleted)
        check("row removed: area hidden", !yes(anyShown));

        // Bitwig's own rule still counts: a hidden deactivated track stays hidden in both lists.
        m32 hiddenByBitwig = new m32(); // OR of nothing = no
        ccS pinnedButDeactivated = new ccS(hiddenByBitwig, new KNG(new Comment("#pin"), pinned));
        check("Bitwig's rule still applies", !yes(pinnedButDeactivated));
        check("stand-in unused value", shownByBitwig.getValue() == null);

        // Emergency switch of the scrolling list: "NOT pinned OR showAll"; showAll gets a "yes" if the pinned area failed.
        Comment c2 = new Comment("#pin");
        m32 showAll = new m32();
        ccS mainRow2 = new ccS(bitwigRule, new m32(new sFY(new KNG(c2, pinned)), showAll));
        Events e2 = new Events();
        mainRow2.addListener(e2);
        check("switch off: pinned track hidden in the scrolling list", !yes(mainRow2));
        showAll.beI((uB1) new ccS());
        check("switch on: pinned track shown in the scrolling list", yes(mainRow2));
        check("switch on fires an event", e2.count > 0);
    }

    // Same regex as PinRule (the probe is compiled without the mod's classes).
    private static boolean bitwigmods_PinRule_isPinned(String c) {
        return c != null && java.util.regex.Pattern.compile("(?i)#pin(?![\\p{L}\\p{N}_])").matcher(c).find();
    }

    private static void layout() throws Exception {
        // content: a frame with padding around a 40 px spacer box (a box of only spacers is 0 wide on its own)
        dXL inner = dXL.sSO();
        inner.sCA(40L);
        doV content = doV.beI(inner, 1, 1, 1, 1);
        dXL area = dXL.sSO();
        area.IvP(content);
        area.sCA(2L); // divider
        area.beI((ekS) egG.beI("probe divider", new SZB(0.1f, 0.1f, 0.1f, 1f)));
        long contentH = height(content);
        check("content has a height", contentH > 0);
        same("area = content + divider", height(area), contentH + 2);

        dXL parent = dXL.unz(1); // like aGj's box5: top to bottom, 1 px spacing
        dXL other = dXL.sSO();
        other.sCA(30L);
        doV otherFramed = doV.beI(other, 1, 1, 1, 1);
        parent.IvP(area);
        parent.IvP(otherFramed);
        long both = height(parent);

        m32 anyShown = new m32();
        doI.beI(area.Afd(), anyShown); // as the hook binds the area
        check("bound to 'no pinned row': area invisible", !area.KrW());
        same("hidden area takes no space", height(parent), height(otherFramed));

        Comment comment = new Comment("#pin");
        anyShown.beI((uB1) new KNG(comment, c -> bitwigmods_PinRule_isPinned((String) c)));
        check("a pinned row: area visible", area.KrW());
        same("shown area is back", height(parent), both);
        comment.setValue("");
        check("unpinned live: area invisible again", !area.KrW());
    }

    private static void colour() {
        SZB mixed = new SZB(0.15f, 0.16f, 0.17f, 1f);
        egG c = egG.beI("probe divider colour", mixed);
        float step = 1f / 255f; // Bitwig stores colours with 8 bits per channel
        SZB back = c.PJ3();
        check("fixed colour keeps its value", back != null && Math.abs(back.MgQ() - 0.15f) <= step
                && Math.abs(back.BBg() - 0.16f) <= step && Math.abs(back.unz() - 0.17f) <= step && Math.abs(back.Anf() - 1f) <= step);
        SZB painted = c.beI((dwF) null); // what a widget paints with (no widget needed for a fixed colour)
        check("painted colour = fixed colour", painted != null && Math.abs(painted.MgQ() - back.MgQ()) < 1e-6);
    }

    /** The classes PinnedTracksHooks.isColumn() checks with instanceof, read from the BUILT mod class. */
    private static Set<String> handledColumns(String hooksClass) throws Exception {
        Set<String> handled = new TreeSet<>();
        ClassModel m = ClassFile.of().parse(Files.readAllBytes(Path.of(hooksClass)));
        for (MethodModel mm : m.methods()) {
            if (mm.methodName().stringValue().equals("isColumn") && mm.code().isPresent()) {
                for (CodeElement ce : mm.code().get()) {
                    if (ce instanceof TypeCheckInstruction t) {
                        handled.add(t.type().asInternalName());
                    }
                }
            }
        }
        return handled;
    }

    private static void columns(String jar, String hooksClass) throws Exception {
        if (jar == null || hooksClass == null) {
            failures++;
            System.out.println("FAIL: columns: paths to bitwig.jar and the built PinnedTracksHooks.class missing");
            return;
        }
        Set<String> HANDLED = handledColumns(hooksClass);
        check("mod's column list read", !HANDLED.isEmpty());
        Map<String, String> superOf = new HashMap<>();
        Map<String, ClassModel> models = new HashMap<>();
        try (ZipFile z = new ZipFile(jar)) {
            for (var e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                try {
                    ClassModel m = ClassFile.of().parse(z.getInputStream(e).readAllBytes());
                    superOf.put(m.thisClass().asInternalName(), m.superclass().map(c -> c.asInternalName()).orElse(""));
                    String n = m.thisClass().asInternalName();
                    if (n.equals("bCf") || n.startsWith("aG")) {
                        models.put(n, m);
                    }
                } catch (Exception ignored) {
                    // not a class we can read: can't be a column
                }
            }
        }
        Set<String> concrete = new TreeSet<>();
        for (String c : superOf.keySet()) {
            for (String k = superOf.get(c); k != null && !k.isEmpty(); k = superOf.get(k)) {
                if (k.equals("aGj")) {
                    if (!Modifier.isAbstract(Class.forName(c.replace('/', '.'), false, PinProbe.class.getClassLoader()).getModifiers())) {
                        concrete.add(c);
                    }
                    break;
                }
            }
        }
        check("arranger columns found", !concrete.isEmpty());
        for (String c : concrete) {
            check("arranger column " + c + " is handled by the mod", HANDLED.contains(c));
        }
        for (String c : HANDLED) {
            check("handled column " + c + " exists in this Bitwig", concrete.contains(c));
        }
        System.out.println("arranger columns: " + concrete + ", handled by the mod: " + HANDLED);

        // 5. reads of bCf.Anf outside the builder
        Set<String> anfReaders = new TreeSet<>();
        for (MethodModel mm : models.get("bCf").methods()) {
            String sig = mm.methodName().stringValue() + mm.methodType().stringValue();
            if (sig.equals("Gef(Lcom/bitwig/flt/document/core/iface/uVN;LaZD;)V") || mm.code().isEmpty()) {
                continue; // the builder (creates it, only when HOV())
            }
            for (CodeElement ce : mm.code().get()) {
                if (ce instanceof FieldInstruction f && f.owner().asInternalName().equals("bCf") && f.name().stringValue().equals("Anf")) {
                    anfReaders.add(sig);
                }
            }
        }
        Set<String> guarded = Set.of(
                "beI(LdKT;)Lcom/bitwig/flt/document/core/iface/clipboard/track/TrackInsertionPoint;",
                "beI(DDLdKT;)Lcom/bitwig/flt/document/core/iface/clipboard/track/TrackInsertionPoint;");
        check("bCf reads the effect box only in the guarded drop methods " + anfReaders, guarded.containsAll(anfReaders));
        System.out.println("bCf.Anf read in: " + anfReaders);
        // the mod replaces IDq() (number of effect tracks) at 3 + 1 call sites in those methods: no more, no less
        Map<String, Integer> idq = new HashMap<>();
        for (MethodModel mm : models.get("bCf").methods()) {
            String sig = mm.methodName().stringValue() + mm.methodType().stringValue();
            if (!guarded.contains(sig) || mm.code().isEmpty()) {
                continue;
            }
            int n = 0;
            for (CodeElement ce : mm.code().get()) {
                if (ce instanceof InvokeInstruction ii && ii.name().stringValue().equals("IDq")
                        && ii.owner().asInternalName().equals("com/bitwig/flt/document/core/iface/uVN")) {
                    n++;
                }
            }
            idq.put(sig.startsWith("beI(DD") ? "inner" : "outer", n);
        }
        check("IDq() call sites in the drop methods are 3 + 1 (as hooked) " + idq,
                Integer.valueOf(3).equals(idq.get("outer")) && Integer.valueOf(1).equals(idq.get("inner")));

        // 6. top-level list factories called by the columns outside kwH()
        Set<String> outside = new TreeSet<>();
        for (String col : concrete) {
            for (String k = col; k != null && !k.equals("aGj") && models.containsKey(k); k = superOf.get(k)) {
                for (MethodModel mm : models.get(k).methods()) {
                    String name = mm.methodName().stringValue();
                    if (name.equals("kwH") || mm.code().isEmpty()) {
                        continue;
                    }
                    for (CodeElement ce : mm.code().get()) {
                        if (ce instanceof InvokeInstruction ii && ii.opcode() == java.lang.classfile.Opcode.INVOKESTATIC) {
                            String ret = ii.typeSymbol().returnType().descriptorString();
                            if (ret.startsWith("L") && extendsClass(superOf, ret.substring(1, ret.length() - 1), "bCf")) {
                                outside.add(k + "." + name + " -> " + ii.owner().asInternalName());
                            }
                        }
                    }
                }
            }
        }
        // aGa.Gef -> bDO: handled (slot-grid-main). aGa.beI -> bDY: the Global row of the launcher column
        // (bDY.ocs() = false: no tracks, VsA() = true: only the Global row), shows no tracks, nothing to filter.
        Set<String> handledOutside = Set.of("aGa.Gef -> bDO", "aGa.beI -> bDY");
        check("track lists built outside kwH() are all handled " + outside, handledOutside.containsAll(outside));
        System.out.println("lists built outside kwH(): " + outside);
    }

    private static boolean extendsClass(Map<String, String> superOf, String c, String base) {
        for (String k = c; k != null && !k.isEmpty(); k = superOf.get(k)) {
            if (k.equals(base)) {
                return true;
            }
        }
        return false;
    }

    private static long height(dwF widget) {
        String s = widget.jzF().toString(); // preferred size, LongSize[width=..,height=..]
        return Long.parseLong(s.substring(s.indexOf("height=") + 7, s.indexOf(']')));
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void same(String name, long actual, long expected) {
        if (actual != expected) {
            failures++;
            System.out.println("FAIL: " + name + ": expected " + expected + " got " + actual);
        }
    }
}
