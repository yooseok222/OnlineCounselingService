package kr.or.kosa.visang.domain.admin.controller;

import kr.or.kosa.visang.common.config.security.CustomUserDetails;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.service.ContractTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContractTemplateController {
    private final ContractTemplateService contractTemplateService;


    @GetMapping("/template/{id}")
    @ResponseBody
    public ContractTemplate template(@PathVariable("id") Long id) {
        // 계약서 템플릿 상세 조회 로직
        ContractTemplate contractTemplate = contractTemplateService.getTemplateById(id);
        return contractTemplate;
    }

    @GetMapping("/template-list")
    public String templateList(@AuthenticationPrincipal CustomUserDetails admin, Model model) {
        Long companyId = admin.getCompanyId(); // 로그인한 사용자의 회사 ID
        model.addAttribute("templateList", contractTemplateService.getAllTemplates(companyId));

        // 공통 레이아웃으로 전체 페이지 렌더링
        model.addAttribute("contentFragment", "contractTemplateManagement");
        model.addAttribute("scriptFragment", "contractTemplateManagement");
        return "admin/adminLayout";

    }

    @PostMapping("/template")
    public ResponseEntity<String> createTemplate(@AuthenticationPrincipal CustomUserDetails admin,
                                                 ContractTemplate contractTemplate) throws IOException, NoSuchAlgorithmException {
        Long companyId = admin.getCompanyId(); // 로그인한 사용자의 회사 ID
        contractTemplate.setCompanyId(companyId);

        // 계약서 템플릿 생성 로직
        contractTemplateService.createTemplate(contractTemplate);
        return new ResponseEntity<String>("success", HttpStatus.CREATED);
    }

    @PutMapping("/template/{templateId}")
    public ResponseEntity<String> updateTemplate(@AuthenticationPrincipal CustomUserDetails admin,
                                                 @PathVariable Long templateId,
                                                 ContractTemplate contractTemplate
    ) {
        Long companyId = admin.getCompanyId(); // 로그인한 사용자의 회사 ID
        try{
            contractTemplateService.updateTemplate(templateId, companyId, contractTemplate);
        }catch (Exception e){
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<String>("success", HttpStatus.OK);
    }


    @DeleteMapping("/template/{id}")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
        contractTemplateService.deleteTemplate(id);
        return new ResponseEntity<String>("success", HttpStatus.OK);
    }

}
