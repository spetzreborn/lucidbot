/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package api.tools.numbers;

import api.tools.text.RegexUtil;
import api.tools.text.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class CalculatorUtil {
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^0-9,\\./\\*\\+\\-\\(\\)\\^\\[\\]]");
    private static final Pattern NON_NUMBERS = Pattern.compile("(/|\\*|\\+|-|\\(|\\)|\\^|\\[|\\])");
    private static final String EOB = "(\\(|\\[)";
    private static final String ECB = "(\\)|\\])";
    private static final Pattern MISSING_OPERATOR_1 = Pattern.compile(ECB + "(\\d)");
    private static final Pattern MISSING_OPERATOR_2 = Pattern.compile(ECB + EOB);
    private static final Pattern MISSING_OPERATOR_3 = Pattern.compile("(\\d)" + EOB);
    private static final Map<String, Operator> OPERATOR_MAP = new HashMap<>();

    static {
        for (Operator operator : Operator.values()) {
            OPERATOR_MAP.put(operator.stringRep, operator);
        }
    }

    private CalculatorUtil() {
    }

    /**
     * Calculates the expression from the specified String
     *
     * @param in                   the expression to calculate
     * @param nullOnUnknownSymbols whether to return null if the expression contains unsupported characters. Those characters are simply
     *                             removed from the String otherwise
     * @return a Double containing the result of the calculation, or null in the case described above
     */
    @Nullable
    public static Double calc(@Nullable final String in, final boolean nullOnUnknownSymbols) {
        if (StringUtil.isNullOrEmpty(in) || nullOnUnknownSymbols && NOT_ALLOWED.matcher(in).find()) return null;

        try {
            String formatted = formatInfixExpression(in);
            List<String> rpn = infixToRPN(formatted);
            return eval(rpn);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatInfixExpression(final CharSequence expression) {
        String formatted = NOT_ALLOWED.matcher(expression).replaceAll("");
        formatted = MISSING_OPERATOR_1.matcher(formatted).replaceAll("$1*$2");
        formatted = MISSING_OPERATOR_2.matcher(formatted).replaceAll("$1*$2");
        formatted = MISSING_OPERATOR_3.matcher(formatted).replaceAll("$1*$2");
        formatted = NON_NUMBERS.matcher(formatted).replaceAll(" $1 ");
        formatted = RegexUtil.TAB_OR_MULTIPLE_SPACES_PATTERN.matcher(formatted).replaceAll(" ");
        return formatted.trim();
    }

    private static List<String> infixToRPN(String formattedInfixExpression) {
        List<String> out = new ArrayList<>();
        LinkedList<String> stack = new LinkedList<>();
        for (String token : StringUtil.splitOnSpace(formattedInfixExpression)) {
            if (isOperator(token)) {
                while (!stack.isEmpty() && isOperator(stack.peek())) {
                    if (isLeftAssociative(token) && comparePrecedence(token, stack.peek()) <= 0 ||
                            !isLeftAssociative(token) && comparePrecedence(token, stack.peek()) < 0) {
                        out.add(stack.pop());
                        continue;
                    }
                    break;
                }
                stack.push(token);
            } else if ("(".equals(token) || "[".equals(token)) {
                stack.push(token);
            } else if (")".equals(token) || "]".equals(token)) {
                while (!stack.isEmpty() && !"(".equals(stack.peek()) && !"[".equals(stack.peek())) {
                    out.add(stack.pop());
                }
                stack.pop();
            } else {
                out.add(token);
            }
        }
        while (!stack.isEmpty()) {
            out.add(stack.pop());
        }
        return out;
    }

    private static boolean isOperator(String token) {
        return OPERATOR_MAP.containsKey(token);
    }

    private static boolean isLeftAssociative(String token) {
        return OPERATOR_MAP.get(token).isLeftAssoc;
    }

    private static int comparePrecedence(String token1, String token2) {
        return Operator.comparePrecedence(OPERATOR_MAP.get(token1), OPERATOR_MAP.get(token2));
    }

    private static Double eval(List<String> tokens) {
        LinkedList<Double> stack = new LinkedList<>();
        for (String token : tokens) {
            if (isOperator(token)) {
                Double pop = stack.pop();
                stack.push(OPERATOR_MAP.get(token).eval(stack.pop(), pop));
            } else {
                stack.push(NumberUtil.parseDouble(token));
            }
        }
        return stack.pop();
    }

    /**
     * Formats the specified Double with the default format for the Bot.
     * The default format is currently (might change):
     * Locale: US
     * Uses grouping
     * Max 5 decimals
     *
     * @param d the Double to format
     * @return a Double formatted as a String
     */
    public static String formatResult(final Double d) {
        return formatResult(checkNotNull(d), 5);
    }

    /**
     * Formats the specified Double with the default format for the Bot.
     * The default format is currently (might change):
     * Locale: US
     * Uses grouping
     *
     * @param d           the Double to format
     * @param maxDecimals the max amount of decimals
     * @return a Double formatted as a String
     */
    public static String formatResult(final Double d, final int maxDecimals) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(maxDecimals);
        nf.setMinimumFractionDigits(0);
        return nf.format(checkNotNull(d));
    }

    private enum Operator {
        PLUS("+", 0, true) {
            @Override
            public double eval(final double left, final double right) {
                return left + right;
            }
        },
        MINUS("-", 0, true) {
            @Override
            public double eval(final double left, final double right) {
                return left - right;
            }
        },
        MULTIPLICATION("*", 1, true) {
            @Override
            public double eval(final double left, final double right) {
                return left * right;
            }
        },
        DIVISION("/", 1, true) {
            @Override
            public double eval(final double left, final double right) {
                return left / right;
            }
        },
        POWER("^", 2, false) {
            @Override
            public double eval(final double left, final double right) {
                return Math.pow(left, right);
            }
        };

        private final String stringRep;
        private final int precedence;
        private final boolean isLeftAssoc;

        Operator(final String stringRep, final int precedence, final boolean leftAssoc) {
            isLeftAssoc = leftAssoc;
            this.precedence = precedence;
            this.stringRep = stringRep;
        }

        public static int comparePrecedence(final Operator left, final Operator right) {
            return left.precedence - right.precedence;
        }

        public abstract double eval(final double left, final double right);
    }
}
