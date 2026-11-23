package io.teaql.core.sqlite;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

/** PostgreSQL-compatible GBK bytes, without MS936 private-use extensions. */
final class SqliteGbkEncoding {
    private static final Charset GBK = Charset.forName("x-mswin-936");

    private SqliteGbkEncoding() {}

    static byte[] encode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == 0 || (character >= '\uE000' && character <= '\uF8FF')) {
                throw invalid();
            }
        }
        try {
            var buffer = GBK.newEncoder().onUnmappableCharacter(CodingErrorAction.REPORT)
                    .onMalformedInput(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        } catch (CharacterCodingException ignored) {
            // Neither input text nor codec diagnostics belong in SQL errors.
            throw invalid();
        }
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("GBK cannot encode text in the portable profile");
    }
}
