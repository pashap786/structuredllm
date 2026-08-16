package com.structuredllm;

import java.util.function.Function;
import java.util.function.Predicate;

public class RetryStrategy<T> {

    private final int maxAttempts;
    private final Function<String, String> llmCall;
    private final Function<String, T> parser;
    private final Predicate<T> validator;

    public RetryStrategy(
            int maxAttempts,
            Function<String, String> llmCall,
            Function<String, T> parser,
            Predicate<T> validator) {
        this.maxAttempts = maxAttempts;
        this.llmCall = llmCall;
        this.parser = parser;
        this.validator = validator;
    }

    public T execute(String prompt) {
        String currentPrompt = prompt;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String rawOutput = llmCall.apply(currentPrompt);
                String cleanJson = LLMParser.extractJson(rawOutput);
                T result = parser.apply(cleanJson);

                if (validator.test(result)) {
                    return result;
                } else {
                    throw new ValidationException("Validation check failed on attempt " + attempt);
                }
            } catch (Exception e) {
                lastException = e;
                // Feedback loop prompt modification for next attempt
                currentPrompt = prompt + "\n\n[SYSTEM NOTE: Your previous response failed with error: "
                        + e.getMessage() + ". Respond with pure, valid JSON only.]";
            }
        }

        throw new RuntimeException("Failed to get valid response from LLM after " + maxAttempts + " attempts.", lastException);
    }
}