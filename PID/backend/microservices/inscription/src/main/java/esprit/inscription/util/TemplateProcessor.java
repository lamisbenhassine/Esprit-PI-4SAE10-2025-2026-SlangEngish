package esprit.inscription.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TemplateProcessor {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+(?:\\.\\w+)*)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+(.+?)\\}\\}(.+?)\\{\\{/if\\}\\}");
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+(\\w+)\\}\\}(.+?)\\{\\{/each\\}\\}");

    public String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.trim().isEmpty()) {
            return template;
        }

        String result = template;

        // Process conditionals first
        result = processConditionals(result, variables);

        // Process loops
        result = processLoops(result, variables);

        // Process simple variables
        result = processVariables(result, variables);

        return result;
    }

    private String processVariables(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variablePath = matcher.group(1);
            String value = getVariableValue(variablePath, variables);
            matcher.appendReplacement(result, value);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String processConditionals(String template, Map<String, Object> variables) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String condition = matcher.group(1);
            String content = matcher.group(2);

            if (evaluateCondition(condition, variables)) {
                matcher.appendReplacement(result, content);
            } else {
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String processLoops(String template, Map<String, Object> variables) {
        Matcher matcher = LOOP_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String arrayName = matcher.group(1);
            String content = matcher.group(2);

            Object arrayValue = getVariableValue(arrayName, variables);
            String loopResult = processLoop(content, arrayValue);
            matcher.appendReplacement(result, loopResult);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String getVariableValue(String path, Map<String, Object> variables) {
        try {
            String[] parts = path.split("\\.");
            Object current = variables;

            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return "";
                }

                if (current == null) {
                    return "";
                }
            }

            return current != null ? current.toString() : "";
        } catch (Exception e) {
            log.debug("Error getting variable value for path: {}", path, e);
            return "";
        }
    }

    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        try {
            // Simple condition evaluation
            // Format: variableName operator value
            String[] parts = condition.split("\\s+");
            if (parts.length < 3) {
                return false;
            }

            String variableName = parts[0];
            String operator = parts[1];
            String value = parts.length > 2 ? parts[2] : "";

            Object variableValue = getVariableValue(variableName, variables);
            if (variableValue == null) {
                return false;
            }

            switch (operator) {
                case "==":
                    return variableValue.toString().equals(value);
                case "!=":
                    return !variableValue.toString().equals(value);
                case ">":
                    return compareNumbers(variableValue, value) > 0;
                case "<":
                    return compareNumbers(variableValue, value) < 0;
                case ">=":
                    return compareNumbers(variableValue, value) >= 0;
                case "<=":
                    return compareNumbers(variableValue, value) <= 0;
                case "exists":
                    return variableValue != null && !variableValue.toString().isEmpty();
                case "empty":
                    return variableValue == null || variableValue.toString().isEmpty();
                default:
                    return false;
            }
        } catch (Exception e) {
            log.debug("Error evaluating condition: {}", condition, e);
            return false;
        }
    }

    private String processLoop(String content, Object arrayValue) {
        if (arrayValue == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        if (arrayValue instanceof Iterable) {
            int index = 0;
            for (Object item : (Iterable<?>) arrayValue) {
                String itemContent = content;
                
                // Replace {{this}} with current item
                itemContent = itemContent.replace("{{this}}", item != null ? item.toString() : "");
                
                // Replace {{@index}} with current index
                itemContent = itemContent.replace("{{@index}}", String.valueOf(index));
                
                result.append(itemContent);
                index++;
            }
        }

        return result.toString();
    }

    private int compareNumbers(Object a, Object b) {
        try {
            double numA = Double.parseDouble(a.toString());
            double numB = Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.toString().compareTo(b.toString());
        }
    }

    public String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }

    public String formatCurrency(Double amount, String currency) {
        if (amount == null) {
            return "0.00";
        }
        
        return String.format("%.2f %s", amount, currency != null ? currency : "EUR");
    }

    public String formatDate(java.time.LocalDateTime date, String pattern) {
        if (date == null) {
            return "";
        }
        
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
            return date.format(formatter);
        } catch (Exception e) {
            log.debug("Error formatting date: {}", date, e);
            return date.toString();
        }
    }
}
