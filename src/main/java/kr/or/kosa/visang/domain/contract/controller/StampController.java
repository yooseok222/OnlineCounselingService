package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.StampDTO;
import kr.or.kosa.visang.domain.contract.service.StampService;
import kr.or.kosa.visang.common.util.SecurityUtil;
import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/stamp")
public class StampController {

    @Autowired
    private StampService stampService;

    @Value("${file.upload-dir.stamp}")
    private String UPLOAD_DIR;

    /**
     * 도장 업로드 페이지 표시
     */
    @GetMapping("/upload")
    public String uploadPage(Model model, HttpSession session) {
        // 현재 로그인한 사용자 정보 가져오기
        CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null || currentUser.getClientId() == null) {
            log.error("로그인하지 않았거나 고객 정보가 없습니다.");
            return "redirect:/login";
        }
        
        Long clientId = currentUser.getClientId();
        log.info("도장 업로드 페이지 접근 - 클라이언트 ID: {}, 사용자명: {}", clientId, currentUser.getName());
        
        // 기존 도장 목록 조회
        List<StampDTO> stamps = stampService.getStampsByClientId(clientId);
        model.addAttribute("stamps", stamps);
        model.addAttribute("clientId", clientId);
        
        return "stamp/upload";
    }

    /**
     * 도장 파일 업로드 처리
     */
    @PostMapping("/upload")
    public String uploadStamp(@RequestParam("file") MultipartFile file,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "파일을 선택해주세요.");
                return "redirect:/stamp/upload";
            }

            // 이미지 파일인지 확인
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "이미지 파일만 업로드 가능합니다.");
                return "redirect:/stamp/upload";
            }

            // 현재 로그인한 사용자 정보 가져오기
            CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
            if (currentUser == null || currentUser.getClientId() == null) {
                log.error("로그인하지 않았거나 고객 정보가 없습니다.");
                redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            Long clientId = currentUser.getClientId();
            log.info("도장 업로드 시작 - 클라이언트 ID: {}, 사용자명: {}, 파일명: {}", 
                    clientId, currentUser.getName(), file.getOriginalFilename());

            // 도장 업로드 및 저장
            StampDTO stampDTO = stampService.uploadStamp(file, clientId);
            
            redirectAttributes.addFlashAttribute("success", "도장이 성공적으로 업로드되었습니다.");
            log.info("도장 업로드 완료 - 클라이언트 ID: {}, 도장 ID: {}, 파일: {}", 
                    clientId, stampDTO.getStampId(), stampDTO.getImagePath());

        } catch (IllegalStateException e) {
            log.warn("도장 업로드 거부 - 클라이언트 ID: {}, 사유: {}", 
                    SecurityUtil.getCurrentUser().getClientId(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            log.error("도장 업로드 실패", e);
            redirectAttributes.addFlashAttribute("error", "도장 업로드 중 오류가 발생했습니다.");
        }

        return "redirect:/stamp/upload";
    }

    /**
     * 도장 목록 페이지 표시
     */
    @GetMapping("/list")
    public String listPage(Model model, HttpSession session) {
        // 현재 로그인한 사용자 정보 가져오기
        CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null || currentUser.getClientId() == null) {
            log.error("로그인하지 않았거나 고객 정보가 없습니다.");
            return "redirect:/login";
        }
        
        Long clientId = currentUser.getClientId();
        log.info("도장 목록 페이지 접근 - 클라이언트 ID: {}, 사용자명: {}", clientId, currentUser.getName());
        
        List<StampDTO> stamps = stampService.getStampsByClientId(clientId);
        model.addAttribute("stamps", stamps);
        model.addAttribute("clientId", clientId);
        
        return "stamp/list";
    }

    /**
     * 현재 로그인한 사용자의 도장 목록 조회 (AJAX용)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<StampDTO>> getCurrentUserStampList() {
        try {
            // 현재 로그인한 사용자 정보 가져오기
            CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
            if (currentUser == null || currentUser.getClientId() == null) {
                log.error("로그인하지 않았거나 고객 정보가 없습니다.");
                return ResponseEntity.status(401).build();
            }

            Long clientId = currentUser.getClientId();
            log.info("도장 목록 API 호출 - 클라이언트 ID: {}, 사용자명: {}", clientId, currentUser.getName());
            
            List<StampDTO> stamps = stampService.getStampsByClientId(clientId);
            return ResponseEntity.ok(stamps);
        } catch (Exception e) {
            log.error("도장 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 클라이언트의 도장 목록 조회 (AJAX용) - 하위 호환성을 위해 유지
     */
    @GetMapping("/api/list/{clientId}")
    @ResponseBody
    public ResponseEntity<List<StampDTO>> getStampList(@PathVariable Long clientId) {
        try {
            // 현재 로그인한 사용자 정보 확인
            CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
            if (currentUser == null || currentUser.getClientId() == null) {
                log.error("로그인하지 않았거나 고객 정보가 없습니다.");
                return ResponseEntity.status(401).build();
            }

            // 요청한 clientId가 현재 로그인한 사용자의 ID와 일치하는지 확인
            if (!currentUser.getClientId().equals(clientId)) {
                log.error("권한 없음 - 요청 클라이언트 ID: {}, 현재 사용자 클라이언트 ID: {}", 
                         clientId, currentUser.getClientId());
                return ResponseEntity.status(403).build();
            }

            log.info("도장 목록 API 호출 - 클라이언트 ID: {}, 사용자명: {}", clientId, currentUser.getName());
            List<StampDTO> stamps = stampService.getStampsByClientId(clientId);
            return ResponseEntity.ok(stamps);
        } catch (Exception e) {
            log.error("도장 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 도장 삭제
     */
    @DeleteMapping("/api/{stampId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteStamp(@PathVariable Long stampId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 현재 로그인한 사용자 정보 확인
            CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
            if (currentUser == null || currentUser.getClientId() == null) {
                log.error("로그인하지 않았거나 고객 정보가 없습니다.");
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 삭제하려는 도장 정보 조회
            StampDTO stamp = stampService.getStampById(stampId);
            if (stamp == null) {
                response.put("success", false);
                response.put("message", "도장을 찾을 수 없습니다.");
                return ResponseEntity.ok(response);
            }

            // 도장 소유자 확인
            if (!currentUser.getClientId().equals(stamp.getClientId())) {
                log.error("권한 없음 - 도장 소유자: {}, 현재 사용자: {}", 
                         stamp.getClientId(), currentUser.getClientId());
                response.put("success", false);
                response.put("message", "해당 도장을 삭제할 권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            log.info("도장 삭제 시작 - 클라이언트 ID: {}, 도장 ID: {}", 
                    currentUser.getClientId(), stampId);

            int result = stampService.deleteStamp(stampId);
            if (result > 0) {
                response.put("success", true);
                response.put("message", "도장이 삭제되었습니다.");
                log.info("도장 삭제 완료 - 클라이언트 ID: {}, 도장 ID: {}", 
                        currentUser.getClientId(), stampId);
            } else {
                response.put("success", false);
                response.put("message", "도장 삭제에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("도장 삭제 실패", e);
            response.put("success", false);
            response.put("message", "도장 삭제 중 오류가 발생했습니다.");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 도장 이미지 파일 서빙
     */
    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getStampImage(@PathVariable String filename) {
        try {
            // 업로드 경로를 절대 경로로 변환
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .contentType(MediaType.IMAGE_PNG)
                        .body(resource);
            } else {
                log.warn("도장 이미지 파일을 찾을 수 없습니다: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("이미지 파일 서빙 실패: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 