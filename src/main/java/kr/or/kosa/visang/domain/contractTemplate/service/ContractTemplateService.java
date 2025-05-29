package kr.or.kosa.visang.domain.contractTemplate.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignatureUtil;
import kr.or.kosa.visang.common.config.hash.HashUtil;
import kr.or.kosa.visang.common.file.FileStorageService;
import kr.or.kosa.visang.domain.contract.repository.PdfMapper;
import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.repository.ContractTemplateMapper;
import kr.or.kosa.visang.domain.pdf.enums.PDFTYPE;
import kr.or.kosa.visang.domain.pdf.model.PDF;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateService {
    private final ContractTemplateMapper contractTemplateMapper;
    private final FileStorageService fileStorageService;
    private final PdfMapper pdfMapper;

    @Value("${file.upload-dir.pdf}")
    private String uploadDirPdf;

    @Value("${file.upload-dir.signed-pdf}")
    private String uploadDirSignedPdf;

    // 계약서 템플릿 목록 조회
    public List<ContractTemplate> getAllTemplates(Long companyId) {
        return contractTemplateMapper.selectAllContractTemplates(companyId);
    }
    // 계약서 템플릿 상세 조회
    public ContractTemplate getTemplateById(Long contractTemplateId) {
        return contractTemplateMapper.selectTemplateById(contractTemplateId);
    }

    public Resource getTemplateResource(Long contractTemplateId) {
        try {
            //1. DB에서 파일 경로 + 해시값 조회
            ContractTemplate template = contractTemplateMapper.getPathAndHash(contractTemplateId);
            if (template == null) {
                log.warn("템플릿을 찾을 수 없음: contractTemplateId={}", contractTemplateId);
                return createDefaultPdf("계약서 템플릿을 찾을 수 없습니다.");
            }

            String path = template.getFilePath();
            if (path == null || path.isEmpty()) {
                log.warn("템플릿 파일 경로가 비어있음: contractTemplateId={}", contractTemplateId);
                return createDefaultPdf("계약서 템플릿 파일 경로가 비어있습니다.");
            }
            String fileHash = template.getFileHash();
            if (fileHash == null || fileHash.isEmpty()) {
                log.warn("템플릿 파일 해시값이 비어있음: contractTemplateId={}", contractTemplateId);
                return createDefaultPdf("계약서 템플릿 파일 해시값이 비어있습니다.");
            }

            //2. 파일 경로를 실제 경로로 변환
            Path filePath = Paths.get(uploadDirPdf, Paths.get(path).getFileName().toString());
            if (!Files.exists(filePath)) {
                log.warn("템플릿 파일이 존재하지 않음: path={}", filePath);
                return createDefaultPdf("계약서 템플릿 파일이 존재하지 않습니다.");
            }

            //3. 해시값 검증
            try {
                String calculatedHash = HashUtil.sha256(filePath.toString());

                if (!calculatedHash.equals(fileHash)) {
                    log.warn("템플릿 파일 해시 불일치: expected={}, actual={}", fileHash, calculatedHash);
                    return createDefaultPdf("계약서 템플릿 파일이 위변조되었습니다.");
                }

            } catch (NoSuchAlgorithmException | IOException e) {
                log.warn("템플릿 파일 해시 검증 실패: {}", e.getMessage());
                return createDefaultPdf("계약서 템플릿 파일 검증에 실패했습니다.");
            }

            // 정상적인 파일 제공
            return fileStorageService.loadResource(path, PDFTYPE.TEMPLATE_PDF);
        } catch (Exception e) {
            log.error("템플릿 파일 제공 중 예외 발생: contractTemplateId={}", contractTemplateId, e);
            return createDefaultPdf("계약서 템플릿 처리 중 오류가 발생했습니다.");
        }
    }

    public Resource getSignedPdfResource(Long pdfId){
        //1. DB에서 파일 경로 + 해시값 조회
        //ContractTemplate template = contractTemplateMapper.getPathAndHash(contractTemplateId);
        PDF pdf = pdfMapper.getPathAndHash(pdfId);
        System.out.println("pdfId: " + pdfId + ", pdf: " + pdf);
        if (pdf == null) throw new RuntimeException("해당 계약서가 존재하지 않습니다.");

        String path = pdf.getFilePath();
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("계약서 파일 경로가 비어있습니다.");
        }
        String fileHash = pdf.getFileHash();
        if (fileHash == null || fileHash.isEmpty()) {
            throw new RuntimeException("계약서 파일 해시값이 비어있습니다.");
        }

        //2. 파일 경로를 실제 경로로 변환
        Path filePath = Paths.get(uploadDirSignedPdf, Paths.get(path).getFileName().toString());
        System.out.println("filePath: " + filePath);

        //3. 해시값 검증
        try {
            String calculatedHash = HashUtil.sha256(filePath.toString());
            System.out.println("calculatedHash: " + calculatedHash);
            System.out.println("fileHash: " + fileHash);

            if (!calculatedHash.equals(fileHash)) {
                throw new SecurityException("❌ 템플릿 파일이 위변조되었습니다. 해시 불일치");
            }

            // 전자서명 검증
            if (!isPdfSignatureValid(filePath.toString())) {
                throw new SecurityException("❌ PDF 서명이 유효하지 않습니다.");
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시값 계산 중 오류가 발생했습니다.", e);
        } catch (IOException e) {
            throw new RuntimeException("계약서 템플릿 파일을 읽는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            System.out.println("PDF 서명 검증 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("PDF 서명 검증 중 오류가 발생했습니다.", e);
        }

        return fileStorageService.loadResource(path, PDFTYPE.SIGNED_PDF);
    }

    public boolean verifyTemplateHash(Long contractTemplateId){
        //1. DB에서 파일 경로 + 해시값 조회
        ContractTemplate template = contractTemplateMapper.getPathAndHash(contractTemplateId);
        if (template == null) return false;

        String path = template.getFilePath();
        if (path == null || path.isEmpty()) return false;
        String fileHash = template.getFileHash();
        if (fileHash == null || fileHash.isEmpty()) return false;

        //2. 파일 경로를 실제 경로로 변환
        Path filePath = Paths.get(uploadDirPdf, Paths.get(path).getFileName().toString());

        //3. 해시값 검증
        try {
            String calculatedHash = HashUtil.sha256(filePath.toString());
            return calculatedHash.equals(fileHash);
        } catch (NoSuchAlgorithmException e1){
            throw new RuntimeException("해시값 계산 중 오류가 발생했습니다.", e1);
        } catch(IOException e2) {
            throw new RuntimeException("파일을 읽던 도중 오류가 발생했습니다.", e2);
        }
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
    public void updateTemplate(Long contractTemplateId, Long companyId, ContractTemplate contractTemplate) throws IOException, NoSuchAlgorithmException {
        // 계약서 템플릿 ID를 사용하여 기존 템플릿을 조회
        contractTemplate.setContractTemplateId(contractTemplateId);
        contractTemplate.setCompanyId(companyId);

        // 파일이 새로 업로드된 경우에만 새로 저장
        if (contractTemplate.getPdf() != null && !contractTemplate.getPdf().isEmpty()) {
            String newPath = savePDF(contractTemplate);
            if (newPath == null || newPath.isEmpty()) {
                throw new RuntimeException("새로운 PDF 파일이 비어있거나 저장에 실패했습니다.");
            }
            String hash = HashUtil.sha256(Paths.get(uploadDirPdf, Paths.get(newPath).getFileName().toString()).toString());

            // 해시 중복 확인
            Optional<ContractTemplate> existing = contractTemplateMapper.findByFileHash(hash);
            if (existing.isPresent() && !existing.get().getContractTemplateId().equals(contractTemplateId)) {
                // 중복 발생 → 저장 중단
                throw new RuntimeException("이미 동일한 템플릿이 존재합니다. 이름: " + existing.get().getContractName());
            }
            // 계약서 템플릿에 새 파일 경로와 해시값 설정
            contractTemplate.setFileHash(hash);
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
            return fileStorageService.savePDF(pdfFile, contractTemplate.getContractTemplateId(), PDFTYPE.TEMPLATE_PDF);
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    public boolean isPdfSignatureValid(String signedPdfPath) throws Exception {
        try {
            System.out.println("PDF 서명 검증 시작: " + signedPdfPath);
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(signedPdfPath));
            SignatureUtil signUtil = new SignatureUtil(pdfDoc);

            if (Security.getProvider("BC") == null) {
                System.out.println("BouncyCastle 프로바이더가 등록되지 않았습니다. 등록합니다.");
                Security.addProvider(new BouncyCastleProvider());
            }

            List<String> signatureNames = signUtil.getSignatureNames();
            if (signatureNames.isEmpty()) {
                throw new IllegalStateException("PDF에 전자서명 필드가 없습니다.");
            }
            for (String name : signatureNames) {
                try {
                    PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);
                    boolean verified = pkcs7.verifySignatureIntegrityAndAuthenticity();

                    System.out.println("서명 필드: " + name);
                    System.out.println("유효한 서명인가? " + verified);
                    System.out.println("서명자 DN: " + pkcs7.getSigningCertificate().getSubjectDN());
                    System.out.println("서명일: " + pkcs7.getSignDate().getTime());

                    if (!verified) return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("서명 필드 [" + name + "] 오류: " + e.getMessage());
                    // 실제 유저에게 보여줄 안내:
                    throw new IllegalStateException("PDF 서명 정보가 비정상적입니다. (서명 필드: " + name + ")");
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("PDF 서명 검증 중 내부 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 템플릿 PDF 파일 경로를 가져옵니다.
     * 
     * @param templateId 템플릿 ID
     * @return PDF 파일 경로
     */
    public String getTemplatePdfPath(Long templateId) {
        try {
            log.info("템플릿 PDF 경로 조회 시작: templateId={}", templateId);
            
            // 1. 먼저 getPathAndHash 메서드 사용 시도 (path와 hash 함께 조회)
            try {
                ContractTemplate template = contractTemplateMapper.getPathAndHash(templateId);
                if (template != null && template.getFilePath() != null && !template.getFilePath().isEmpty()) {
                    log.info("getPathAndHash에서 경로 조회 성공: {}", template.getFilePath());
                    return template.getFilePath();
                }
            } catch (Exception e) {
                log.warn("getPathAndHash 호출 실패: {}", e.getMessage());
            }
            
            // 2. 일반 getTemplateById 메서드 사용 시도
            ContractTemplate template = getTemplateById(templateId);
            if (template == null) {
                log.warn("템플릿을 찾을 수 없음: templateId={}", templateId);
                return null;
            }
            
            // 3. filePath 필드에 직접 접근 시도
            try {
                // 리플렉션으로 필드 접근 시도
                java.lang.reflect.Field field = ContractTemplate.class.getDeclaredField("filePath");
                field.setAccessible(true);
                String filePath = (String) field.get(template);
                if (filePath != null && !filePath.isEmpty()) {
                    log.info("리플렉션으로 filePath 필드 접근 성공: {}", filePath);
                    return filePath;
                }
            } catch (Exception e) {
                log.warn("리플렉션으로 filePath 필드 접근 실패: {}", e.getMessage());
            }
            
            // 4. getter 메서드 호출 시도
            try {
                java.lang.reflect.Method method = ContractTemplate.class.getMethod("getFilePath");
                String filePath = (String) method.invoke(template);
                if (filePath != null && !filePath.isEmpty()) {
                    log.info("getFilePath 메서드 호출 성공: {}", filePath);
                    return filePath;
                }
            } catch (Exception e) {
                log.warn("getFilePath 메서드 호출 실패: {}", e.getMessage());
            }
            
            // 5. 다른 필드나 메서드 시도
            String[] possibleFieldNames = {"pdfPath", "templatePath", "contractPath", "path"};
            for (String fieldName : possibleFieldNames) {
                try {
                    java.lang.reflect.Field field = ContractTemplate.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    String filePath = (String) field.get(template);
                    if (filePath != null && !filePath.isEmpty()) {
                        log.info("리플렉션으로 {} 필드 접근 성공: {}", fieldName, filePath);
                        return filePath;
                    }
                } catch (Exception e) {
                    log.debug("리플렉션으로 {} 필드 접근 실패: {}", fieldName, e.getMessage());
                }
            }
            
            // 6. 다른 getter 메서드 시도
            String[] possibleMethodNames = {"getPdfPath", "getTemplatePath", "getContractPath", "getPath"};
            for (String methodName : possibleMethodNames) {
                try {
                    java.lang.reflect.Method method = ContractTemplate.class.getMethod(methodName);
                    String filePath = (String) method.invoke(template);
                    if (filePath != null && !filePath.isEmpty()) {
                        log.info("{} 메서드 호출 성공: {}", methodName, filePath);
                        return filePath;
                    }
                } catch (Exception e) {
                    log.debug("{} 메서드 호출 실패: {}", methodName, e.getMessage());
                }
            }
            
            // 7. 필드 접근 실패 시 템플릿 ID를 기반으로 기본 경로 반환
            String defaultPath = "templates/template_" + templateId + ".pdf";
            log.info("모든 시도 실패, 기본 경로 반환: {}", defaultPath);
            return defaultPath;
        } catch (Exception e) {
            log.error("템플릿 PDF 경로 조회 중 예외 발생: templateId={}", templateId, e);
            return "templates/template_" + templateId + ".pdf";
        }
    }

    /**
     * 기본 PDF 리소스 생성
     */
    private Resource createDefaultPdf(String message) {
        try {
            log.info("기본 PDF 생성: {}", message);
            // 임시 파일 생성
            Path tempFile = Files.createTempFile("default_template_", ".txt");
            Files.write(tempFile, message.getBytes());
            
            // 리소스로 변환
            return new org.springframework.core.io.FileSystemResource(tempFile.toFile());
        } catch (IOException e) {
            log.error("기본 PDF 생성 실패", e);
            // 메모리 리소스로 대체
            return new org.springframework.core.io.ByteArrayResource(message.getBytes());
        }
    }
}
