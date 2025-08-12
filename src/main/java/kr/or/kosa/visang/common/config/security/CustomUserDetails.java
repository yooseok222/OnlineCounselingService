package kr.or.kosa.visang.common.config.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final Long userId;
    private final String name;
    private final String role;
    private final Long clientId;
    private final Long agentId;
    private final Long companyId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
                            Long userId, String name, String role,
                            Long clientId, Long agentId, Long companyId) {
        super(username, password, authorities);
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.clientId = clientId;
        this.agentId = agentId;
        this.companyId = companyId;
    }

} 