package io.teaql.core.parser;

import java.util.ArrayList;
import java.util.List;

import io.teaql.core.utils.StrBuilder;
import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.StrUtil;

public class Parser {

    public static String[] split(String input, char... separators) {
        List<String> result = new ArrayList<>();
        int length = input.length();
        StrBuilder sb = StrUtil.strBuilder();
        boolean preIsEscape = false;
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (ArrayUtil.contains(separators, c)) {
                if (preIsEscape) {
                    sb.append(c);
                    preIsEscape = false;
                }
                else {
                    if (!sb.isEmpty()) {
                        result.add(sb.toString());
                        sb.clear();
                    }
                }
            }
            else if (c == '\\') {
                if (preIsEscape) {
                    sb.append(c);
                    preIsEscape = false;
                }
                else {
                    preIsEscape = true;
                }
            }
            else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            result.add(sb.toString());
        }
        return result.toArray(new String[0]);
    }

    public static StringPair splitToPair(String input, char... separators) {
        int length = input.length();
        StrBuilder sb = StrUtil.strBuilder();
        boolean preIsEscape = false;
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (ArrayUtil.contains(separators, c)) {
                if (preIsEscape) {
                    sb.append(c);
                    preIsEscape = false;
                }
                else {
                    return new StringPair(sb.toString(), StrUtil.subSuf(input, i + 1));
                }
            }
            else if (c == '\\') {
                if (preIsEscape) {
                    sb.append(c);
                    preIsEscape = false;
                }
                else {
                    preIsEscape = true;
                }
            }
            else {
                sb.append(c);
            }
        }
        return new StringPair(sb.toString(), "");
    }

    public record StringPair(String pre, String post) {
    }
}
