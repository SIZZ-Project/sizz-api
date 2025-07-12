package sizz.api.Comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sizz.api.Comment.dto.CommentRequest;
import sizz.api.Comment.dto.CommentResponse;
import sizz.api.Comment.entity.CommentEntity;
import sizz.api.Comment.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentResponse addComment(Long articleId, CommentRequest request) {
        CommentEntity comment = CommentEntity.builder()
                .articleId(articleId)
                .writer(request.getWriter())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        CommentEntity saved = commentRepository.save(comment);
        return new CommentResponse(saved.getId(), saved.getWriter(), saved.getContent(), saved.getCreatedAt());
    }

    public List<CommentResponse> getComments(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId)
                .stream()
                .map(c -> new CommentResponse(c.getId(), c.getWriter(), c.getContent(), c.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
