package sizz.api.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;

    /**
     * 범용 생성자: response_schema 없이 mimeType만 지정
     */
    public static GeminiRequest of(String prompt, int maxOutputTokens) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part), "user");
        GenerationConfig config = GenerationConfig.builder()
                .maxOutputTokens(maxOutputTokens)
                .temperature(0.4)
                .responseMimeType("application/json")
                .build();
        return new GeminiRequest(List.of(content), config);
    }

    /**
     * 키워드용: JSON 배열(문자열) 스키마 강제
     */
    public static GeminiRequest ofKeywords(String prompt, int maxOutputTokens) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part), "user");

        // response_schema: ARRAY<STRING>
        Map<String, Object> schema = Map.of(
                "type", "ARRAY",
                "items", Map.of("type", "STRING")
        );

        GenerationConfig config = GenerationConfig.builder()
                .maxOutputTokens(maxOutputTokens)
                .temperature(0.4)
                .responseMimeType("application/json")
                .responseSchema(schema)
                .build();

        return new GeminiRequest(List.of(content), config);
    }

    /**
     * 요약/성향용: { summary:string, inclination:string } 스키마 강제
     */
    public static GeminiRequest ofSummary(String prompt, int maxOutputTokens) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part), "user");

        // response_schema: OBJECT { summary, inclination }
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "STRING"),
                        "inclination", Map.of("type", "STRING")
                ),
                "required", List.of("summary", "inclination")
        );

        GenerationConfig config = GenerationConfig.builder()
                .maxOutputTokens(maxOutputTokens)
                .temperature(0.4)
                .responseMimeType("application/json")
                .responseSchema(schema)
                .build();

        return new GeminiRequest(List.of(content), config);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GenerationConfig {
        private Integer maxOutputTokens;
        private Double temperature;
        private String responseMimeType;
        private Map<String, Object> responseSchema;
    }
}
