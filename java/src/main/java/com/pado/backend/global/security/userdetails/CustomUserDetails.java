package com.pado.backend.global.security.userdetails;

import com.pado.backend.domain.Role;
import com.pado.backend.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails 인터페이스 구현체
 * User 엔티티를 Spring Security가 이해할 수 있는 형태로 래핑
 */
public class CustomUserDetails implements UserDetails {
    
    private final User user;
    
    public CustomUserDetails(User user) {
        this.user = user;
    }
    
    /**
     * 사용자의 권한 목록을 반환
     * @return 권한 컬렉션
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Role enum을 Spring Security GrantedAuthority로 변환
        return Collections.singletonList(
            new SimpleGrantedAuthority(user.getRole().getAuthority())
        );
    }
    
    /**
     * 사용자의 비밀번호 반환
     * @return 암호화된 비밀번호
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    /**
     * 사용자의 username 반환 (여기서는 email 사용)
     * @return 이메일
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    /**
     * 계정이 만료되지 않았는지 여부
     * @return true (만료되지 않음)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    /**
     * 계정이 잠기지 않았는지 여부
     * @return true (잠기지 않음)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    /**
     * 자격증명(비밀번호)이 만료되지 않았는지 여부
     * @return true (만료되지 않음)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    /**
     * 계정이 활성화되어 있는지 여부
     * @return true (활성화됨)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * 원본 User 엔티티를 반환
     * @return User 엔티티
     */
    public User getUser() {
        return user;
    }
    
    /**
     * 사용자 ID 반환
     * @return 사용자 ID
     */
    public Long getUserId() {
        return user.getUserId();
    }
    
    /**
     * 사용자 이름 반환
     * @return 사용자 이름
     */
    public String getName() {
        return user.getUserName();
    }
    
    /**
     * 사용자 역할 반환
     * @return Role enum
     */
    public Role getRole() {
        return user.getRole();
    }
    
    /**
     * 이메일 반환
     * @return 이메일
     */
    public String getEmail() {
        return user.getEmail();
    }
}
