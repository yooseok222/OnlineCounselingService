package kr.or.kosa.visang.domain.client.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.common.util.SecurityUtil;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.service.ClientService;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.StampDTO;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contract.service.StampService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {
    
    private final ClientService clientService;
    private final ContractService contractService;
    private final StampService stampService;
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    /**
     * 클라이언트 대시보드
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long clientId = user.getClientId();
        
        // 오늘의 계약 조회
        List<Contract> todayContracts = contractService.getTodayContractsByClientId(clientId);
        
        // 전체 계약 상태별 카운트
        Map<String, Integer> contractCounts = contractService.getContractCountsByClientId(clientId);
        
        model.addAttribute("clientId", clientId);
        model.addAttribute("todayContracts", todayContracts);
        model.addAttribute("contractCounts", contractCounts);
        model.addAttribute("contentFragment", "client/clientDashboard");
        model.addAttribute("scriptFragment", "client/clientDashboard");
        model.addAttribute("sideFragment", "client/clientSidebar");
        
        return "layout/main";
    }
    
    /**
     * 오늘의 계약 페이지
     */
    @GetMapping("/contracts/today")
    public String todayContracts(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long clientId = user.getClientId();
        List<Contract> todayContracts = contractService.getTodayContractsByClientId(clientId);
        
        model.addAttribute("contracts", todayContracts);
        model.addAttribute("contentFragment", "client/clientTodayContracts");
        model.addAttribute("scriptFragment", "client/clientTodayContracts");
        model.addAttribute("sideFragment", "client/clientSidebar");
        
        return "layout/main";
    }
    
    /**
     * 나의 계약 현황 페이지
     */
    @GetMapping("/contracts")
    public String myContracts(@AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {
        Long clientId = user.getClientId();
        
        // 계약 목록 조회 (페이징 처리)
        Map<String, Object> contractData = contractService.getContractsByClientIdPaged(clientId, status, page, size);
        
        model.addAttribute("contracts", contractData.get("contracts"));
        model.addAttribute("totalCount", contractData.get("totalCount"));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contractData.get("totalPages"));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("contentFragment", "client/clientContracts");
        model.addAttribute("scriptFragment", "client/clientContracts");
        model.addAttribute("sideFragment", "client/clientSidebar");
        
        return "layout/main";
    }
    
    /**
     * 계약 취소
     */
    @PostMapping("/contracts/{contractId}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelContract(@PathVariable Long contractId,
                                                            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long clientId = user.getClientId();
            
            // 계약 소유자 확인
            Contract contract = contractService.getContractById(contractId);
            if (contract == null || !contract.getClientId().equals(clientId)) {
                response.put("success", false);
                response.put("message", "계약을 찾을 수 없거나 권한이 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 계약 취소 가능 여부 확인 (예약 시간 24시간 전까지만 취소 가능)
            String contractTimeStr = contract.getContractTime();
            if (contractTimeStr != null && !contractTimeStr.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime contractTime = LocalDateTime.parse(contractTimeStr, formatter);
                if (contractTime.isBefore(LocalDateTime.now().plusHours(24))) {
                    response.put("success", false);
                    response.put("message", "예약 시간 24시간 전까지만 취소가 가능합니다.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 계약 취소 처리
            contractService.cancelContract(contractId);
            
            response.put("success", true);
            response.put("message", "계약이 취소되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("계약 취소 실패", e);
            response.put("success", false);
            response.put("message", "계약 취소 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 마이페이지 - 개인정보 수정
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long clientId = user.getClientId();
        Client client = clientService.getClientById(clientId);
        
        model.addAttribute("client", client);
        model.addAttribute("contentFragment", "client/clientProfile");
        model.addAttribute("scriptFragment", "client/clientProfile");
        model.addAttribute("sideFragment", "client/clientSidebar");
        
        return "layout/main";
    }
    
    /**
     * 개인정보 수정 처리
     */
    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                              @ModelAttribute Client client,
                              RedirectAttributes redirectAttributes) {
        try {
            client.setClientId(user.getClientId());
            clientService.updateClient(client);
            
            redirectAttributes.addFlashAttribute("success", "개인정보가 수정되었습니다.");
        } catch (Exception e) {
            log.error("개인정보 수정 실패", e);
            redirectAttributes.addFlashAttribute("error", "개인정보 수정 중 오류가 발생했습니다.");
        }
        
        return "redirect:/client/profile";
    }
    
    /**
     * 비밀번호 변경
     */
    @PostMapping("/api/profile/password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestParam String currentPassword,
                                                            @RequestParam String newPassword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            clientService.changePassword(user.getClientId(), currentPassword, newPassword);
            response.put("success", true);
            response.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("비밀번호 변경 실패", e);
            response.put("success", false);
            response.put("message", "비밀번호 변경 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/api/profile/image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfileImage(@AuthenticationPrincipal CustomUserDetails user,
                                                                @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String profileImageUrl = clientService.uploadProfileImage(user.getClientId(), file);
            response.put("success", true);
            response.put("message", "프로필 이미지가 업로드되었습니다.");
            response.put("imageUrl", profileImageUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패", e);
            response.put("success", false);
            response.put("message", "이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 프로필 이미지 삭제
     */
    @DeleteMapping("/api/profile/image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteProfileImage(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            clientService.deleteProfileImage(user.getClientId());
            response.put("success", true);
            response.put("message", "프로필 이미지가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프로필 이미지 삭제 실패", e);
            response.put("success", false);
            response.put("message", "이미지 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 마이페이지 - 도장 관리
     */
    @GetMapping("/stamp")
    public String stampManagement(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long clientId = user.getClientId();
        List<StampDTO> stamps = stampService.getStampsByClientId(clientId);
        
        model.addAttribute("stamps", stamps);
        model.addAttribute("contentFragment", "client/clientStamp");
        model.addAttribute("scriptFragment", "client/clientStamp");
        model.addAttribute("sideFragment", "client/clientSidebar");
        model.addAttribute("stampGuide", "도장은 계약서에 서명 대신 사용됩니다. 한 사용자는 하나의 도장만 등록할 수 있습니다.");
        
        return "layout/main";
    }
}
