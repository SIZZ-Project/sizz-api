package sizz.api.search.service;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.phrase_extractor.KoreanPhraseExtractor.KoreanPhrase;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer.KoreanToken;
import scala.collection.Seq;

import java.util.List;
import java.util.stream.Collectors;

public class KoreanTokenizer {

    public static List<String> extractNounPhrases(String text) {
        // 1) 정규화
        CharSequence normalized = OpenKoreanTextProcessorJava.normalize(text);

        // 2) 토큰화
        Seq<KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(normalized);

        // 3) 명사/명사구 추출
        List<KoreanPhrase> phrases = OpenKoreanTextProcessorJava.extractPhrases(tokens, true, true);

        // 4) 텍스트만 추출 + 정제
        return phrases.stream()
                .map(KoreanPhrase::text)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
