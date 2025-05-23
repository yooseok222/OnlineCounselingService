package kr.or.kosa.visang.domain.contractTemplate.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ContractTemplate {
    private Long contractTemplateId; // 계약서 템플릿 ID
    private String contractName; // 계약서 제목
    private String descript; // 계약서 내용
    private String filePath; // 파일 경로
    private Long companyId; // 회사 ID
    private String version; // 버전

    private String createdAt; // 생성일
    private String updatedAt; // 수정일

    private MultipartFile pdf; // pdf파일 파일 업로드
}
