package kr.or.kosa.visang.domain.contractTemplate.service;

import kr.or.kosa.visang.common.file.FileStorageService;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.repository.ContractTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractTemplateService {
    private final ContractTemplateMapper contractTemplateMapper;
    private final FileStorageService fileStorageService;
    // 계약서 템플릿 관련 비즈니스 로직 구현
    // 예: 계약서 템플릿 목록 조회, 추가, 수정, 삭제 등의 메서드 정의

    // 계약서 템플릿 목록 조회
    public List<ContractTemplate> getAllTemplates(Long companyId) {
        return contractTemplateMapper.selectAllContractTemplates(companyId);
    }
    // 계약서 템플릿 상세 조회
    public ContractTemplate getTemplateById(Long contractTemplateId) {
        return contractTemplateMapper.selectTemplateById(contractTemplateId);
    }

    // 계약서 템플릿 생성
    public void createTemplate(ContractTemplate contractTemplate) {
        // 1. 먼저 ID 확보
        Long id = contractTemplateMapper.getNextTemplateId(); // selectKey만 따로 만든 쿼리로 처리
        contractTemplate.setContractTemplateId(id);

        // 2. 파일 저장
        savePDF(contractTemplate);

        // 3. insert
        contractTemplateMapper.insertTemplate(contractTemplate);
    }

    // 계약서 템플릿 수정
    public void updateTemplate(Long contractTemplateId, ContractTemplate contractTemplate) {
        // 계약서 템플릿 ID를 사용하여 기존 템플릿을 조회
        contractTemplate.setContractTemplateId(contractTemplateId);

        // 파일이 새로 업로드된 경우에만 새로 저장
        if (contractTemplate.getPdf() != null && !contractTemplate.getPdf().isEmpty()) {
            String newPath = savePDF(contractTemplate);
            contractTemplate.setFilePath(newPath);
        }
        contractTemplateMapper.updateTemplate(contractTemplate);
    }

    // 계약서 템플릿 삭제
    public void deleteTemplate(Long contractTemplateId) {
        contractTemplateMapper.deleteTemplate(contractTemplateId);
    }

    private String savePDF(ContractTemplate contractTemplate) {
        MultipartFile pdfFile = contractTemplate.getPdf();
        try {
            String savePath = fileStorageService.savePDF(pdfFile, contractTemplate.getContractTemplateId());
            contractTemplate.setFilePath(savePath);
            return savePath;
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 저장 중 오류가 발생했습니다.", e);
        }
    }
}
