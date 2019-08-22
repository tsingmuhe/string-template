package com.sunchangpeng.template;

import java.util.function.Function;

public class StringTemplateParser {
    public static final String EMPTY = "";

    public static final String DEFAULT_MACRO_PREFIX = "$";
    public static final String DEFAULT_MACRO_START = "${";
    public static final String DEFAULT_MACRO_END = "}";

    // ---------------------------------------------------------------- properties

    protected boolean replaceMissingKey = true;
    protected String missingKeyReplacement;
    protected boolean resolveEscapes = true;
    protected String macroPrefix = DEFAULT_MACRO_PREFIX;
    protected String macroStart = DEFAULT_MACRO_START;
    protected String macroEnd = DEFAULT_MACRO_END;
    protected char escapeChar = '\\';
    protected boolean parseValues;

    /**
     * Specifies if missing keys should be resolved at all,
     * <code>true</code> by default.
     * If <code>false</code> missing keys will be left as it were, i.e.
     * they will not be replaced.
     */
    public StringTemplateParser setReplaceMissingKey(final boolean replaceMissingKey) {
        this.replaceMissingKey = replaceMissingKey;
        return this;
    }

    /**
     * Specifies replacement for missing keys. If <code>null</code>
     * exception will be thrown.
     */
    public StringTemplateParser setMissingKeyReplacement(final String missingKeyReplacement) {
        this.missingKeyReplacement = missingKeyReplacement;
        return this;
    }

    /**
     * Specifies if escaped values should be resolved. In special usecases,
     * when the same string has to be processed more then once,
     * this may be set to <code>false</code> so escaped values
     * remains.
     */
    public StringTemplateParser setResolveEscapes(final boolean resolveEscapes) {
        this.resolveEscapes = resolveEscapes;
        return this;
    }

    /**
     * Defines macro start string.
     */
    public StringTemplateParser setMacroStart(final String macroStart) {
        this.macroStart = macroStart;
        return this;
    }

    public StringTemplateParser setMacroPrefix(final String macroPrefix) {
        this.macroPrefix = macroPrefix;
        return this;
    }


    /**
     * Defines macro end string.
     */
    public StringTemplateParser setMacroEnd(final String macroEnd) {
        this.macroEnd = macroEnd;
        return this;
    }

    /**
     * Sets the strict format by setting the macro prefix to <code>null</code>.
     */
    public StringTemplateParser setStrictFormat() {
        macroPrefix = null;
        return this;
    }

    /**
     * Defines escape character.
     */
    public StringTemplateParser setEscapeChar(final char escapeChar) {
        this.escapeChar = escapeChar;
        return this;
    }

    /**
     * Defines if macro values has to be parsed, too.
     * By default, macro values are returned as they are.
     */
    public StringTemplateParser setParseValues(final boolean parseValues) {
        this.parseValues = parseValues;
        return this;
    }


    // ---------------------------------------------------------------- parse

