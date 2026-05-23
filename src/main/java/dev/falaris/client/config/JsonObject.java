package dev.falaris.client.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class JsonObject {
    private final Map<String, Object> values;

    public JsonObject() {
        this.values = new LinkedHashMap<>();
    }

    private JsonObject(Map<String, Object> values) {
        this.values = values;
    }

    public static JsonObject parse(String json) {
        return new Parser(json).parseObject();
    }

    public static String stringify(Map<String, Object> value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        builder.append('\n');
        return builder.toString();
    }

    public Optional<JsonObject> object(String key) {
        Object value = values.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String stringKey) {
                    typed.put(stringKey, entry.getValue());
                }
            }

            return Optional.of(new JsonObject(typed));
        }

        return Optional.empty();
    }

    public Optional<Boolean> booleanValue(String key) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? Optional.of(bool) : Optional.empty();
    }

    public Optional<Integer> intValue(String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }

        return Optional.empty();
    }

    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value instanceof Map<?, ?> map) {
            writeObject(builder, map, indent);
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else {
            builder.append(value);
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent) {
        builder.append("{\n");
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            indent(builder, indent + 2);
            builder.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
            writeValue(builder, entry.getValue(), indent + 2);
            if (++index < map.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }

        indent(builder, indent);
        builder.append('}');
    }

    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(indent));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Parser {
        private final String input;
        private int cursor;

        private Parser(String input) {
            this.input = input;
        }

        private JsonObject parseObject() {
            skipWhitespace();
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();

            while (peek() != '}') {
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, parseValue());
                skipWhitespace();

                if (peek() == ',') {
                    cursor++;
                    skipWhitespace();
                }
            }

            expect('}');
            return new JsonObject(map);
        }

        private Object parseValue() {
            char next = peek();
            if (next == '{') {
                return parseObject().values;
            }
            if (next == '"') {
                return parseString();
            }
            if (input.startsWith("true", cursor)) {
                cursor += 4;
                return true;
            }
            if (input.startsWith("false", cursor)) {
                cursor += 5;
                return false;
            }

            return parseNumber();
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (peek() != '"') {
                char value = input.charAt(cursor++);
                if (value == '\\') {
                    value = input.charAt(cursor++);
                }
                builder.append(value);
            }
            expect('"');
            return builder.toString();
        }

        private Number parseNumber() {
            int start = cursor;
            while (cursor < input.length() && "-0123456789".indexOf(input.charAt(cursor)) >= 0) {
                cursor++;
            }
            return Integer.parseInt(input.substring(start, cursor));
        }

        private char peek() {
            if (cursor >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON.");
            }
            return input.charAt(cursor);
        }

        private void expect(char expected) {
            if (peek() != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at " + cursor + ".");
            }
            cursor++;
        }

        private void skipWhitespace() {
            while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
                cursor++;
            }
        }
    }
}
