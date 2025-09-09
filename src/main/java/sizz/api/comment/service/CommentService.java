package sizz.api.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sizz.api.comment.dto.CommentRequest;
import sizz.api.comment.dto.CommentResponse;
import sizz.api.comment.entity.CommentEntity;
import sizz.api.comment.repository.CommentRepository;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentResponse addComment(String articleId, CommentRequest request) {
        CommentEntity comment = CommentEntity.builder()
                .articleId(request.getArticleId())
                .userId(request.getUserId())
                .content(request.getContent())
                .build();

        CommentEntity saved = commentRepository.save(comment);
        return CommentResponse.fromEntity(saved);
    }

    public List<CommentResponse> getComments(String articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId)
                .stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }
    public CommentResponse patchComment(String articleId, Long commentId, CommentRequest request) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!comment.getArticleId().equals(articleId)) {
            throw new IllegalArgumentException("해당 뉴스 댓글이 아닙니다.");
        }

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }

        CommentEntity updated = commentRepository.save(comment);
        return CommentResponse.fromEntity(updated);
    }

    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("댓글이 존재하지 않습니다.");
        }
        commentRepository.deleteById(commentId);
    }
}
