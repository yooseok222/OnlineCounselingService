package kr.or.kosa.visang.domain.contract.repository;

import kr.or.kosa.visang.domain.contract.model.PdfDTO;
import kr.or.kosa.visang.domain.pdf.model.PDF;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PdfMapper {
    // PDF 조회
    PdfDTO selectPdfById(Long pdfId);
    
    // 계약 ID로 PDF 조회
    List<PdfDTO> selectPdfsByContractId(Long contractId);
    
    // PDF 추가
    int insertPdf(PdfDTO pdf);

    PDF getPathAndHash(Long pdfId);
    
    // PDF 정보 업데이트
    int updatePdf(PdfDTO pdf);
    
    // PDF 삭제
    int deletePdf(Long pdfId);
} 