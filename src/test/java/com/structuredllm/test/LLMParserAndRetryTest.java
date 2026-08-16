package com.structuredllm.test;

import com.structuredllm.LLMParser;
import com.structuredllm.RetryStrategy;
import com.structuredllm.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LLMParserAndRetryTest {

    // Target record for testing structured output
    public record SentimentResult(String sentiment, double score) {}

    @Nested
    @DisplayName("LLMParser Tests")
    class ParserTests {

        @Test
        @DisplayName("Extracts JSON from markdown code blocks")
        void testMarkdownJsonExtraction() {
            String rawLlmResponse = """
        Here is the parsed sentiment:
        ```json
        {
          "sentiment": "POSITIVE",
          "score": 0.98
        }
        ```
        Hope this helps!
        """;

            String extracted = LLMParser.extractJson(rawLlmResponse);

            // Normalize newlines and spaces for testing content structural equality
            String normalizedActual = extracted.replaceAll("\\s+", "");
            String normalizedExpected = "{\"sentiment\":\"POSITIVE\",\"score\":0.98}";

            assertEquals(normalizedExpected, normalizedActual);
        }

        @Test
        @DisplayName("Extracts raw JSON object when no markdown fences are used")
        void testRawJsonObjectExtraction() {
            String rawLlmResponse = "The response is {\"sentiment\": \"NEUTRAL\", \"score\": 0.50} as requested.";

            String extracted = LLMParser.extractJson(rawLlmResponse);
            assertEquals("{\"sentiment\": \"NEUTRAL\", \"score\": 0.50}", extracted);
        }

        @Test
        @DisplayName("Validates record successfully when all fields are present")
        void testRecordValidationSuccess() {
            SentimentResult validRecord = new SentimentResult("POSITIVE", 0.95);
            assertDoesNotThrow(() -> LLMParser.validateRecord(validRecord));
        }

        @Test
        @DisplayName("Throws ValidationException when a record field is null")
        void testRecordValidationFailureOnNullField() {
            SentimentResult invalidRecord = new SentimentResult(null, 0.95);

            ValidationException exception = assertThrows(
                    ValidationException.class,
                    () -> LLMParser.validateRecord(invalidRecord)
            );

            assertTrue(exception.getMessage().contains("Field 'sentiment'"));
        }
    }

    @Nested
    @DisplayName("RetryStrategy Tests")
    class RetryStrategyTests {

        @Test
        @DisplayName("Succeeds on the first attempt without retrying")
        void testRetrySucceedsFirstTry() {
            @SuppressWarnings("unchecked")
            Function<String, String> mockLlmCall = mock(Function.class);

            when(mockLlmCall.apply(anyString()))
                    .thenReturn("```json\n{\"sentiment\": \"POSITIVE\", \"score\": 0.90}\n```");

            // Simple manual parser mapping
            Function<String, SentimentResult> mockParser = json -> new SentimentResult("POSITIVE", 0.90);

            RetryStrategy<SentimentResult> retryStrategy = new RetryStrategy<>(
                    3,
                    mockLlmCall,
                    mockParser,
                    result -> result.sentiment() != null && result.score() > 0
            );

            SentimentResult result = retryStrategy.execute("Analyze this text");

            assertNotNull(result);
            assertEquals("POSITIVE", result.sentiment());
            assertEquals(0.90, result.score());

            // Should only call LLM once
            verify(mockLlmCall, times(1)).apply(anyString());
        }

        @Test
        @DisplayName("Retries on failure and succeeds on subsequent attempt")
        void testRetrySucceedsOnSecondAttempt() {
            @SuppressWarnings("unchecked")
            Function<String, String> mockLlmCall = mock(Function.class);

            // Attempt 1 fails (bad output), Attempt 2 succeeds
            when(mockLlmCall.apply(anyString()))
                    .thenReturn("I cannot process this request properly.")
                    .thenReturn("{\"sentiment\": \"NEGATIVE\", \"score\": 0.10}");

            Function<String, SentimentResult> mockParser = json -> {
                if (!json.startsWith("{")) {
                    throw new IllegalArgumentException("Invalid JSON format");
                }
                return new SentimentResult("NEGATIVE", 0.10);
            };

            RetryStrategy<SentimentResult> retryStrategy = new RetryStrategy<>(
                    3,
                    mockLlmCall,
                    mockParser,
                    result -> result.sentiment() != null
            );

            SentimentResult result = retryStrategy.execute("Analyze this text");

            assertEquals("NEGATIVE", result.sentiment());
            assertEquals(0.10, result.score());

            // Verified 2 executions occurred
            verify(mockLlmCall, times(2)).apply(anyString());
        }

        @Test
        @DisplayName("Exhausts all retry attempts and throws RuntimeException")
        void testExhaustsRetriesAndFails() {
            @SuppressWarnings("unchecked")
            Function<String, String> mockLlmCall = mock(Function.class);

            // Always returns invalid payload
            when(mockLlmCall.apply(anyString())).thenReturn("Invalid output");

            Function<String, SentimentResult> mockParser = json -> {
                throw new IllegalArgumentException("Malformed JSON");
            };

            RetryStrategy<SentimentResult> retryStrategy = new RetryStrategy<>(
                    2,
                    mockLlmCall,
                    mockParser,
                    result -> true
            );

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> retryStrategy.execute("Analyze this text")
            );

            assertTrue(exception.getMessage().contains("Failed to get valid response from LLM after 2 attempts"));
            verify(mockLlmCall, times(2)).apply(anyString());
        }
    }
}