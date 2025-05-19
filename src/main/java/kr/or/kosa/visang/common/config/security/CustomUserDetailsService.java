package kr.or.kosa.visang.common.config.security;

import kr.or.kosa.visang.domain.admin.model.Admin;
import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.user.model.User;
import kr.or.kosa.visang.domain.user.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import kr.or.kosa.visang.common.config.security.exception.EmailNotVerifiedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final ClientMapper clientMapper;
    private final AgentMapper agentMapper;
    private final AdminMapper adminMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일로 사용자 유형 확인
        String userType = userMapper.findUserTypeByEmail(email);
        
        if (userType == null) {
            log.error("사용자를 찾을 수 없습니다: {}", email);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
        }
        
        // 사용자 유형에 따라 적절한 매퍼에서 사용자 정보 조회
        switch (userType) {
            case "CLIENT":
                return loadClientDetails(email);
            case "ADMIN":
                return loadAdminDetails(email);
            case "AGENT":
                return loadAgentDetails(email);
            default:
                log.error("지원되지 않는 사용자 유형: {}", userType);
                throw new UsernameNotFoundException("지원되지 않는 사용자 유형: " + userType);
        }
    }
    
    /**
     * 고객 정보 조회 및 UserDetails 반환
     */
    private UserDetails loadClientDetails(String email) {
        Client client = clientMapper.findByEmail(email);
        
        if (client == null) {
            log.error("고객을 찾을 수 없습니다: {}", email);
            throw new UsernameNotFoundException("고객을 찾을 수 없습니다: " + email);
        }
        
        // 이메일 인증 확인
        if (!client.isEmailVerified()) {
            log.error("이메일 인증이 완료되지 않은 고객입니다: {}", email);
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않은 사용자입니다. 이메일을 확인해주세요.");
        }
        
        // 권한 설정
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + client.getRole()));
        
        return new CustomUserDetails(
                client.getEmail(),
                client.getPassword(),
                authorities,
                client.getClientId(),
                client.getName(),
                client.getRole(),
                client.getClientId(),
                null,
                null
        );
    }
    
    /**
     * 관리자 정보 조회 및 UserDetails 반환
     */
    private UserDetails loadAdminDetails(String email) {
        Admin admin = adminMapper.findByEmail(email);
        
        if (admin == null) {
            log.error("관리자를 찾을 수 없습니다: {}", email);
            throw new UsernameNotFoundException("관리자를 찾을 수 없습니다: " + email);
        }
        
        // 이메일 인증 확인
        if (!admin.isEmailVerified()) {
            log.error("이메일 인증이 완료되지 않은 관리자입니다: {}", email);
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않은 사용자입니다. 이메일을 확인해주세요.");
        }
        
        // 권한 설정
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + admin.getRole()));
        
        return new CustomUserDetails(
                admin.getEmail(),
                admin.getPassword(),
                authorities,
                admin.getAdminId(),
                admin.getName(),
                admin.getRole(),
                null,
                null,
                admin.getCompanyId()
        );
    }
    
    /**
     * 상담원 정보 조회 및 UserDetails 반환
     */
    private UserDetails loadAgentDetails(String email) {
        Agent agent = agentMapper.findByEmail(email);
        
        if (agent == null) {
            log.error("상담원을 찾을 수 없습니다: {}", email);
            throw new UsernameNotFoundException("상담원을 찾을 수 없습니다: " + email);
        }
        
        // 이메일 인증 확인
        if (!agent.isEmailVerified()) {
            log.error("이메일 인증이 완료되지 않은 상담원입니다: {}", email);
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않은 사용자입니다. 이메일을 확인해주세요.");
        }
        
        // 상담원 계정 활성화 상태 확인
        if ("INACTIVE".equals(agent.getState())) {
            log.error("비활성화된 상담원 계정입니다: {}", email);
            throw new UsernameNotFoundException("비활성화된 상담원 계정입니다. 관리자에게 문의하세요.");
        }
        
        // 상담원 계정 승인 대기 상태 확인
        if ("PENDING".equals(agent.getState())) {
            log.error("승인 대기 중인 상담원 계정입니다: {}", email);
            throw new UsernameNotFoundException("승인 대기 중인 상담원 계정입니다. 관리자 승인 후 이용 가능합니다.");
        }
        
        // 권한 설정
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + agent.getRole()));
        
        return new CustomUserDetails(
                agent.getEmail(),
                agent.getPassword(),
                authorities,
                agent.getAgentId(),
                agent.getName(),
                agent.getRole(),
                null,
                agent.getAgentId(),
                agent.getCompanyId()
        );
    }
} 