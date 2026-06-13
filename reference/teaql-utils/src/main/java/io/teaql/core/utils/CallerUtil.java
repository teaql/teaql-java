package io.teaql.core.utils;

import java.util.List;
import java.util.stream.Collectors;

public class CallerUtil {

    public static java.lang.Class<?> getCaller() {
        return getCaller(0);
    }

    public static java.lang.Class<?> getCaller(int p0) {
        try {
            List<Class<?>> classes = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream
                    .map(StackWalker.StackFrame::getDeclaringClass)
                    .filter(clazz -> clazz != CallerUtil.class)
                    .collect(Collectors.toList())
                );
            if (p0 < 0 || p0 >= classes.size()) {
                return null;
            }
            return classes.get(p0);
        } catch (Exception e) {
            return null;
        }
    }

}
