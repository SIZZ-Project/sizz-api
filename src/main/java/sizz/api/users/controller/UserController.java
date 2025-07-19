package sizz.api.users.controller;

import sizz.api.users.dto.ApiResponse;
import sizz.api.users.dto.LoginRequest;
import sizz.api.users.dto.SignupRequest;
import sizz.api.users.entity.User;
import sizz.api.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest signupRequest,
                                            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "입력값을 확인해주세요.", errors));
        }
        
        try {
            User user = userService.createUser(signupRequest);
            String token = userService.login(user.getEmail(), signupRequest.getPassword()); 
        
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "createdAt", user.getCreatedAt(),
                "token", token
            ));
            
            return ResponseEntity.ok(
                new ApiResponse(true, "회원가입이 완료되었습니다.", responseData));
                
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        return ResponseEntity.ok(new ApiResponse(true, "로그인에 성공하였습니다.", token));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "인증이 필요합니다."));
        }

        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "사용자를 찾을 수 없습니다."));
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("nickname", user.getNickname());

        return ResponseEntity.ok(new ApiResponse(true, "현재 사용자 정보", userData));
    }
    
    @GetMapping("/check/email")
    public ResponseEntity<ApiResponse> checkEmailAvailability(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        
        if (exists) {
            return ResponseEntity.ok(new ApiResponse(false, "이미 사용 중인 이메일입니다."));
        } else {
            return ResponseEntity.ok(new ApiResponse(true, "사용 가능한 이메일입니다."));
        }
    }
    
    @GetMapping("/check/nickname")
    public ResponseEntity<ApiResponse> checkNicknameAvailability(@RequestParam String nickname) {
        boolean exists = userService.existsByNickname(nickname);
        
        if (exists) {
            return ResponseEntity.ok(new ApiResponse(false, "이미 사용 중인 닉네임입니다."));
        } else {
            return ResponseEntity.ok(new ApiResponse(true, "사용 가능한 닉네임입니다."));
        }
    }
}