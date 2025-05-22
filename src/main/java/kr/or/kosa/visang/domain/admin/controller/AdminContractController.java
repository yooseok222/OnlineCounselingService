package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contract.enums.ContractStatus;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.ContractDetail;
import kr.or.kosa.visang.domain.contract.model.ContractSearchRequest;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import kr.or.kosa.visang.domain.contractTemplate.service.ContractTemplateService;
import kr.or.kosa.visang.domain.page.model.PageRequest;
import kr.or.kosa.visang.domain.page.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContractController {
    private final ContractService contractService;
    private final ContractTemplateService contractTemplateService;

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

        System.out.println("request = " + request);
        System.out.println("page = " + page);
        System.out.println("size = " + size);

        request.setCompanyId(companyId); // 검색 요청에 회사 ID 추가
        PageResult<Contract> con =  contractService.searchContracts(request, pr);
        System.out.println("con = " + con);
        return con; // 계약 목록을 반환합니다.
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
        Resource pdf = contractTemplateService.getTemplateResource(templateId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/files/{templateId}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long templateId) {
        Resource pdf = contractTemplateService.getTemplateResource(templateId);
        System.out.println("file = " + pdf);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract_template.pdf\"")
                .body(pdf);
    }
}