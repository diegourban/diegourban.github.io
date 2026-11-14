package demo.booking;

import java.util.function.Consumer;

/**
 * Pause points for the deterministic test. The writers call {@link #at} at "beforeLock" and "beforeCommit"; with no hook
 * set on the thread (the hammer, or real code) the call does nothing.
 */
public final class Hooks {
    private static final ThreadLocal<Consumer<String>> HOOK = new ThreadLocal<>();

    private Hooks() {
    }

    static void set(Consumer<String> hook) {
        HOOK.set(hook);
    }

    static void clear() {
        HOOK.remove();
    }

    static void at(String point) {
        Consumer<String> hook = HOOK.get();
        if (hook != null) {
            hook.accept(point);
        }
    }
}
