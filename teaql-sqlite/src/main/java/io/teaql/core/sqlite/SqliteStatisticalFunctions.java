package io.teaql.core.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.Function;
import org.sqlite.core.Codes;

/** Connection-local, constant-space numeric statistics; never coerces Decimal TEXT. */
final class SqliteStatisticalFunctions {
    private static final double WORD = 4_294_967_296.0;

    private SqliteStatisticalFunctions() {}

    static void register(Connection connection) throws SQLException {
        Connection sqlite = connection instanceof org.sqlite.SQLiteConnection
                ? connection : connection.unwrap(org.sqlite.SQLiteConnection.class);
        Function.create(sqlite, "stddev", new Moments(false, true), 1);
        Function.create(sqlite, "stddev_samp", new Moments(false, true), 1);
        Function.create(sqlite, "stddev_pop", new Moments(true, true), 1);
        Function.create(sqlite, "var_samp", new Moments(false, false), 1);
        Function.create(sqlite, "var_pop", new Moments(true, false), 1);
        Function.create(sqlite, "bit_and", new Bits('&'), 1);
        Function.create(sqlite, "bit_or", new Bits('|'), 1);
        Function.create(sqlite, "bit_xor", new Bits('^'), 1);
    }

    // Xerial clones each aggregate prototype per group. Keep mutable state in
    // primitive fields, not in a shared object referenced by those clones.
    private static final class Moments extends Function.Aggregate {
        private final boolean population;
        private final boolean squareRoot;
        private long count;
        private boolean integerOrigin;
        private long originLong;
        private double originReal;
        private double mean;
        private double m2;

        Moments(boolean population, boolean squareRoot) {
            this.population = population;
            this.squareRoot = squareRoot;
        }

        @Override
        protected void xStep() throws SQLException {
            int type = value_type(0);
            if (type == Codes.SQLITE_NULL) return;
            if (type != Codes.SQLITE_INTEGER && type != Codes.SQLITE_FLOAT) {
                error("SQLite statistical aggregate requires finite INTEGER or REAL values");
                return;
            }
            boolean integer = type == Codes.SQLITE_INTEGER;
            long number = integer ? value_long(0) : 0;
            double real = integer ? 0 : value_double(0);
            if (!integer && !Double.isFinite(real)) {
                error("SQLite statistical aggregate requires finite INTEGER or REAL values");
                return;
            }
            if (count == 0) {
                integerOrigin = integer;
                originLong = number;
                originReal = real;
            }
            // Subtract before converting the whole long to double. Splitting
            // signed high/unsigned low words preserves adjacent i64 values,
            // including mixed INTEGER/REAL inputs at the representation edge.
            double centered;
            if (integer && integerOrigin) {
                centered = ((double) (number >> 32) - (originLong >> 32)) * WORD
                        + ((double) (number & 0xffff_ffffL) - (originLong & 0xffff_ffffL));
            } else if (integer) {
                centered = ((number >> 32) * WORD - originReal) + (number & 0xffff_ffffL);
            } else if (integerOrigin) {
                centered = (real - (originLong >> 32) * WORD) - (originLong & 0xffff_ffffL);
            } else {
                centered = real - originReal;
            }
            if (count == Long.MAX_VALUE) {
                error("SQLite statistical count overflow");
                return;
            }
            count++;
            double delta = centered - mean;
            double nextMean = mean + delta / count;
            double nextM2 = m2 + delta * (centered - nextMean);
            if (!Double.isFinite(centered) || !Double.isFinite(nextMean)
                    || !Double.isFinite(nextM2) || nextM2 < 0) {
                error("SQLite statistical aggregate exceeds finite numeric range");
                return;
            }
            mean = nextMean;
            m2 = nextM2;
        }

        @Override
        protected void xFinal() throws SQLException {
            if (count == 0 || (!population && count < 2)) {
                result();
            } else {
                double variance = m2 / (population ? count : count - 1);
                result(squareRoot ? Math.sqrt(variance) : variance);
            }
        }
    }

    private static final class Bits extends Function.Aggregate {
        private final char operation;
        private boolean seen;
        private long bits;

        Bits(char operation) { this.operation = operation; }

        @Override
        protected void xStep() throws SQLException {
            int type = value_type(0);
            if (type == Codes.SQLITE_NULL) return;
            if (type != Codes.SQLITE_INTEGER) {
                error("SQLite bitwise aggregate requires INTEGER values");
                return;
            }
            long next = value_long(0);
            if (!seen) bits = next;
            else bits = switch (operation) {
                case '&' -> bits & next;
                case '|' -> bits | next;
                default -> bits ^ next;
            };
            seen = true;
        }

        @Override
        protected void xFinal() throws SQLException {
            if (seen) result(bits);
            else result();
        }
    }
}
