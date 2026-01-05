import java.util.function.Function;

public class Profiler {
    static long globalTime;

    public static void analyse(Runnable action){
        long start = timestamp();
        action.run();
        long stop = timestamp();
        globalTime += (stop - start);
    }

    public static void init() {
        globalTime = 0;
    }

    public static void getGlobalTime() {
        double elapsed = (globalTime) / 1e9;
        String unit = "s";
        if (elapsed < 1.0) {
            elapsed *= 1000.0;
            unit = "ms";
        }
        System.out.println(String.format("%.4g%s elapsed", elapsed, unit));
    }

    /**
     * Si clock0 est >0, retourne une chaîne de caractères
     * représentant la différence de temps depuis clock0.
     * @param clock0 instant initial
     * @return expression du temps écoulé depuis clock0
     */
    public static String timestamp(long clock0) {
        String result = null;

        if (clock0 > 0) {
            double elapsed = (System.nanoTime() - clock0) / 1e9;
            String unit = "s";
            if (elapsed < 1.0) {
                elapsed *= 1000.0;
                unit = "ms";
            }
            result = String.format("%.4g%s elapsed", elapsed, unit);
        }
        return result;
    }

    /**
     * retourne l'heure courante en ns.
     * @return
     */
    public static long timestamp() {
        return System.nanoTime();
    }
}