    /**
     * Parses string template and replaces macros with resolved values.
     */
    public String parse(String template, final Function<String, String> macroResolver) {
        StringBuilder result = new StringBuilder(template.length());

        int i = 0;
        int len = template.length();

        // strict flag means that start and end tag are not necessary
        boolean strict;

        if (macroPrefix == null) {
            // when prefix is not specified, make it equals to macro start
            // so we can use the same code
            macroPrefix = macroStart;

            strict = true;
        } else {
            strict = false;
        }

        final int prefixLen = macroPrefix.length();
        final int startLen = macroStart.length();
        final int endLen = macroEnd.length();

        while (i < len) {
            int ndx = template.indexOf(macroPrefix, i);
            if (ndx == -1) {
                result.append(i == 0 ? template : template.substring(i));
                break;
            }

            // check escaped
            int j = ndx - 1;
            boolean escape = false;
            int count = 0;

            while ((j >= 0) && (template.charAt(j) == escapeChar)) {
                escape = !escape;
                if (escape) {
                    count++;
                }
                j--;
            }
            if (resolveEscapes) {
                result.append(template.substring(i, ndx - count));
            } else {
                result.append(template.substring(i, ndx));
            }
            if (escape) {
                result.append(macroPrefix);

                i = ndx + prefixLen;

                continue;
            }

            // macro started, detect strict format

            boolean detectedStrictFormat = strict;

            if (!detectedStrictFormat) {
                if (isSubstringAt(template, macroStart, ndx)) {
                    detectedStrictFormat = true;
                }
            }

            int ndx1;
            int ndx2;

            if (!detectedStrictFormat) {
                // not strict format: $foo

                ndx += prefixLen;
                ndx1 = ndx;
                ndx2 = ndx;

                while ((ndx2 < len) && isPropertyNameChar(template.charAt(ndx2))) {
                    ndx2++;
                }

                if (ndx2 == len) {
                    ndx2--;
                }

                while ((ndx2 > ndx) && !isAlphaOrDigit(template.charAt(ndx2))) {
                    ndx2--;
                }

                ndx2++;

                if (ndx2 == ndx1 + 1) {
                    // no value, hence no macro
                    result.append(macroPrefix);

                    i = ndx1;
                    continue;
                }
            } else {
                // strict format: ${foo}

                // find macros end
                ndx += startLen;
                ndx2 = template.indexOf(macroEnd, ndx);
                if (ndx2 == -1) {
                    throw new IllegalArgumentException("Invalid template, unclosed macro at: " + (ndx - startLen));
                }

                // detect inner macros, there is no escaping
                ndx1 = ndx;
                while (ndx1 < ndx2) {
                    int n = indexOf(template, macroStart, ndx1, ndx2);
                    if (n == -1) {
                        break;
                    }
                    ndx1 = n + startLen;
                }
            }

            final String name = template.substring(ndx1, ndx2);

            // find value and append

            Object value;
            if (missingKeyReplacement != null || !replaceMissingKey) {
                try {
                    value = macroResolver.apply(name);
                } catch (Exception ignore) {
                    value = null;
                }

                if (value == null) {
                    if (replaceMissingKey) {
                        value = missingKeyReplacement;
                    } else {
                        if (detectedStrictFormat) {
                            value = template.substring(ndx1 - startLen, ndx2 + endLen);
                        } else {
                            value = template.substring(ndx1 - 1, ndx2);
                        }
                    }
                }
            } else {
                value = macroResolver.apply(name);
                if (value == null) {
                    value = EMPTY;
                }
            }

            if (ndx == ndx1) {
                String stringValue = value.toString();
                if (parseValues) {
                    if (stringValue.contains(macroStart)) {
                        stringValue = parse(stringValue, macroResolver);
                    }
                }
                result.append(stringValue);

                i = ndx2;
                if (detectedStrictFormat) {
                    i += endLen;
                }
            } else {
                // inner macro
                template = template.substring(0, ndx1 - startLen) + value.toString() + template.substring(ndx2 + endLen);
                len = template.length();
                i = ndx - startLen;
            }
        }
        return result.toString();
    }


    /**
     * Returns <code>true</code> if substring exist at given offset in a string.
     */
    public static boolean isSubstringAt(final String string, final String substring, final int offset) {
        int len = substring.length();

        int max = offset + len;

        if (max > string.length()) {
            return false;
        }

        int ndx = 0;
        for (int i = offset; i < max; i++, ndx++) {
            if (string.charAt(i) != substring.charAt(ndx)) {
                return false;
            }
        }

        return true;
    }

    public static int indexOf(final String src, final String sub, int startIndex, int endIndex) {
        if (startIndex < 0) {
            startIndex = 0;
        }
        int srclen = src.length();
        if (endIndex > srclen) {
            endIndex = srclen;
        }
        int sublen = sub.length();
        if (sublen == 0) {
            return startIndex > srclen ? srclen : startIndex;
        }

        int total = endIndex - sublen + 1;
        char c = sub.charAt(0);
        mainloop:
        for (int i = startIndex; i < total; i++) {
            if (src.charAt(i) != c) {
                continue;
            }
            int j = 1;
            int k = i + 1;
            while (j < sublen) {
                if (sub.charAt(j) != src.charAt(k)) {
                    continue mainloop;
                }
                j++;
                k++;
            }
            return i;
        }
        return -1;
    }

    public static boolean isPropertyNameChar(final char c) {
        return isDigit(c) || isAlpha(c) || (c == '_') || (c == '.') || (c == '[') || (c == ']');
    }

    public static boolean isAlphaOrDigit(final char c) {
        return isDigit(c) || isAlpha(c);
    }

    public static boolean isAlpha(final char c) {
        return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
    }

    public static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

}