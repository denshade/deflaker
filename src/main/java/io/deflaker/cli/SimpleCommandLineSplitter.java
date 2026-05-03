package io.deflaker.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a pasted shell-style command line into tokens. Double-quoted segments stay together;
 * quotes are removed from the resulting tokens.
 */
public final class SimpleCommandLineSplitter {

    private SimpleCommandLineSplitter() {}

    public static List<String> split(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (!inDoubleQuotes && Character.isWhitespace(c)) {
                flushToken(tokens, current);
                continue;
            }

            current.append(c);
        }

        flushToken(tokens, current);

        return Collections.unmodifiableList(tokens);
    }

    private static void flushToken(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }
}