package kr.or.kosa.visang.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 응답 기본 클래스
 * 모든 API 응답의 표준 형식을 정의합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;     // 요청 성공 여부
    private String message;      // 응답 메시지
    private T data;              // 응답 데이터
    private boolean duplicated;  // 중복 여부 (중복 체크 API용)
    
    /**
     * 성공 응답 생성 (메시지만)
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 중복 체크 응답 생성
     */
    public static ApiResponse<Void> duplicateCheck(boolean isDuplicated, String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .duplicated(isDuplicated)
                .message(message)
                .build();
    }
} 