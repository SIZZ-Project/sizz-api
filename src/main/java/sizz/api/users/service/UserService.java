package sizz.api.users.service;

import sizz.api.users.dto.SignupRequest;
import sizz.api.users.entity.User;
import sizz.api.users.repository.UserRepository;
import sizz.api.users.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }
    
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

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("비밀번호가 틀렸습니다.");
        }

        return jwtTokenProvider.createToken(user.getEmail());
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

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}