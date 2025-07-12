package sizz.api.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {
    
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    @NotBlank(message = "이메일은 필수입니다")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    private String password;
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다")
    private String nickname;
    
    public SignupRequest() {}
    
    public SignupRequest(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}