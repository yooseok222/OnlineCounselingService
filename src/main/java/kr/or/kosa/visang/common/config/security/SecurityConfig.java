package kr.or.kosa.visang.common.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

/**
 * Spring Security 설정 클래스
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    /**
     * 비밀번호 인코더 빈 설정
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DaoAuthenticationProvider 빈 설정
     * CustomUserDetailsService와 PasswordEncoder를 명시적으로 적용하여
     * 비밀번호 검증 오류를 방지한다.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 인증 실패 핸들러
     * 이메일 인증이 완료되지 않은 사용자에게 적절한 메시지 표시
     * @return AuthenticationFailureHandler
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    /**
     * 인증 성공 핸들러
     * 사용자 유형에 따라 다른 페이지로 리다이렉트
     * @return AuthenticationSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setUseReferer(true);
        
        return (request, response, authentication) -> {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String redirectUrl;
            
            switch (userDetails.getRole()) {
                case "USER":
                    redirectUrl = "/user/dashboard";
                    break;
                case "AGENT":
                    redirectUrl = "/agent/dashboard";
                    break;
                case "ADMIN":
                    redirectUrl = "/admin/dashboard";
                    break;
                default:
                    redirectUrl = "/";
                    break;
            }
            
            response.sendRedirect(redirectUrl);
        };
    }

    /**
     * Security 필터 체인 설정
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 정적 리소스 접근 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                // H2 콘솔 접근 허용
                .requestMatchers("/h2-console/**").permitAll()
                // 공개 페이지 접근 허용
                .requestMatchers("/", "/login", "/register/**", "/verify/**").permitAll()
                // API 엔드포인트 접근 허용
                .requestMatchers("/api/email/check", "/api/phone/check", "/api/ssn/check", "/api/invitation/verify", "/api/verify/resend").permitAll()
                // 사용자 페이지 접근 권한 설정
                .requestMatchers("/user/**").hasRole("USER")
                // 상담원 페이지 접근 권한 설정
                .requestMatchers("/agent/**").hasRole("AGENT")
                // 관리자 페이지 접근 권한 설정
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login/process")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired=true")
            )
            .csrf(csrf -> csrf
                // H2 콘솔 및 API는 CSRF 비활성화
                .ignoringRequestMatchers("/h2-console/**", "/api/**")
            )
            // H2 콘솔 사용을 위한 설정
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }
} 