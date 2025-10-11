package sizz.api.comment.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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

    @Transactional
    public CommentResponse addComment(String userId, String articleId, CommentRequest request) {
        CommentEntity comment = CommentEntity.builder()
                .articleId(articleId)
                .userId(userId)
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

    @Transactional
    public CommentResponse patchComment(String email, String articleId, Long commentId, CommentRequest request) {

        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글이 존재하지 않습니다."));

        if(!email.equals(comment.getUserId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }

        if (!comment.getArticleId().equals(articleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 뉴스 댓글이 아닙니다.");
        }

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }

        CommentEntity updated = commentRepository.save(comment);
        return CommentResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteComment(String email, String articleId, Long commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글이 존재하지 않습니다."));

        if(!email.equals(comment.getUserId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        if (!comment.getArticleId().equals(articleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 뉴스 댓글이 아닙니다.");
        }

        commentRepository.deleteById(commentId);
    }
}
