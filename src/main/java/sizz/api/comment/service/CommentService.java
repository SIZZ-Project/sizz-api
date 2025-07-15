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

    public CommentResponse addComment(Long articleId, CommentRequest request) {
        CommentEntity comment = CommentEntity.builder()
                .articleId(request.getArticleId())
                .writer(request.getWriter())
                .content(request.getContent())
                .build();

        CommentEntity saved = commentRepository.save(comment);
        return CommentResponse.fromEntity(saved);
    }

    public List<CommentResponse> getComments(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId)
                .stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("댓글이 존재하지 않습니다.");
        }
        commentRepository.deleteById(commentId);
    }
}
