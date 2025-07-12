package sizz.api.users.service;

import sizz.api.users.dto.SignupRequest;
import sizz.api.users.entity.User;
import sizz.api.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional
    public User createUser(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        
        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }
        
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        
        User user = new User(
            signupRequest.getEmail(),
            encodedPassword,
            signupRequest.getNickname()
        );
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User processOAuthPostLogin(String email, String name, User.AuthProvider provider, String providerId) {
        var existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setProvider(provider);
            user.setProviderId(providerId);
            return userRepository.save(user);
        } else {
            User newUser = new User(email, name, provider, providerId);
            return userRepository.save(newUser);
        }
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}