package kr.or.kosa.visang.domain.user.service.impl;

import kr.or.kosa.visang.domain.admin.model.Admin;
import kr.or.kosa.visang.domain.admin.repository.AdminMapper;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.company.model.Company;
import kr.or.kosa.visang.domain.company.repository.CompanyMapper;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;
import kr.or.kosa.visang.domain.company.service.InvitationService;
import kr.or.kosa.visang.domain.user.dto.UserRegistrationRequest;
import kr.or.kosa.visang.domain.user.dto.UserResponse;
import kr.or.kosa.visang.domain.user.repository.UserMapper;
import kr.or.kosa.visang.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ClientMapper clientMapper;
    private final AgentMapper agentMapper;
    private final AdminMapper adminMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final InvitationService invitationService;
    
    // 이메일 인증 코드 저장을 위한 Redis 키 프리픽스
    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    // 이메일 인증 코드 유효 시간 (분)
    private static final long EMAIL_VERIFICATION_EXPIRY = 30;
    // 이메일 재발송 제한을 위한 Redis 키 프리픽스
    private static final String EMAIL_RESEND_LIMIT_PREFIX = "email:resend:limit:";
    // 이메일 재발송 제한 시간 (분)
    private static final long EMAIL_RESEND_LIMIT_EXPIRY = 5;
    // 이메일 재발송 최대 횟수 (n분 내)
    private static final int EMAIL_RESEND_MAX_COUNT = 3;

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        // 이메일 중복 확인
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        request.setEmail(normalizedEmail);
        
        if (isEmailDuplicated(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }
        
        // 전화번호 중복 확인 (전화번호가 제공된 경우)
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if (isPhoneNumberDuplicated(request.getPhoneNumber())) {
                throw new IllegalArgumentException("이미 사용 중인 전화번호입니다: " + request.getPhoneNumber());
            }
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        // 사용자 정보 공통 설정
        LocalDateTime now = LocalDateTime.now();
        
        UserResponse response;
        
        // 사용자 유형에 따라 처리
        switch (request.getUserType()) {
            case "USER":
                // 고객인 경우에만 주민등록번호 검증
                if (request.getSsn() == null || request.getSsn().isEmpty()) {
                    throw new IllegalArgumentException("주민등록번호는 필수 입력 항목입니다.");
                }
                
                // 주민번호 중복 확인
                if (isSsnDuplicated(request.getSsn())) {
                    throw new IllegalArgumentException("이미 사용 중인 주민등록번호입니다: " + request.getSsn());
                }
                
                // 고객 저장
                Client client = Client.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .phoneNumber(request.getPhoneNumber())
                        .address(request.getAddress())
                        .role("USER")
                        .ssn(request.getSsn())
                        .createdAt(now)
                        .emailVerified(false)
                        .build()
                        .validate();
                
                clientMapper.save(client);
                
                response = UserResponse.builder()
                        .userId(client.getClientId())
                        .name(client.getName())
                        .email(client.getEmail())
                        .phoneNumber(client.getPhoneNumber())
                        .address(client.getAddress())
                        .role(client.getRole())
                        .createdAt(client.getCreatedAt())
                        .build();
                break;
                
            case "ADMIN":
                // 회사 저장
                Company company = Company.builder()
                        .companyName(request.getCompanyName())
                        .createdAt(now)
                        .build()
                        .validate();
                
                companyMapper.save(company);
                
                // 관리자 저장
                Admin admin = Admin.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .phoneNumber(request.getPhoneNumber())
                        .address(request.getAddress())
                        .role("ADMIN")
                        .companyId(company.getCompanyId())
                        .createdAt(now)
                        .emailVerified(false)
                        .build()
                        .validate();
                
                adminMapper.save(admin);
                
                response = UserResponse.builder()
                        .userId(admin.getAdminId())
                        .name(admin.getName())
                        .email(admin.getEmail())
                        .phoneNumber(admin.getPhoneNumber())
                        .address(admin.getAddress())
                        .role(admin.getRole())
                        .createdAt(admin.getCreatedAt())
                        .companyId(company.getCompanyId())
                        .companyName(company.getCompanyName())
                        .build();
                break;
                
            case "AGENT":
                // 초대코드 필수 체크
                if (request.getInvitationCode() == null || request.getInvitationCode().isEmpty()) {
                    throw new IllegalArgumentException("초대코드는 필수 입력 항목입니다.");
                }
                
                // 초대코드 검증
                InvitationVerifyResponse verifyResponse = invitationService.verifyInvitation(request.getInvitationCode());
                if (!verifyResponse.isValid()) {
                    throw new IllegalArgumentException(verifyResponse.getErrorMessage());
                }
                
                // 회사 정보 설정
                Long companyId = verifyResponse.getCompanyId();
                request.setCompanyId(companyId);
                
                // 회사 정보 조회
                Company agentCompany = companyMapper.findById(companyId);
                if (agentCompany == null) {
                    throw new IllegalArgumentException("유효하지 않은 회사 정보입니다.");
                }
                
                // 상담원 저장
                Agent agent = Agent.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .phoneNumber(request.getPhoneNumber())
                        .address(request.getAddress())
                        .role("AGENT")
                        .companyId(companyId)
                        .state("INACTIVE") // 초기 상태
                        .createdAt(now)
                        .emailVerified(false)
                        .build()
                        .validate();
                
                agentMapper.save(agent);
                
                response = UserResponse.builder()
                        .userId(agent.getAgentId())
                        .name(agent.getName())
                        .email(agent.getEmail())
                        .phoneNumber(agent.getPhoneNumber())
                        .address(agent.getAddress())
                        .role(agent.getRole())
                        .createdAt(agent.getCreatedAt())
                        .companyId(agent.getCompanyId())
                        .companyName(agentCompany.getCompanyName())
                        .state(agent.getState())
                        .build();
                break;
                
            default:
                throw new IllegalArgumentException("유효하지 않은 사용자 유형입니다: " + request.getUserType());
        }
        
        // 이메일 인증 요청 자동 처리
        try {
            requestEmailVerification(request.getEmail());
            log.info("회원가입 완료 및 이메일 인증 요청 발송: {}", request.getEmail());
        } catch (Exception e) {
            log.error("이메일 인증 요청 발송 실패: {}", request.getEmail(), e);
            // 이메일 인증 요청 실패는 회원가입을 롤백하지 않음 (사용자가 나중에 재요청 가능)
        }
        
        return response;
    }

    @Override
    public boolean isEmailDuplicated(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        email = email.trim().toLowerCase();

        log.info("이메일 중복 체크 시작 - 이메일: {}", email);
        // 각 테이블에서 이메일 중복 체크
        boolean clientExists = clientMapper.isEmailExists(email) > 0;
        boolean adminExists = adminMapper.isEmailExists(email) > 0;
        boolean agentExists = agentMapper.isEmailExists(email) > 0;
        log.info("이메일 중복 체크 결과 - Client: {}, Admin: {}, Agent: {}", clientExists, adminExists, agentExists);
        boolean isDuplicated = clientExists || adminExists || agentExists;
        log.info("이메일 중복 체크 최종 결과 - 이메일: {}, 중복 여부: {}", email, isDuplicated);
        return isDuplicated;
    }

    @Override
    public boolean isPhoneNumberDuplicated(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        phoneNumber = phoneNumber.trim();
        boolean clientExists = clientMapper.isPhoneNumberExists(phoneNumber) > 0;
        boolean adminExists = adminMapper.isPhoneNumberExists(phoneNumber) > 0;
        boolean agentExists = agentMapper.isPhoneNumberExists(phoneNumber) > 0;
        return clientExists || adminExists || agentExists;
    }

    @Override
    public boolean isSsnDuplicated(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return false;
        }
        ssn = ssn.trim();
        return clientMapper.isSsnExists(ssn) > 0;
    }

    @Override
    public boolean requestEmailVerification(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        email = email.trim().toLowerCase();

        try {
            // 1. 이미 인증된 이메일인지 확인
            String userType = userMapper.findUserTypeByEmail(email);
            if (userType == null) {
                log.error("사용자를 찾을 수 없습니다: {}", email);
                throw new IllegalArgumentException("존재하지 않는 이메일입니다: " + email);
            }
            
            boolean isVerified = false;
            switch (userType) {
                case "CLIENT":
                    Client client = clientMapper.findByEmail(email);
                    isVerified = client != null && client.isEmailVerified();
                    break;
                case "ADMIN":
                    Admin admin = adminMapper.findByEmail(email);
                    isVerified = admin != null && admin.isEmailVerified();
                    break;
                case "AGENT":
                    Agent agent = agentMapper.findByEmail(email);
                    isVerified = agent != null && agent.isEmailVerified();
                    break;
            }
            
            if (isVerified) {
                log.info("이미 인증된 이메일입니다: {}", email);
                throw new IllegalArgumentException("이미 인증이 완료된 이메일입니다.");
            }
            
            // 2. 재발송 제한 확인
            String resendLimitKey = EMAIL_RESEND_LIMIT_PREFIX + email;
            String resendCountStr = redisTemplate.opsForValue().get(resendLimitKey);
            int resendCount = 0;
            
            if (resendCountStr != null) {
                resendCount = Integer.parseInt(resendCountStr);
                if (resendCount >= EMAIL_RESEND_MAX_COUNT) {
                    log.warn("이메일 인증 코드 재발송 제한 초과: {}, 현재 횟수: {}", email, resendCount);
                    throw new IllegalArgumentException(EMAIL_RESEND_LIMIT_EXPIRY + "분 내에 최대 " + EMAIL_RESEND_MAX_COUNT + "회까지만 인증 메일을 재발송할 수 있습니다. 잠시 후 다시 시도해주세요.");
                }
            }
            
            // 3. 인증 코드 생성 및 발송
            String verificationCode = UUID.randomUUID().toString().substring(0, 8);
            
            // Redis에 인증 코드 저장 (30분 유효)
            String key = EMAIL_VERIFICATION_PREFIX + email;
            redisTemplate.opsForValue().set(key, verificationCode, EMAIL_VERIFICATION_EXPIRY, TimeUnit.MINUTES);
            
            // 서버의 기본 URL 가져오기
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            
            // 이메일 주소에 '+' 등이 포함될 경우를 대비해 URL 인코딩 처리
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);

            // 자동 인증 URL 생성 (이메일 파라미터 인코딩 적용)
            String autoVerificationUrl = baseUrl + "/verify/auto?email=" + encodedEmail + "&code=" + verificationCode;

            // 수동 인증 페이지 URL 생성 (이메일 파라미터 인코딩 적용)
            String manualVerificationUrl = baseUrl + "/verify?email=" + encodedEmail;
            
            // Thymeleaf 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("autoVerificationUrl", autoVerificationUrl);
            context.setVariable("manualVerificationUrl", manualVerificationUrl);
            
            // 이메일 템플릿 처리
            String htmlContent = templateEngine.process("email/verification", context);
            
            // MimeMessage 생성 및 설정
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("[비상(Visang)] 이메일 인증 코드");
            helper.setText(htmlContent, true); // HTML 형식 사용
            
            mailSender.send(message);
            
            // 4. 재발송 카운트 증가
            resendCount++;
            redisTemplate.opsForValue().set(resendLimitKey, String.valueOf(resendCount), EMAIL_RESEND_LIMIT_EXPIRY, TimeUnit.MINUTES);
            
            log.info("이메일 인증 코드 발송: {}, 재발송 횟수: {}/{}", email, resendCount, EMAIL_RESEND_MAX_COUNT);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("이메일 인증 코드 발송 실패(인자 오류): {}", email, e);
            throw e; // 상위 호출자에게 예외 그대로 전달
        } catch (Exception e) {
            log.error("이메일 인증 코드 발송 실패: {}", email, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean verifyEmail(String email, String verificationCode) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        email = email.trim().toLowerCase();

        try {
            // 1. 이미 인증된 이메일인지 확인
            String userType = userMapper.findUserTypeByEmail(email);
            if (userType == null) {
                log.error("사용자를 찾을 수 없습니다: {}", email);
                return false;
            }
            
            boolean isVerified = false;
            switch (userType) {
                case "CLIENT":
                    Client client = clientMapper.findByEmail(email);
                    isVerified = client != null && client.isEmailVerified();
                    break;
                case "ADMIN":
                    Admin admin = adminMapper.findByEmail(email);
                    isVerified = admin != null && admin.isEmailVerified();
                    break;
                case "AGENT":
                    Agent agent = agentMapper.findByEmail(email);
                    isVerified = agent != null && agent.isEmailVerified();
                    break;
                default:
                    log.error("알 수 없는 사용자 유형: {}", userType);
                    return false;
            }
            
            if (isVerified) {
                log.info("이미 인증된 이메일입니다: {}", email);
                return true; // 이미 인증됨
            }
            
            // 2. 인증 코드 검증
            String key = EMAIL_VERIFICATION_PREFIX + email;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.error("인증 코드가 만료되었거나 존재하지 않습니다: {}", email);
                return false;
            }
            
            if (!storedCode.equals(verificationCode)) {
                log.error("인증 코드가 일치하지 않습니다: {}", email);
                return false;
            }
            
            // 3. 인증 성공, Redis에서 인증 코드 삭제
            redisTemplate.delete(key);
            
            // 4. 사용자 유형에 따라 인증 상태 업데이트
            switch (userType) {
                case "CLIENT":
                    clientMapper.updateEmailVerificationStatus(email, true);
                    break;
                case "ADMIN":
                    adminMapper.updateEmailVerificationStatus(email, true);
                    break;
                case "AGENT":
                    // 상담원인 경우 이메일 인증 성공 시 상태도 ACTIVE로 업데이트
                    Agent agent = agentMapper.findByEmail(email);
                    if (agent != null) {
                        agentMapper.updateEmailVerificationStatus(email, true);
                        agentMapper.updateAgentStatus(agent.getAgentId(), "ACTIVE");
                        log.info("상담원 이메일 인증 및 상태 ACTIVE로 업데이트: {}", email);
                    } else {
                        log.error("상담원 정보를 찾을 수 없습니다: {}", email);
                        return false;
                    }
                    break;
                default:
                    log.error("알 수 없는 사용자 유형: {}", userType);
                    return false;
            }
            
            log.info("이메일 인증 성공: {}", email);
            return true;
        } catch (Exception e) {
            log.error("이메일 인증 처리 중 오류 발생: {}", email, e);
            return false;
        }
    }
} 