package sizz.api.news.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;

    public static GeminiRequest of(String prompt, int maxOutputTokens) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part), "user");
        GenerationConfig config = new GenerationConfig(maxOutputTokens);
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
    public static class GenerationConfig {
        private Integer maxOutputTokens;
    }
}
