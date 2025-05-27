package kr.or.kosa.visang.domain.contract.controller;

import kr.or.kosa.visang.domain.contract.model.EndContractMessage;
import kr.or.kosa.visang.domain.contract.model.UserJoinMessage;
import kr.or.kosa.visang.domain.contract.model.*;
import kr.or.kosa.visang.domain.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Controller
public class PdfSyncController {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfSyncController.class);
    
    @Autowired
    private ContractService contractService;

    @MessageMapping("/sync/page")
    @SendTo("/topic/page")
    public PdfPageMessage syncPage(PdfPageMessage message) {
        logger.info("페이지 동기화 메시지 수신: 페이지 {}", message.getPageNumber());
        return message;
    }

    @MessageMapping("/sync/pdf")
    @SendTo("/topic/pdfPath")
    public PdfPathMessage syncPdfPath(PdfPathMessage message) {
        logger.info("PDF 경로 동기화 메시지 수신: {}", message.getUrl());
        return message;
    }

    @MessageMapping("/sync/draw")
    @SendTo("/topic/draw")
    public DrawMessage syncDraw(DrawMessage message) {
        logger.info("드로잉 동기화 메시지 수신: 타입={}, 세션={}", message.getType(), message.getSessionId());
        
        // 페이지 필드 동기화 (page 필드를 pageNumber로 매핑)
        if (message.getPageNumber() == 0 && message.getPage() > 0) {
            message.setPageNumber(message.getPage());
        }
        
        return message;
    }

    @MessageMapping("/sync/scroll")
    @SendTo("/topic/scroll")
    public PdfScrollMessage syncScroll(PdfScrollMessage message) {
        logger.info("스크롤 동기화 메시지 수신: 페이지 {}", message.getPageNumber());
        return message;
    }

    @MessageMapping("/sync/stamp")
    @SendTo("/topic/stamp")
    public StampMessage syncStamp(StampMessage message) {
        logger.info("도장 동기화 메시지 수신: 페이지 {}", message.getPageNumber());
        return message;
    }

    @MessageMapping("/sync/signature")
    @SendTo("/topic/signature")
    public SignatureMessage syncSignature(SignatureMessage message) {
        logger.info("서명 동기화 메시지 수신");
        return message;
    }

    @MessageMapping("/sync/text")
    @SendTo("/topic/text")
    public TextMessage syncText(TextMessage message) {
        logger.info("텍스트 동기화 메시지 수신: 텍스트=\"{}\", 세션={}", message.getText(), message.getSessionId());
        
        // 페이지 필드 동기화 (page 필드를 pageNumber로 매핑)
        if (message.getPageNumber() == 0 && message.getPage() > 0) {
            message.setPageNumber(message.getPage());
        }
        
        return message;
    }

    @MessageMapping("/sync/userJoin")
    @SendTo("/topic/userJoin")
    public UserJoinMessage syncUserJoin(UserJoinMessage message) {
        logger.info("사용자 입장 메시지 수신: 타입={}, 세션={}", message.getUserType(), message.getSessionId());
        return message;
    }

    @MessageMapping("/sync/endConsult")
    @SendTo("/topic/endConsult")
    public EndContractMessage syncEndConsult(EndContractMessage message) {
        // 상담 종료는 ConsultationController에서 이미 처리되므로 여기서는 상태 업데이트 하지 않음
        logger.info("상담 종료 WebSocket 메시지 처리: 계약 ID={}", message.getContractId());
        
        // 클라이언트에게 메인 페이지로 리다이렉션하라는 메시지를 보냄
        message.setRedirectUrl("/");
        return message;
    }

    @MessageMapping("/sync/consultComplete")
    @SendTo("/topic/consultComplete")
    public EndContractMessage syncConsultComplete(EndContractMessage message) {
        // 상담 완료는 ConsultationController에서 이미 처리되므로 여기서는 상태 업데이트 하지 않음
        logger.info("상담 완료 WebSocket 메시지 처리: 계약 ID={}", message.getContractId());
        
        // 클라이언트에게 메인 페이지로 리다이렉션하라는 메시지를 보냄
        message.setRedirectUrl("/");
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

    @MessageMapping("/room/{roomId}/draw")
    @SendTo("/topic/room/{roomId}/draw")
    public DrawMessage roomDraw(DrawMessage message) {
        logger.info("방 단위 드로잉 메시지 수신: 세션={}", message.getSessionId());
        return message;
    }
    
    @MessageMapping("/room/{roomId}/text")
    @SendTo("/topic/room/{roomId}/text")
    public TextMessage roomText(TextMessage message) {
        logger.info("방 단위 텍스트 메시지 수신: 세션={}", message.getSessionId());
        return message;
    }
    
    // 특정 방에 대한 도장 메시지 처리
    @MessageMapping("/room/{roomId}/stamp")
    @SendTo("/topic/room/{roomId}/stamp")
    public Map<String, Object> roomStamp(@DestinationVariable String roomId, Map<String, Object> stampData) {
        logger.info("방 단위 도장 메시지 수신: 세션={}, 페이지={}", 
                roomId, stampData.get("page"));
        return stampData;
    }
    
    // 특정 방에 대한 서명 메시지 처리
    @MessageMapping("/room/{roomId}/signature")
    @SendTo("/topic/room/{roomId}/signature")
    public Map<String, Object> roomSignature(@DestinationVariable String roomId, Map<String, Object> signatureData) {
        logger.info("방 단위 서명 메시지 수신: 세션={}, 페이지={}", 
                roomId, signatureData.get("page"));
        return signatureData;
    }
    
    // 특정 방 입장 메시지 처리
    @MessageMapping("/room/{roomId}/join")
    @SendTo("/topic/room/{roomId}/join")
    public Map<String, Object> roomJoin(@DestinationVariable String roomId, Map<String, Object> joinData) {
        logger.info("방 입장 메시지 수신: 세션={}, 역할={}", 
                roomId, joinData.get("role"));
        return joinData;
    }
    
    // 특정 방 동기화 요청 메시지 처리
    @MessageMapping("/room/{roomId}/sync")
    @SendTo("/topic/room/{roomId}/sync")
    public Map<String, Object> roomSync(@DestinationVariable String roomId, Map<String, Object> syncData) {
        logger.info("방 동기화 요청 메시지 수신: 세션={}, 요청자={}", 
                roomId, syncData.get("sender"));
        return syncData;
    }
}