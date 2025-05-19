package kr.or.kosa.visang.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Pattern(regexp = "^[가-힣]{2,10}$", message = "이름은 2~10자의 한글만 입력 가능합니다.")
    private String name;
    
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$", 
             message = "비밀번호는 8~20자의 영문 대소문자, 숫자, 특수문자(!@#$%^&*)를 포함해야 합니다.")
    private String password;
    
    @Pattern(regexp = "^(01[016789]-?\\d{3,4}-?\\d{4})?$", 
             message = "유효한 전화번호 형식이 아닙니다. (예: 010-1234-5678)")
    private String phoneNumber;
    
    private String address;
    
    @NotBlank(message = "사용자 유형은 필수 입력 항목입니다.")
    private String userType; // USER, ADMIN, AGENT
    
    // 고객(User) 특화 필드
    @Pattern(regexp = "^\\d{6}-?[1-4]\\d{6}$", 
             message = "유효한 주민등록번호 형식이 아닙니다. (예: 901234-1234567)")
    private String ssn;
    
    // 관리자(Admin) 특화 필드
    private String companyName;
    
    // 상담원(Agent) 특화 필드
    private Long companyId;
    private String invitationCode;
} 