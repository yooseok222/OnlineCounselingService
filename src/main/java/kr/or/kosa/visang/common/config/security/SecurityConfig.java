package kr.or.kosa.visang.common.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

/**
 * Spring Security 설정 클래스
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${http.port}")
    private int httpPort;

    @Value("${server.port}")
    private int httpsPort;

    private final CustomUserDetailsService userDetailsService;
    private final String REMEMBER_ME_KEY = "visangSecretKey";

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
     * RememberMe 서비스 빈 설정
     * 자동 로그인 기능을 위한 토큰 기반 서비스를 구성한다.
     * @return TokenBasedRememberMeServices
     */
    @Bean
    public TokenBasedRememberMeServices rememberMeServices() {
        TokenBasedRememberMeServices rememberMeServices =
            new TokenBasedRememberMeServices(REMEMBER_ME_KEY, userDetailsService);

        // 쿠키 설정
        rememberMeServices.setParameter("remember-me"); // 체크박스 이름
        rememberMeServices.setCookieName("remember-me"); // 쿠키 이름
        rememberMeServices.setTokenValiditySeconds(1209600); // 유효기간 2주 (60*60*24*14)

        return rememberMeServices;
    }

    /**
     * RememberMe 인증 제공자 빈 설정
     * 자동 로그인 인증을 처리한다.
     * @return RememberMeAuthenticationProvider
     */
    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider() {
        return new RememberMeAuthenticationProvider(REMEMBER_ME_KEY);
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
        http.requiresChannel(channel -> channel.anyRequest().requiresSecure())
                // 포트 매핑은 동일하게 지정
                .portMapper(mapper -> mapper
                        .http(httpPort).mapsTo(httpsPort)
                )
            .authorizeHttpRequests(authorize -> authorize
                // 정적 리소스 접근 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                // H2 콘솔 접근 허용
                .requestMatchers("/h2-console/**").permitAll()
                // 공개 페이지 접근 허용
                .requestMatchers("/", "/login", "/register/**", "/verify/**").permitAll()
                // API 엔드포인트 접근 허용
                .requestMatchers("/api/email/check", "/api/phone/check", "/api/ssn/check", "/api/invitation/verify", "/api/verify/resend").permitAll()
                    // 상담 관련 API 접근 허용 (인증된 사용자만)
                    .requestMatchers("/api/consultation/**").authenticated()
                    // WebSocket 연결 허용
                    .requestMatchers("/ws/**").permitAll()
                    // 계약 관련 페이지 접근 허용 (인증된 사용자만)
                    .requestMatchers("/contract/**").authenticated()
                    // 사용자 페이지 접근 권한 설정
                .requestMatchers("/user/**").hasRole("USER")
                // 상담원 페이지 접근 권한 설정
                .requestMatchers("/agent/**").hasRole("AGENT")
                // 관리자 페이지 접근 권한 설정
                .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/app/**").permitAll()          // 클라이언트 → 서버로 메시지 보낼 때
                    .requestMatchers("/topic/**").permitAll()        // 서버 → 클라이언트로 메시지 보낼 때
                    // 다운로드용 파일 접근 허용
                    .requestMatchers("/files/**").permitAll()
                    // 내보낸 채팅 이력 정보 조회 허용
                    .requestMatchers("/api/chat/export/**").permitAll()

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
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            // H2 콘솔 사용을 위한 설정
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }
} 