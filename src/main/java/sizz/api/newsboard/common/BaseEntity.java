package sizz.api.newsboard.common;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass  // 공통 매핑 정보 제공
@EntityListeners(AuditingEntityListener.class)      //JPA Auditing : 생성일, 생성자, 수정일, 수정자 를 자동으로 기록 가능
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)   //수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
