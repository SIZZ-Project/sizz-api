package sizz.api.users.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  @Email
  @NotBlank
  private String email;

  @Column
  private String password;
  
  @Column(nullable = false)
  @NotBlank
  @Size(min = 2, max = 50)
  private String nickname;
  
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider = AuthProvider.LOCAL;
  
  @Column(name = "provider_id")
  private String providerId;

  public User() {}
  
  public User(String email, String password, String nickname) {
      this.email = email;
      this.password = password;
      this.nickname = nickname;
      this.provider = AuthProvider.LOCAL;
  }
  
  public User(String email, String nickname, AuthProvider provider, String providerId) {
      this.email = email;
      this.nickname = nickname;
      this.provider = provider;
      this.providerId = providerId;
  }
  
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  
  public AuthProvider getProvider() { return provider; }
  public void setProvider(AuthProvider provider) { this.provider = provider; }
  
  public String getProviderId() { return providerId; }
  public void setProviderId(String providerId) { this.providerId = providerId; }
  
  public enum AuthProvider {
      LOCAL, GOOGLE
  }
}
