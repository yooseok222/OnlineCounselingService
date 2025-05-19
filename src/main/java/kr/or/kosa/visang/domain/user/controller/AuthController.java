package kr.or.kosa.visang.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.or.kosa.visang.common.validation.ConditionalValidation;
import kr.or.kosa.visang.domain.company.dto.InvitationVerifyResponse;
import kr.or.kosa.visang.domain.company.service.InvitationService;
import kr.or.kosa.visang.domain.user.dto.ApiResponse;
import kr.or.kosa.visang.domain.user.dto.UserRegistrationRequest;
import kr.or.kosa.visang.domain.user.dto.UserResponse;
import kr.or.kosa.visang.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final InvitationService invitationService;
    private final SmartValidator validator;

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, @RequestParam(required = false) String email, Model model) {
        if (error != null) {
            model.addAttribute("error", "로그인 정보가 올바르지 않습니다.");
        }
        if (email != null && !email.isEmpty()) {
            model.addAttribute("email", email);
        }
        return "auth/login";
    }

    /**
     * 회원가입 페이지
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userRegistrationRequest", new UserRegistrationRequest());
        return "auth/register";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("userRegistrationRequest") UserRegistrationRequest request,
                                      Model model, RedirectAttributes redirectAttributes) {
        try {
            // 회원가입 처리
            UserResponse userResponse = userService.register(request);
            
            // 이메일 인증 요청
            userService.requestEmailVerification(request.getEmail());
            
            // 성공 메시지 설정
            redirectAttributes.addFlashAttribute("email", request.getEmail());
            
            return "redirect:/register/success";
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
    
    /**
     * 회원가입 성공 페이지
     */
    @GetMapping("/register/success")
    public String registerSuccess(Model model) {
        return "auth/register-success";
    }
    
    /**
     * 이메일 중복 확인 API
     */
    @GetMapping("/api/email/check")
    @ResponseBody
    public ApiResponse checkEmail(@RequestParam String email) {
        log.info("이메일 중복 확인 API 호출 - 이메일: {}", email);
        
        // 실제 중복 체크 전에 로그 추가
        boolean isDuplicated = userService.isEmailDuplicated(email);
        log.info("이메일 중복 확인 결과 - 이메일: {}, 중복여부: {}", email, isDuplicated);
        
        ApiResponse response = new ApiResponse();
        response.setDuplicated(isDuplicated);
        
        if (isDuplicated) {
            response.setMessage("이미 사용 중인 이메일입니다.");
        } else {
            response.setMessage("사용 가능한 이메일입니다.");
        }
        
        log.info("이메일 중복 확인 API 응답 - 이메일: {}, 응답: {}", email, response);
        return response;
    }
    
    /**
     * 전화번호 중복 확인 API
     */
    @GetMapping("/api/phone/check")
    @ResponseBody
    public ApiResponse checkPhoneNumber(@RequestParam String phoneNumber) {
        log.info("전화번호 중복 확인 API 호출 - 전화번호: {}", phoneNumber);
        
        // 전화번호가 빈 문자열이면 중복 아님
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.info("전화번호 중복 확인 API - 빈 값이므로 false 반환");
            ApiResponse response = new ApiResponse();
            response.setDuplicated(false);
            response.setMessage("전화번호를 입력해주세요.");
            return response;
        }
        
        // 전화번호 정규화 로깅 추가
        String normalizedPhoneNumber = phoneNumber.replaceAll("-", "");
        log.info("전화번호 중복 확인 API - 정규화된 값: {}", normalizedPhoneNumber);
        
        // 중복 체크 동작 세부 확인을 위한 로그
        log.info("전화번호 중복 체크 전 - 원본: {}, 정규화: {}", phoneNumber, normalizedPhoneNumber);
        boolean isDuplicated = userService.isPhoneNumberDuplicated(phoneNumber);
        log.info("전화번호 중복 체크 후 - 원본: {}, 중복 여부: {}", phoneNumber, isDuplicated);
        
        ApiResponse response = new ApiResponse();
        response.setDuplicated(isDuplicated);
        
        if (isDuplicated) {
            response.setMessage("이미 사용 중인 전화번호입니다.");
        } else {
            response.setMessage("사용 가능한 전화번호입니다.");
        }
        
        log.info("전화번호 중복 확인 API 응답 - 전화번호: {}, 응답: {}", phoneNumber, response);
        return response;
    }
    
    /**
     * 주민번호 중복 확인 API
     */
    @GetMapping("/api/ssn/check")
    @ResponseBody
    public ApiResponse checkSsn(@RequestParam String ssn) {
        log.info("주민번호 중복 확인 API 호출 - 주민번호: {}", ssn);
        
        // 주민번호가 빈 문자열이면 중복 아님
        if (ssn == null || ssn.trim().isEmpty()) {
            log.info("주민번호 중복 확인 API - 빈 값이므로 false 반환");
            ApiResponse response = new ApiResponse();
            response.setDuplicated(false);
            response.setMessage("주민번호를 입력해주세요.");
            return response;
        }
        
        // 주민번호 정규화 로깅 추가
        String normalizedSsn = ssn.replaceAll("-", "");
        log.info("주민번호 중복 확인 API - 정규화된 값: {}", normalizedSsn);
        
        // 중복 체크 동작 세부 확인을 위한 로그
        log.info("주민번호 중복 체크 전 - 원본: {}, 정규화: {}", ssn, normalizedSsn);
        boolean isDuplicated = userService.isSsnDuplicated(ssn);
        log.info("주민번호 중복 체크 후 - 원본: {}, 중복 여부: {}", ssn, isDuplicated);
        
        ApiResponse response = new ApiResponse();
        response.setDuplicated(isDuplicated);
        
        if (isDuplicated) {
            response.setMessage("이미 사용 중인 주민번호입니다.");
        } else {
            response.setMessage("사용 가능한 주민번호입니다.");
        }
        
        log.info("주민번호 중복 확인 API 응답 - 주민번호: {}, 응답: {}", ssn, response);
        return response;
    }
    
    /**
     * 초대코드 검증 API
     */
    @PostMapping("/api/invitation/verify")
    @ResponseBody
    public InvitationVerifyResponse verifyInvitation(@RequestParam String code) {
        return invitationService.verifyInvitation(code);
    }
    
    /**
     * 이메일 인증 페이지 (수동 인증)
     */
    @GetMapping("/verify")
    public String verifyEmailPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "auth/verify";
    }
    
    /**
     * 이메일 인증 처리 (수동 인증)
     */
    @PostMapping("/verify")
    public String processVerification(@RequestParam String email, 
                                     @RequestParam String code, 
                                     Model model) {
        try {
            boolean isVerified = userService.verifyEmail(email, code);
            
            if (isVerified) {
                return "auth/verify-success";
            } else {
                model.addAttribute("email", email);
                model.addAttribute("error", "인증 코드가 유효하지 않거나 만료되었습니다.");
                return "auth/verify";
            }
        } catch (Exception e) {
            log.error("이메일 인증 처리 중 오류: {}", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "auth/verify";
        }
    }
    
    /**
     * 자동 이메일 인증 처리
     */
    @GetMapping("/verify/auto")
    public String autoVerifyEmail(@RequestParam String email, @RequestParam String code, Model model) {
        try {
            boolean isVerified = userService.verifyEmail(email, code);
            
            if (isVerified) {
                return "auth/verify-success";
            } else {
                model.addAttribute("error", "인증 코드가 유효하지 않거나 만료되었습니다.");
                return "auth/verify-error";
            }
        } catch (Exception e) {
            log.error("자동 이메일 인증 처리 중 오류: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/verify-error";
        }
    }
    
    /**
     * 인증 메일 재발송
     */
    @PostMapping("/verify/resend")
    public String resendVerificationEmail(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            boolean sent = userService.requestEmailVerification(email);
            if (sent) {
                redirectAttributes.addFlashAttribute("message", "인증 메일이 재발송되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("error", "인증 메일 발송에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("인증 메일 재발송 중 오류: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/verify?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }
    
    /**
     * AJAX를 통한 인증 메일 재발송 API
     */
    @PostMapping("/api/verify/resend")
    @ResponseBody
    public Map<String, Object> resendVerificationEmailApi(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean sent = userService.requestEmailVerification(email);
            if (sent) {
                response.put("success", true);
                response.put("message", "인증 메일이 재발송되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "인증 메일 발송에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("인증 메일 재발송 중 오류: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
} 