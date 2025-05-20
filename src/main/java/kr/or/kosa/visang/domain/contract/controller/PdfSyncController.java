package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.EndContractMessage;
import kr.or.kosa.visang.domain.contract.model.UserJoinMessage;
import kr.or.kosa.visang.domain.contract.model.*;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PdfSyncController {
    
    @Autowired
    private ContractService contractService;

    @MessageMapping("/sync/page")
    @SendTo("/topic/page")
    public PdfPageMessage syncPage(PdfPageMessage message) {
        return message;
    }

    @MessageMapping("/sync/pdf")
    @SendTo("/topic/pdfPath")
    public PdfPathMessage syncPdfPath(PdfPathMessage message) {
        return message;
    }

    @MessageMapping("/sync/draw")
    @SendTo("/topic/draw")
    public DrawMessage syncDraw(DrawMessage message) {
        return message;
    }

    @MessageMapping("/sync/scroll")
    @SendTo("/topic/scroll")
    public PdfScrollMessage syncScroll(PdfScrollMessage message) {
        return message;
    }

    @MessageMapping("/sync/stamp")
    @SendTo("/topic/stamp")
    public StampMessage syncStamp(StampMessage message) {
        return message;
    }

    @MessageMapping("/sync/signature")
    @SendTo("/topic/signature")
    public SignatureMessage syncSignature(SignatureMessage message) {
        return message;
    }

    @MessageMapping("/sync/text")
    @SendTo("/topic/text")
    public TextMessage syncText(TextMessage message) {
        return message;
    }

    @MessageMapping("/sync/userJoin")
    @SendTo("/topic/userJoin")
    public UserJoinMessage syncUserJoin(UserJoinMessage message) {
        return message;
    }

    @MessageMapping("/sync/endConsult")
    @SendTo("/topic/endConsult")
    public EndContractMessage syncEndConsult(EndContractMessage message) {
        // 계약 상태를 "완료"로 업데이트
        if (message.getContractId() != null) {
            contractService.updateContractStatus(message.getContractId(), "5"); // 5는 완료 상태
        }
        
        // 클라이언트에게 메인 페이지로 리다이렉션하라는 메시지를 보냄
        message.setRedirectUrl("/main-page");
        return message;
    }

    @MessageMapping("/sync/consultComplete")
    @SendTo("/topic/consultComplete")
    public EndContractMessage syncConsultComplete(EndContractMessage message) {
        // 계약 완료 처리
        if (message.getContractId() != null) {
            contractService.updateContractStatus(message.getContractId(), "5"); // 5는 완료 상태
        }
        
        // 클라이언트에게 메인 페이지로 리다이렉션하라는 메시지를 보냄
        message.setRedirectUrl("/main-page");
        return message;
    }
    
    // PDF 요청 메시지 처리
    @MessageMapping("/sync/requestPdf")
    @SendTo("/topic/requestPdf")
    public RequestPdfMessage syncRequestPdf(RequestPdfMessage message) {
        return message;
    }
    
    // 현재 페이지 정보 요청 처리
    @MessageMapping("/sync/requestCurrentPage")
    @SendTo("/topic/requestCurrentPage")
    public RequestPageMessage syncRequestCurrentPage(RequestPageMessage message) {
        return message;
    }
}