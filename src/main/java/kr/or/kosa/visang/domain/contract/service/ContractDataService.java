package kr.or.kosa.visang.domain.contract.service;

import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.PdfDTO;
//import kr.or.kosa.visang.domain.contract.service.ContractService;
//import kr.or.kosa.visang.domain.contract.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractDataService {

    @Autowired
    private ContractService contractService;
    
    @Autowired
    private PdfService pdfService;

    // 모든 계약 목록 조회
    public List<Contract> getAllContracts() {
        return contractService.getAllContracts();
    }

    // 계약 데이터 조회
    public Map<String, Object> getContractData(Long contractId) {
        Map<String, Object> contractData = new HashMap<>();
        
        // 계약 정보 조회
        Contract contract = contractService.getContractById(contractId);
        contractData.put("contract", contract);
        
        if (contract != null) {
            // PDF 정보 조회
            List<PdfDTO> pdfs = pdfService.getPdfsByContractId(contractId);
            contractData.put("pdfs", pdfs);
        }
        
        return contractData;
    }
    
    // 계약 상태 업데이트
    public boolean updateContractStatus(Long contractId, String status) {
        int result = contractService.updateContractStatus(contractId, status);
        return result > 0;
    }
    
    // 계약 메모 업데이트
    public boolean updateContractMemo(Long contractId, String memo) {
        int result = contractService.updateContractMemo(contractId, memo);
        return result > 0;
    }
    
    // 계약 생성
    public int createContract(Contract contract) {
        return contractService.createContract(contract);
    }
} 