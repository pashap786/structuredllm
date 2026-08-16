package com.structuredllm;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMParser {

    private static final Pattern MARKDOWN_JSON_PATTERN =
            Pattern.compile("```(?:json)?\\s*(\\{.*?\\}|\\[.*?\\])\\s*```", Pattern.DOTALL);

    private static final Pattern RAW_JSON_PATTERN =
            Pattern.compile("(\\{.*?\\}|\\[.*?\\])", Pattern.DOTALL);

    /**
     * Extracts raw JSON content out of common LLM markdown wrapper signatures.
     */
    public static String extractJson(String rawLlmResponse) {
        if (rawLlmResponse == null || rawLlmResponse.isBlank()) {
            throw new IllegalArgumentException("Response from LLM was empty or null.");
        }

        // Strategy 1: Check for standard ```json ... ``` codeblocks
        Matcher markdownMatcher = MARKDOWN_JSON_PATTERN.matcher(rawLlmResponse);
        if (markdownMatcher.find()) {
            return markdownMatcher.group(1).trim();
        }

        // Strategy 2: Extract first JSON object or array payload bracket pair
        Matcher rawMatcher = RAW_JSON_PATTERN.matcher(rawLlmResponse);
        if (rawMatcher.find()) {
            return rawMatcher.group(1).trim();
        }

        return rawLlmResponse.trim();
    }

    /**
     * Lightweight field presence validator for records.
     */
    public static <T> T validateRecord(T recordInstance) throws ValidationException {
        Objects.requireNonNull(recordInstance, "Parsed object instance cannot be null.");

        if (!recordInstance.getClass().isRecord()) {
            return recordInstance;
        }

        for (var component : recordInstance.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(recordInstance);
                if (value == null) {
                    throw new ValidationException(
                            "Validation failed: Field '" + component.getName() + "' in "
                                    + recordInstance.getClass().getSimpleName() + " is null."
                    );
                }
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException("Unable to inspect field " + component.getName(), e);
            }
        }
        return recordInstance;
    }
}
