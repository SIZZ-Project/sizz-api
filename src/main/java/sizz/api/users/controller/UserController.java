package sizz.api.users.controller;

import sizz.api.users.dto.ApiResponse;
import sizz.api.users.dto.SignupRequest;
import sizz.api.users.entity.User;
import sizz.api.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("nickname", user.getNickname());
            userData.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(
                new ApiResponse(true, "회원가입이 완료되었습니다.", userData));
                
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
        }
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