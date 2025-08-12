package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.chat.service.ChatService;
import kr.or.kosa.visang.domain.contract.enums.ContractStatus;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.ContractDetail;
import kr.or.kosa.visang.domain.contract.model.ContractSearchRequest;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contractTemplate.service.ContractTemplateService;
import kr.or.kosa.visang.domain.page.model.PageRequest;
import kr.or.kosa.visang.domain.page.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContractController {
    private final ContractService contractService;
    private final ContractTemplateService contractTemplateService;
    private final ChatService chatService;

    @GetMapping("/contract-list/{status}")
    public String templateList(@AuthenticationPrincipal CustomUserDetails admin,
                               @PathVariable("status") String status,
                               Model model
    ) {
        try {
            String enumName = status.replace("-", "_").toUpperCase();
            ContractStatus contractStatus = ContractStatus.valueOf(enumName);

            model.addAttribute("pageTitle", contractStatus.name().replace("_", " "));
            model.addAttribute("status", contractStatus.name());

            // 공통 레이아웃으로 전체 페이지 렌더링
            model.addAttribute("contentFragment", "contractManagement");
            model.addAttribute("scriptFragment", "contractManagement");
            return "admin/adminLayout";
        } catch (IllegalArgumentException e) {
            // 잘못된 status가 들어왔을 때
            return "error/404";
        }
    }


    @GetMapping("/contracts/search")
    @ResponseBody
    public PageResult<Contract> searchContracts(@AuthenticationPrincipal CustomUserDetails admin,
                                                @ModelAttribute ContractSearchRequest request,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size
    ) {
        Long companyId = admin.getCompanyId(); // 로그인한 사용자의 회사 ID
        PageRequest pr = new PageRequest(page, size);
        
        request.setCompanyId(companyId); // 검색 요청에 회사 ID 추가
        
        
        return contractService.searchContracts(request, pr);// 계약 목록 반환
    }

    @GetMapping("/contract/{contractId}")
    @ResponseBody
    public ContractDetail contractDetail(@PathVariable Long contractId) {
        ContractDetail contract = contractService.getContractDetail(contractId);
        if (contract == null) {
            throw new RuntimeException("contract not found");
        }

        return contract;
    }

    @GetMapping("/files/{templateId}/preview")
    public ResponseEntity<Resource> previewPdf(@PathVariable Long templateId) {
        try {
            Resource pdf = contractTemplateService.getTemplateResource(templateId);
            if( pdf == null || !pdf.exists() ) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF 파일을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (SecurityException e) {
            // 파일 변조 등 보안 이슈
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            // 일반적인 예외(존재하지 않음, 해시 오류 등)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 기타 예외
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일을 미리보기할 수 없습니다.");
        }
    }

    @GetMapping("/files/{contractId}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long contractId) {
        try {
            Resource pdf = contractTemplateService.getTemplateResource(contractId);

            if (pdf == null || !pdf.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract_template.pdf\"")
                    .body(pdf);
        } catch (SecurityException e) {
            // 파일 변조 등 보안 이슈
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            // 일반적인 예외(존재하지 않음, 해시 오류 등)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 기타 예외
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일을 미리보기할 수 없습니다.");
        }
    }

    @GetMapping("/files/signed/{contractId}/preview")
    public ResponseEntity<Resource> previewSignedPdf(@PathVariable Long contractId) {
        System.out.println("contractId ID: " + contractId + "signed pdf preview");


        try {
            // 계약 ID로 PDF ID 조회
            Long pdfId = contractService.getPdfIdByContractId(contractId);

            System.out.println("PDF ID: " + pdfId + "signed pdf preview");

            Resource SignedPdf = contractTemplateService.getSignedPdfResource(pdfId);
            System.out.println("SignedPdf: " + SignedPdf + "signed pdf preview");

            if( SignedPdf == null || !SignedPdf.exists() ) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF 파일을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(SignedPdf);
        } catch (SecurityException e) {
            // 파일 변조 등 보안 이슈
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            // 일반적인 예외(존재하지 않음, 해시 오류 등)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 기타 예외
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일을 미리보기할 수 없습니다.");
        }
    }

    @GetMapping("/files/signed/{contractId}/download")
    public ResponseEntity<Resource> downloadSignedPdf(@PathVariable Long contractId) {
        try {
            // 계약 ID로 PDF ID 조회
            Long pdfId = contractService.getPdfIdByContractId(contractId);

            Resource SignedPdf = contractTemplateService.getSignedPdfResource(pdfId);

            if (SignedPdf == null || !SignedPdf.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract_template.pdf\"")
                    .body(SignedPdf);
        } catch (SecurityException e) {
            // 파일 변조 등 보안 이슈
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            // 일반적인 예외(존재하지 않음, 해시 오류 등)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 기타 예외
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일을 미리보기할 수 없습니다.");
        }
    }

    /**
     * 채팅 파일 다운로드 API
     */
    @GetMapping("/contract/chat/{contractId}/download")
    public ResponseEntity<Resource> downloadChatTing(@PathVariable Long contractId) {
        try {
            File file = chatService.getChatFile(contractId);
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("채팅 파일 다운로드 실패: voiceId={}", contractId, e);
            return ResponseEntity.notFound().build();
        }
    }
}