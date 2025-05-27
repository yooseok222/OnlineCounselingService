package kr.or.kosa.visang.domain.contractTemplate.service;

import kr.or.kosa.visang.common.config.hash.HashUtil;
import kr.or.kosa.visang.common.file.FileStorageService;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.repository.ContractTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContractTemplateService {
    private final ContractTemplateMapper contractTemplateMapper;
    private final FileStorageService fileStorageService;
    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    // 계약서 템플릿 목록 조회
    public List<ContractTemplate> getAllTemplates(Long companyId) {
        return contractTemplateMapper.selectAllContractTemplates(companyId);
    }
    // 계약서 템플릿 상세 조회
    public ContractTemplate getTemplateById(Long contractTemplateId) {
        return contractTemplateMapper.selectTemplateById(contractTemplateId);
    }

    public Resource getTemplateResource(Long contractTemplateId) {
        //1. DB에서 파일 경로 + 해시값 조회
        ContractTemplate template = contractTemplateMapper.getPathAndHash(contractTemplateId);
        if (template == null) throw new RuntimeException("해당 계약서 템플릿이 존재하지 않습니다.");

        String path = template.getFilePath();
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("계약서 템플릿 파일 경로가 비어있습니다.");
        }
        String fileHash = template.getFileHash();
        if (fileHash == null || fileHash.isEmpty()) {
            throw new RuntimeException("계약서 템플릿 파일 해시값이 비어있습니다.");
        }

        //2. 파일 경로를 실제 경로로 변환
        Path filePath = Paths.get(uploadDirPdf, Paths.get(path).getFileName().toString());

        //3. 해시값 검증
        try {
            String calculatedHash = HashUtil.sha256(filePath.toString());

            if (!calculatedHash.equals(fileHash)) {
                throw new SecurityException("❌ 템플릿 파일이 위변조되었습니다. 해시 불일치");
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시값 계산 중 오류가 발생했습니다.", e);
        } catch (IOException e) {
            throw new RuntimeException("계약서 템플릿 파일을 읽는 중 오류가 발생했습니다.", e);
        }


        return fileStorageService.loadTemplateAsResource(path);
    }

    // 계약서 템플릿 생성
    public void createTemplate(ContractTemplate contractTemplate) throws IOException, NoSuchAlgorithmException {
        // 1. 먼저 ID 확보
        Long id = contractTemplateMapper.getNextTemplateId(); // selectKey만 따로 만든 쿼리로 처리
        contractTemplate.setContractTemplateId(id);

        // 2. 파일 저장
        String savePath = savePDF(contractTemplate);

        // 3. 해시 계산
        if (savePath == null || savePath.isEmpty()) {
            throw new RuntimeException("PDF 파일이 비어있거나 저장에 실패했습니다.");
        }

        // 파일 경로를 실제 경로로 변환
        String fileName = Paths.get(savePath).getFileName().toString();
        Path path = Paths.get(uploadDirPdf, fileName);
        String hash = HashUtil.sha256(path.toString());

        // 3. 해시 중복 확인
        Optional<ContractTemplate> existing = contractTemplateMapper.findByFileHash(hash);
        if (existing.isPresent()) {
            // 중복 발생 → 저장 중단
            throw new RuntimeException("이미 동일한 템플릿이 존재합니다. 이름: " + existing.get().getContractName());
        }

        // 계약서 템플릿에 파일 경로와 해시값 설정
        contractTemplate.setFileHash(hash);
        contractTemplate.setFilePath(savePath);

        // 3. insert
        contractTemplateMapper.insertTemplate(contractTemplate);
    }

    // 계약서 템플릿 수정
    public void updateTemplate(Long contractTemplateId, Long companyId, ContractTemplate contractTemplate) {
        // 계약서 템플릿 ID를 사용하여 기존 템플릿을 조회
        contractTemplate.setContractTemplateId(contractTemplateId);
        contractTemplate.setCompanyId(companyId);

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
            return fileStorageService.savePDF(pdfFile, contractTemplate.getContractTemplateId());
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 저장 중 오류가 발생했습니다.", e);
        }
    }
}
