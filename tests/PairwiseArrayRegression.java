import java.io.BufferedWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import leb.process.ProcCalcPairwiseAAI;
import leb.util.seq.Blast6FormatHitDomain;

/** Run against the built EzAAI jar; no external sequence data are required. */
public class PairwiseArrayRegression {
    private static Blast6FormatHitDomain hit(String query, String target,
            double identity, int length) {
        Blast6FormatHitDomain h = new Blast6FormatHitDomain();
        h.setQuery(query);
        h.setTarget(target);
        h.setIdentity(identity);
        h.setAlignmentLength(length);
        h.setStartInQuery(1);
        h.setEndInQuery(91);
        return h;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void check(List<Blast6FormatHitDomain> vice,
            List<Blast6FormatHitDomain> versa, double threshold,
            String[] expected, String[] expectedPairs) throws Exception {
        ProcCalcPairwiseAAI calculator = new ProcCalcPairwiseAAI();
        calculator.setMode(ProcCalcPairwiseAAI.MODE_MMSEQS);
        calculator.setIdentity(threshold);
        for (String name : Arrays.asList("label1", "label2")) {
            Field field = ProcCalcPairwiseAAI.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(calculator, name);
        }
        Map<String, Integer> lengths = new HashMap<>(), a = new HashMap<>(), b = new HashMap<>();
        List<String> namesA = new ArrayList<>(), namesB = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String ai = "a" + i, bi = "b" + i;
            lengths.put(ai, 100); lengths.put(bi, 100);
            a.put(ai, i); b.put(bi, i);
            namesA.add(ai); namesB.add(bi);
        }
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        calculator.setMatchout(writer);
        Method calculate = ProcCalcPairwiseAAI.class.getDeclaredMethod("calcIdentityWithDetails",
                List.class, List.class, Map.class, Map.class, Map.class, List.class, List.class);
        calculate.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) calculate.invoke(calculator,
                vice, versa, lengths, a, b, namesB, namesA);
        writer.flush();
        require(result.equals(Arrays.asList(expected)), "unexpected result: " + result);
        String[] lines = output.toString().isEmpty() ? new String[0] : output.toString().split("\\n");
        require(lines.length == expectedPairs.length, "unexpected match count");
        for (int i = 0; i < lines.length; i++) {
            String[] fields = lines[i].split("\\t");
            require((fields[4] + ":" + fields[5]).equals(expectedPairs[i]),
                    "incorrect match or output order: " + lines[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        Blast6FormatHitDomain lowCoverage = hit("b2", "a1", .90, 30);
        lowCoverage.setEndInQuery(30);
        List<Blast6FormatHitDomain> vice = Arrays.asList(
                hit("b1", "a0", .90, 80), hit("b0", "a2", .80, 70),
                hit("b2", "a1", .39, 50), lowCoverage);
        List<Blast6FormatHitDomain> versa = Arrays.asList(
                hit("a2", "b0", .81, 68), hit("a0", "b1", .91, 82),
                hit("a1", "b2", .99, 90));
        check(vice, versa, .4, new String[]{"3", "3", "2", "3", "2", "75", "85.5"},
                new String[]{"b1:a0", "b0:a2"});
        check(new ArrayList<Blast6FormatHitDomain>(), new ArrayList<Blast6FormatHitDomain>(), .4,
                new String[]{"3", "3", "0", "0", "0", "NaN", "NaN"}, new String[]{});
        // A real zero-identity hit is distinct from a missing hit when the threshold is zero.
        check(Arrays.asList(hit("b1", "a0", 0, 80)), Arrays.asList(hit("a0", "b1", 0, 82)), 0,
                new String[]{"3", "3", "1", "1", "1", "81", "0.0"}, new String[]{"b1:a0"});
        // Multiple alignments to the same target retain the last passing record.
        List<Blast6FormatHitDomain> repeated = new ArrayList<>(vice);
        repeated.add(hit("b1", "a0", .92, 80));
        check(repeated, versa, .4, new String[]{"3", "3", "3", "3", "2", "75", "86.0"},
                new String[]{"b1:a0", "b0:a2"});
        System.out.println("Pairwise array regression checks passed.");
    }
}
