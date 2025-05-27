// WebSocket 통신 관련 기능

// WebSocket 연결 상태 및 재연결 시도 횟수
let reconnectAttempts = 0;
let reconnectTimer = null;
let isConnecting = false;

// 세션 ID 확인 전역 함수 - 일관된 세션 ID 사용을 위한 유틸리티 함수
function ensureValidSessionId() {
  // URL에서 세션 ID 가져오기 (항상 최우선)
  const urlParams = new URLSearchParams(window.location.search);
  const urlSessionId = urlParams.get('session');
  
  if (urlSessionId) {
    // URL에 세션 ID가 있으면 이를 절대적 진실로 사용
    if (sessionId !== urlSessionId) {
      console.log(`세션 ID 불일치 수정: ${sessionId} → ${urlSessionId}`);
      sessionId = urlSessionId;
      sessionStorage.setItem("sessionId", sessionId);
      
      // URL 업데이트 (일관성 유지)
      const url = new URL(window.location.href);
      url.searchParams.set('session', sessionId);
      window.history.replaceState({}, '', url);
    }
    return sessionId;
  } else if (sessionStorage.getItem("sessionId")) {
    // URL에 없고 세션 스토리지에 있으면 사용
    sessionId = sessionStorage.getItem("sessionId");
    console.log("세션 스토리지에서 세션 ID 복구:", sessionId);
    
    // URL 업데이트 (일관성 유지)
    const url = new URL(window.location.href);
    url.searchParams.set('session', sessionId);
    window.history.replaceState({}, '', url);
    return sessionId;
  }
  
  // 세션 ID가 없으면 null 반환 (오류 상황)
  return null;
}

// WebSocket 연결 초기화
function initializeWebSocket() {
  // 세션 ID 확인 - 유틸리티 함수 사용
  const validSessionId = ensureValidSessionId();
  if (!validSessionId) {
    console.error("유효한 세션 ID를 찾을 수 없어 WebSocket 연결을 초기화할 수 없습니다.");
    showToast("연결 오류", "세션 ID를 찾을 수 없습니다. 페이지를 새로고침해주세요.", "error");
    return;
  }
  
  // 역할 확인
  const urlParams = new URLSearchParams(window.location.search);
  const urlUserRole = urlParams.get('role');
  
  // URL에 있는 역할을 우선 사용
  if (urlUserRole) {
    if (userRole !== urlUserRole) {
      console.log(`사용자 역할 불일치 수정: ${userRole} → ${urlUserRole}`);
      userRole = urlUserRole;
      sessionStorage.setItem("role", userRole);
    }
  } else if (!userRole) {
    userRole = sessionStorage.getItem("role");
    console.log("세션 스토리지에서 사용자 역할 가져옴:", userRole);
  }
  
  // 사용자 역할이 없으면 오류 처리
  if (!userRole) {
    console.error("사용자 역할을 찾을 수 없어 WebSocket 연결을 초기화할 수 없습니다.");
    showToast("연결 오류", "사용자 역할을 찾을 수 없습니다. 페이지를 새로고침해주세요.", "error");
    return;
  }

  // 이미 연결 중이면 중복 연결 방지
  if (isConnecting) {
    console.log("WebSocket 연결이 이미 진행 중입니다.");
    return;
  }

  // 연결 중 상태로 설정
  isConnecting = true;
  console.log(`WebSocket 연결 초기화 - 세션: ${sessionId}, 역할: ${userRole}`);

  try {
    // 이미 연결되어 있으면 연결 해제
    if (stompClient && stompClient.connected) {
      console.log("이미 연결된 WebSocket 연결 해제");
      stompClient.disconnect(function() {
        console.log("이전 WebSocket 연결이 정상적으로 해제되었습니다.");
        // 새 연결 시도
        createStompConnection();
      });
    } else {
      // 새 연결 시도
      createStompConnection();
    }
  } catch (e) {
    console.error("WebSocket 연결 초기화 오류:", e);
    isConnecting = false;
    scheduleReconnect();
  }
}

// STOMP 연결 생성 함수 (코드 분리로 가독성 향상)
function createStompConnection() {
  console.log("WebSocket 연결 시도...");
  
  // SockJS 연결
  const socket = new SockJS('/ws');
  
  // Stomp 클라이언트 생성
  stompClient = Stomp.over(socket);
  
  // 디버그 로그 활성화 (개발 중에는 활성화, 프로덕션에서는 null로 설정)
  stompClient.debug = function(str) {
    console.log(str); // WebSocket 디버그 메시지 출력
  };
  
  // 연결 옵션 설정
  const connectOptions = {
    // 헤더에 세션 ID 및 사용자 역할 추가
    sessionId: sessionId,
    role: userRole
  };
  
  // 연결 시도
  stompClient.connect(
    connectOptions,
    function(frame) {
      // 연결 성공
      console.log("WebSocket 연결 성공:", frame);
      isConnecting = false;
      reconnectAttempts = 0;
      
      // 메시지 수신을 위한 토픽 구독
      subscribeToTopics();
      
      // 연결 성공 토스트 메시지
      showToast("연결 성공", "실시간 통신이 연결되었습니다.", "success");
      
      // 방 입장 메시지 전송 (상담원, 고객 모두 입장 메시지 전송)
      stompClient.send(`/app/room/${sessionId}/join`, {}, JSON.stringify({
        type: "join",
        userId: `${userRole}_${Date.now()}`,
        role: userRole,
        sessionId: sessionId,
        timestamp: Date.now()
      }));
      
      // 페이지가 준비되었음을 알리는 사용자 정의 이벤트 발생
      document.dispatchEvent(new CustomEvent('websocketConnected', {
        detail: { sessionId: sessionId, userRole: userRole }
      }));
      
      // WebRTC 연결 시작 (이벤트 리스너 대신 직접 호출)
      if (typeof window.initWebRTC === 'function') {
        console.log("WebRTC 초기화 함수 호출");
        // 약간의 지연 후 초기화 (안정성을 위해)
        setTimeout(() => window.initWebRTC(), 500);
      }
    },
    function(error) {
      // 연결 실패
      console.error("WebSocket 연결 실패:", error);
      isConnecting = false;
      
      // 재연결 시도
      scheduleReconnect();
      
      // 오류 토스트 메시지 (첫 번째 시도에만 표시)
      if (reconnectAttempts === 1) {
        showToast("연결 실패", "실시간 통신 연결에 실패했습니다. 재연결을 시도합니다.", "error");
      }
    }
  );
}

// 메시지 수신을 위한 토픽 구독
function subscribeToTopics() {
  console.log("=== 웹소켓 구독 시작 ===");
  console.log("stompClient 연결 상태:", stompClient ? stompClient.connected : "null");
  console.log("세션 ID:", sessionId);
  console.log("사용자 역할:", userRole);
  console.log("========================");
  
  if (!stompClient || !stompClient.connected || !sessionId) {
    console.error("구독할 WebSocket 연결이 없습니다.");
    return;
  }
  
  console.log("메시지 토픽 구독 시작:", sessionId);
  
  // 전역 드로잉 이벤트 구독 (모든 방 공통)
  stompClient.subscribe('/topic/draw', function(message) {
    try {
      const drawData = JSON.parse(message.body);
      
      // 같은 세션 ID의 메시지만 처리 (세션 ID가 없으면 모든 메시지 처리)
      if (drawData.sessionId === sessionId || !drawData.sessionId) {
        console.log("전역 드로잉 데이터 수신:", drawData);
        
        // 데이터 유형에 따라 적절한 핸들러 호출
        if (drawData.type === 'highlight' || drawData.type === 'pen') {
          handleRemoteDrawing(drawData);
        }
      }
    } catch (e) {
      console.error("전역 드로잉 데이터 처리 오류:", e, message.body);
    }
  });
  
  // 전역 텍스트 이벤트 구독 (모든 방 공통)
  stompClient.subscribe('/topic/text', function(message) {
    try {
      const textData = JSON.parse(message.body);
      
      // 같은 세션 ID의 메시지만 처리 (세션 ID가 없으면 모든 메시지 처리)
      if (textData.sessionId === sessionId || !textData.sessionId) {
        console.log("전역 텍스트 데이터 수신:", textData);
        handleRemoteText(textData);
      }
    } catch (e) {
      console.error("전역 텍스트 데이터 처리 오류:", e, message.body);
    }
  });
  
  // 방 단위 드로잉 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/draw`, function(message) {
    try {
      const drawData = JSON.parse(message.body);
      console.log("방 단위 드로잉 데이터 수신:", drawData);
      
      // 데이터 유형에 따라 적절한 핸들러 호출
      if (drawData.type === 'highlight' || drawData.type === 'pen') {
        handleRemoteDrawing(drawData);
      } else if (drawData.type === 'text') {
        handleRemoteText(drawData);
      } else if (drawData.type === 'stamp') {
        handleRemoteStamp(drawData);
      } else if (drawData.type === 'signature') {
        handleRemoteSignature(drawData);
      }
    } catch (e) {
      console.error("방 단위 드로잉 데이터 처리 오류:", e);
    }
  });
  
  // 방 단위 텍스트 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/text`, function(message) {
    try {
      const textData = JSON.parse(message.body);
      console.log("방 단위 텍스트 데이터 수신:", textData);
      handleRemoteText(textData);
    } catch (e) {
      console.error("방 단위 텍스트 데이터 처리 오류:", e);
    }
  });
  
  // 도장 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/stamp`, function(message) {
    try {
      const stampData = JSON.parse(message.body);
      console.log("도장 데이터 수신:", { ...stampData, imageData: stampData.imageData ? '이미지 데이터 있음' : '이미지 데이터 없음' });
      
      // sender 정보 확인
      if (stampData.sender === userRole) {
        console.log("자신이 보낸 도장 데이터이므로 처리하지 않습니다.");
        return;
      }
      
      handleRemoteStamp(stampData);
    } catch (e) {
      console.error("도장 데이터 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  // 서명 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/signature`, function(message) {
    try {
      const signatureData = JSON.parse(message.body);
      console.log("서명 데이터 수신:", { ...signatureData, imageData: signatureData.imageData ? '이미지 데이터 있음' : '이미지 데이터 없음' });
      
      // sender 정보 확인
      if (signatureData.sender === userRole) {
        console.log("자신이 보낸 서명 데이터이므로 처리하지 않습니다.");
        return;
      }
      
      handleRemoteSignature(signatureData);
    } catch (e) {
      console.error("서명 데이터 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  // 페이지 동기화 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/page`, function(message) {
    try {
      const pageData = JSON.parse(message.body);
      console.log("페이지 동기화 데이터 수신:", pageData);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (pageData.sender !== userRole) {
        // 페이지 동기화 처리
        handlePageSync(pageData);
      }
    } catch (e) {
      console.error("페이지 동기화 데이터 처리 오류:", e);
    }
  });
  
  // PDF 업로드 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/pdf`, function(message) {
    try {
      const pdfData = JSON.parse(message.body);
      console.log("PDF 데이터 수신:", pdfData);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (pdfData.sender !== userRole) {
        // 이전 데이터 초기화 플래그 확인
        if (pdfData.clearPreviousData === true) {
          console.log("새 PDF 업로드로 인한 이전 데이터 초기화 시작");
          
          // 모든 이전 캔버스 데이터 초기화
          if (typeof drawingDataPerPage !== 'undefined') {
            drawingDataPerPage = {};
            console.log("원격 드로잉 데이터 초기화");
          }
          
          if (typeof textDataPerPage !== 'undefined') {
            textDataPerPage = {};
            console.log("원격 텍스트 데이터 초기화");
          }
          
          if (typeof stampDataPerPage !== 'undefined') {
            stampDataPerPage = {};
            console.log("원격 도장 데이터 초기화");
          }
          
          if (typeof signatureDataPerPage !== 'undefined') {
            signatureDataPerPage = {};
            console.log("원격 서명 데이터 초기화");
          }
          
          // 드로잉 캔버스가 있으면 초기화
          const drawingCanvas = document.getElementById('drawingCanvas');
          if (drawingCanvas) {
            const drawCtx = drawingCanvas.getContext('2d', { alpha: true });
            drawCtx.clearRect(0, 0, drawingCanvas.width, drawingCanvas.height);
            console.log("원격 드로잉 캔버스 초기화 완료");
          }
          
          console.log("원격 PDF 업로드로 인한 데이터 초기화 완료");
        }
        
        showToast("PDF 수신", "상대방이 PDF 파일을 업로드했습니다.", "info");
        uploadedPdfUrl = pdfData.pdfUrl;
        loadAndRenderPDF(uploadedPdfUrl);
      }
    } catch (e) {
      console.error("PDF 데이터 처리 오류:", e);
    }
  });
  
  // WebRTC 신호 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/rtc`, function(message) {
    try {
      const rtcData = JSON.parse(message.body);
      console.log("RTC 신호 수신:", rtcData.type);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (rtcData.sender !== userRole) {
        if (typeof window.handleRtcMessage === 'function') {
          window.handleRtcMessage(rtcData);
        } else {
          console.error("RTC 메시지 핸들러가 정의되지 않았습니다.");
        }
      }
    } catch (e) {
      console.error("RTC 신호 처리 오류:", e);
    }
  });

  // 스크롤 동기화 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/scroll`, function(message) {
    try {
      const scrollData = JSON.parse(message.body);
      console.log("스크롤 동기화 데이터 수신:", scrollData);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (scrollData.sender !== userRole) {
        if (typeof handleRemoteScrollSync === 'function') {
          handleRemoteScrollSync(scrollData.scrollData);
        } else {
          console.error("스크롤 동기화 핸들러가 정의되지 않았습니다.");
        }
      }
    } catch (e) {
      console.error("스크롤 동기화 데이터 처리 오류:", e);
    }
  });
  
  // 방 입장 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/join`, function(message) {
    try {
      const joinData = JSON.parse(message.body);
      console.log("방 입장 데이터 수신:", joinData);
      
      // 상대방이 입장한 경우
      if (joinData.role !== userRole) {
        if (joinData.role === "client") {
          showToast("고객 입장", "고객이 상담방에 입장했습니다.", "info");
        } else {
          showToast("상담원 입장", "상담원이 상담방에 입장했습니다.", "info");
        }
        
        // 방 입장 이벤트 발생
        document.dispatchEvent(new CustomEvent('roomJoinEvent', {
          detail: { role: joinData.role, sessionId: joinData.sessionId }
        }));
        
        // WebRTC 연결 시작 (이벤트 리스너 대신 직접 호출)
        if (typeof startCall === 'function') {
          setTimeout(() => {
            console.log("상대방 입장 감지 - startCall 직접 호출");
            startCall();
          }, 1000);
        }
      }
    } catch (e) {
      console.error("방 입장 데이터 처리 오류:", e);
    }
  });
  
  // 동기화 요청 구독
  stompClient.subscribe(`/topic/room/${sessionId}/sync`, function(message) {
    try {
      const syncData = JSON.parse(message.body);
      console.log("동기화 요청 수신:", syncData);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (syncData.sender !== userRole) {
        // 동기화 요청에 응답
        handleSyncRequest(syncData);
      }
    } catch (e) {
      console.error("동기화 요청 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  // 동기화 응답 구독
  stompClient.subscribe(`/topic/room/${sessionId}/sync_response`, function(message) {
    try {
      const syncResponse = JSON.parse(message.body);
      console.log("동기화 응답 수신:", syncResponse);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (syncResponse.sender !== userRole) {
        // 동기화 응답 처리
        handleSyncResponse(syncResponse);
      }
    } catch (e) {
      console.error("동기화 응답 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  // 전역 상담 종료 메시지 구독
  stompClient.subscribe('/topic/endConsult', function(message) {
    try {
      const endData = JSON.parse(message.body);
      console.log("상담 종료 메시지 수신:", endData);
      
      // 상담 종료 처리
      handleConsultationEnd(endData);
    } catch (e) {
      console.error("상담 종료 메시지 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  // 세션별 상담 종료 메시지 구독
  stompClient.subscribe(`/topic/room/${sessionId}/endConsult`, function(message) {
    try {
      const endData = JSON.parse(message.body);
      console.log("세션별 상담 종료 메시지 수신:", endData);
      
      // 상담 종료 처리
      handleConsultationEnd(endData);
    } catch (e) {
      console.error("세션별 상담 종료 메시지 처리 오류:", e);
      console.error("원본 메시지:", message.body);
    }
  });
  
  console.log("모든 토픽 구독 완료");
}

// WebSocket 재연결 예약
function scheduleReconnect() {
  // 이미 재연결 타이머가 있으면 취소
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
  }
  
  // 최대 시도 횟수 제한 (10회)
  if (reconnectAttempts >= 10) {
    console.error("최대 재연결 시도 횟수에 도달했습니다. 재연결을 중단합니다.");
    showToast("연결 실패", "서버 연결에 실패했습니다. 페이지를 새로고침해주세요.", "error");
    return;
  }
  
  // 재연결 시도 횟수 증가
  reconnectAttempts++;
  
  // 지수 백오프 적용 (최대 30초)
  const delay = Math.min(1000 * Math.pow(1.5, reconnectAttempts), 30000);
  
  console.log(`${delay / 1000}초 후에 WebSocket 재연결 시도 (${reconnectAttempts}번째 시도)`);
  
  // 재연결 타이머 설정
  reconnectTimer = setTimeout(() => {
    initializeWebSocket();
  }, delay);
}

// WebSocket 연결 상태 확인 및 재연결
function checkAndReconnectWebSocket() {
  // 세션 ID 확인
  const validSessionId = ensureValidSessionId();
  if (!validSessionId) {
    console.error("유효한 세션 ID를 찾을 수 없어 WebSocket 연결을 시도할 수 없습니다.");
    showToast("연결 오류", "세션 ID를 찾을 수 없습니다.", "error");
    return;
  }
  
  // 이미 연결되어 있으면 상태 확인
  if (stompClient && stompClient.connected) {
    console.log("WebSocket이 이미 연결되어 있습니다.");
    return;
  }
  
  // 연결되어 있지 않으면 연결 시도
  console.log("WebSocket이 연결되어 있지 않습니다. 연결을 시도합니다.");
  initializeWebSocket();
}

// RTC 메시지 전송 함수 - webRTC.js의 sendRtcMessage()에서 사용됨
function sendRtcMessage(type, data = {}) {
  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket 연결이 없어 RTC 메시지를 전송할 수 없습니다.");
    return;
  }
  
  // 메시지 형식 통일
  const message = {
    type: type,
    sender: userRole,
    roomId: sessionId,
    timestamp: Date.now(),
    ...data
  };
  
  console.log(`RTC 메시지 전송 (${type})`);
  
  // 올바른 토픽 경로로 메시지 전송
  try {
    stompClient.send(`/app/room/${sessionId}/rtc`, {}, JSON.stringify(message));
  } catch (e) {
    console.error("RTC 메시지 전송 오류:", e);
  }
}

// 채팅 메시지 처리 함수
function handleChatMessage(chatData) {
  // 채팅 기능이 있다면 여기서 처리
  console.log("채팅 메시지:", chatData.message);
  
  // 토스트 메시지로 채팅 표시
  showToast("메시지 수신", chatData.message, "info");
}

// 상담 종료 메시지 처리 함수
function handleConsultationEnd(endData) {
  console.log("=== 상담 종료 처리 시작 ===");
  console.log("종료 데이터:", endData);
  
  // 고객인 경우 상담원과 동일한 스타일의 모달 표시
  if (userRole === 'client') {
    showClientConsultationEndModal();
  } else {
    // 상담원인 경우 기존 모달 표시
    showConsultationEndModal(endData.message || "상담이 종료되었습니다.", endData.redirectUrl || "/");
  }
}

// 상담 종료 모달 표시 함수
function showConsultationEndModal(message, redirectUrl) {
  // 기존 모달이 있으면 제거
  const existingModal = document.getElementById('consultationEndModal');
  if (existingModal) {
    existingModal.remove();
  }
  
  // 모달 HTML 생성
  const modalHTML = `
    <div id="consultationEndModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <i class="fas fa-check-circle modal-icon"></i>
          <h3 class="modal-title">상담 종료</h3>
        </div>
        <p class="modal-message">${message}</p>
        <div class="modal-buttons">
          <button onclick="confirmConsultationEnd('${redirectUrl}')" class="btn-confirm">
            <i class="fas fa-home"></i> 메인 페이지로 이동
          </button>
        </div>
      </div>
    </div>
  `;
  
  // 모달을 body에 추가
  document.body.insertAdjacentHTML('beforeend', modalHTML);
  
  // 모달 스타일 추가 (기존 스타일이 없는 경우)
  if (!document.getElementById('consultationEndModalStyles')) {
    const styles = `
      <style id="consultationEndModalStyles">
        #consultationEndModal .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;
          background-color: rgba(0, 0, 0, 0.7);
          display: flex;
          justify-content: center;
          align-items: center;
          z-index: 99999;
        }
        
        #consultationEndModal .modal-content {
          background: white;
          padding: 30px;
          border-radius: 12px;
          box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
          animation: modalFadeIn 0.3s ease-out;
          position: relative;
          margin: auto;
          max-width: 400px;
          width: 90%;
        }
        
        @keyframes modalFadeIn {
          from {
            opacity: 0;
            transform: scale(0.9) translateY(-20px);
          }
          to {
            opacity: 1;
            transform: scale(1) translateY(0);
          }
        }
        
        #consultationEndModal .btn-confirm {
          background-color: #0057d7;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: background-color 0.2s;
        }
        
        #consultationEndModal .btn-confirm:hover {
          background-color: #004bb5;
        }
        
        #consultationEndModal .modal-header {
          text-align: center;
          margin-bottom: 20px;
        }
        
        #consultationEndModal .modal-icon {
          font-size: 48px;
          color: #0057d7;
          margin-bottom: 15px;
          display: block;
        }
        
        #consultationEndModal .modal-title {
          margin: 0;
          color: #333;
          font-size: 20px;
          font-weight: bold;
        }
        
        #consultationEndModal .modal-message {
          margin: 20px 0;
          font-size: 16px;
          line-height: 1.5;
          text-align: center;
          color: #555;
        }
        
        #consultationEndModal .btn-confirm {
          width: 100%;
          padding: 12px;
          font-size: 16px;
          font-weight: bold;
        }
      </style>
    `;
    document.head.insertAdjacentHTML('beforeend', styles);
  }
  
  console.log("상담 종료 모달 표시 완료");
}

// 고객용 상담 종료 모달 표시 함수 (상담원과 동일한 스타일)
function showClientConsultationEndModal() {
  // 기존 모달이 있으면 제거
  const existingModal = document.getElementById('clientEndConsultationModal');
  if (existingModal) {
    existingModal.remove();
  }
  
  // 모달 HTML 생성 (상담원과 동일한 구조) - 인라인 스타일로 강제 적용
  const modalHTML = `
    <div id="clientEndConsultationModal" class="modal-overlay" style="position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; background-color: rgba(0, 0, 0, 0.7) !important; display: flex !important; justify-content: center !important; align-items: center !important; z-index: 999999 !important; margin: 0 !important; padding: 0 !important;">
      <div class="modal-content" style="background: white !important; padding: 30px !important; border-radius: 12px !important; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3) !important; max-width: 500px !important; width: 90% !important; position: relative !important; margin: 0 auto !important; transform: none !important;">
        <h3 style="margin-top: 0 !important; margin-bottom: 20px !important; color: #333 !important; border-bottom: 2px solid #0057d7 !important; padding-bottom: 10px !important; text-align: center !important; font-size: 20px !important; font-weight: bold !important;"><i class="fas fa-clipboard-check"></i> 상담 종료</h3>
        <p style="margin: 15px 0 !important; font-size: 16px !important; line-height: 1.5 !important; text-align: center !important; color: #555 !important;">상담이 종료되었습니다.</p>
        <p style="margin: 15px 0 !important; font-size: 16px !important; line-height: 1.5 !important; text-align: center !important; color: #555 !important;">메인 페이지로 이동합니다.</p>
        <div class="modal-buttons" style="display: flex !important; gap: 10px !important; justify-content: center !important; margin-top: 25px !important;">
          <button onclick="confirmClientConsultationEnd()" class="btn-confirm" style="padding: 12px 24px !important; border: none !important; border-radius: 6px !important; cursor: pointer !important; font-size: 16px !important; font-weight: bold !important; background-color: #0057d7 !important; color: white !important; min-width: 200px !important;">
            <i class="fas fa-home"></i> 메인 페이지로 이동
          </button>
        </div>
      </div>
    </div>
  `;
  
  // 모달을 body에 추가
  document.body.insertAdjacentHTML('beforeend', modalHTML);
  
  // 모달 스타일 추가 (main.js의 스타일과 동일)
  addClientModalStyles();
  
  console.log("고객용 상담 종료 모달 표시 완료");
}

// 고객용 모달 스타일 추가 (main.js와 동일한 스타일)
function addClientModalStyles() {
  // 이미 스타일이 추가되어 있는지 확인
  if (document.getElementById('clientModalStyles')) {
    return;
  }
  
  const style = document.createElement('style');
  style.id = 'clientModalStyles';
  style.textContent = `
    /* 고객용 상담 종료 모달 - 최고 우선순위 */
    div#clientEndConsultationModal.modal-overlay {
      position: fixed !important;
      top: 0 !important;
      left: 0 !important;
      width: 100vw !important;
      height: 100vh !important;
      background-color: rgba(0, 0, 0, 0.7) !important;
      display: flex !important;
      justify-content: center !important;
      align-items: center !important;
      z-index: 999999 !important;
      margin: 0 !important;
      padding: 0 !important;
    }
    
    div#clientEndConsultationModal .modal-content {
      background: white !important;
      padding: 30px !important;
      border-radius: 12px !important;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3) !important;
      max-width: 500px !important;
      width: 90% !important;
      max-height: 80vh !important;
      overflow-y: auto !important;
      position: relative !important;
      margin: 0 auto !important;
      transform: none !important;
      animation: clientModalFadeIn 0.3s ease-out !important;
    }
    
    @keyframes clientModalFadeIn {
      from {
        opacity: 0;
        transform: scale(0.9) translateY(-20px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }
    
    div#clientEndConsultationModal .modal-content h3 {
      margin-top: 0 !important;
      margin-bottom: 20px !important;
      color: #333 !important;
      border-bottom: 2px solid #0057d7 !important;
      padding-bottom: 10px !important;
      text-align: center !important;
      font-size: 20px !important;
      font-weight: bold !important;
    }
    
    div#clientEndConsultationModal .modal-content p {
      margin: 15px 0 !important;
      font-size: 16px !important;
      line-height: 1.5 !important;
      text-align: center !important;
      color: #555 !important;
    }
    
    div#clientEndConsultationModal .modal-buttons {
      display: flex !important;
      gap: 10px !important;
      justify-content: center !important;
      margin-top: 25px !important;
    }
    
    div#clientEndConsultationModal .btn-confirm {
      padding: 12px 24px !important;
      border: none !important;
      border-radius: 6px !important;
      cursor: pointer !important;
      font-size: 16px !important;
      font-weight: bold !important;
      transition: background-color 0.2s !important;
      background-color: #0057d7 !important;
      color: white !important;
      min-width: 200px !important;
    }
    
    div#clientEndConsultationModal .btn-confirm:hover {
      background-color: #004bb5 !important;
    }
    
    div#clientEndConsultationModal .btn-confirm i {
      margin-right: 8px !important;
    }
  `;
  
  document.head.appendChild(style);
}

// 고객용 상담 종료 확인 함수
function confirmClientConsultationEnd() {
  console.log("고객 상담 종료 확인");
  
  // 모달 제거
  const modal = document.getElementById('clientEndConsultationModal');
  if (modal) {
    modal.remove();
  }
  
  // WebSocket 연결 종료
  if (stompClient && stompClient.connected) {
    try {
      stompClient.disconnect(() => {
        console.log("WebSocket 연결 종료 완료");
      });
    } catch (e) {
      console.error("WebSocket 연결 종료 오류:", e);
    }
  }
  
  // WebRTC 연결 종료
  if (typeof cleanupWebRTC === 'function') {
    try {
      cleanupWebRTC();
      console.log("WebRTC 연결 정리 완료");
    } catch (e) {
      console.error("WebRTC 연결 정리 오류:", e);
    }
  }
  
  // 페이지 이동
  setTimeout(() => {
    window.location.href = "/";
  }, 500);
}

// 상담 종료 확인 함수
function confirmConsultationEnd(redirectUrl) {
  console.log("상담 종료 확인 - 리다이렉트 URL:", redirectUrl);
  
  // 모달 제거
  const modal = document.getElementById('consultationEndModal');
  if (modal) {
    modal.remove();
  }
  
  // WebSocket 연결 종료
  if (stompClient && stompClient.connected) {
    try {
      stompClient.disconnect(() => {
        console.log("WebSocket 연결 종료 완료");
      });
    } catch (e) {
      console.error("WebSocket 연결 종료 오류:", e);
    }
  }
  
  // WebRTC 연결 종료
  if (typeof cleanupWebRTC === 'function') {
    try {
      cleanupWebRTC();
      console.log("WebRTC 연결 정리 완료");
    } catch (e) {
      console.error("WebRTC 연결 정리 오류:", e);
    }
  }
  
  // 페이지 이동
  setTimeout(() => {
    window.location.href = redirectUrl;
  }, 500);
}

// 페이지 접속 시 WebSocket 연결 초기화
document.addEventListener('DOMContentLoaded', function() {
  // DOM이 로드된 후 실행
  setTimeout(() => {
    checkAndReconnectWebSocket();
  }, 1000);
});

// 페이지 동기화 처리 함수
function handlePageSync(pageData) {
  if (!pageData || !pageData.page) {
    console.error("유효하지 않은 페이지 동기화 데이터:", pageData);
    return;
  }
  
  console.log(`페이지 동기화 요청 수신: ${currentPage} → ${pageData.page}`);
  
  // 현재 페이지와 같으면 무시
  if (pageData.page === currentPage) {
    console.log("이미 같은 페이지에 있어 동기화가 필요하지 않습니다.");
    return;
  }
  
  // 현재 작업 중인 데이터 저장
  saveDrawingData();
  saveTextData();
  saveStampData();
  saveSignatureData();
  
  // 타겟 페이지로 이동
  if (typeof renderPage === 'function') {
    // 페이지 렌더링 함수가 있으면 직접 호출
    renderPage(pageData.page);
    
    // 도장 및 서명 데이터 동기화 요청
    if (stompClient && stompClient.connected && userRole === 'agent') {
      // 상담원인 경우에만 동기화 데이터 요청
      requestSyncStampAndSignature(pageData.page);
    }
    
    // 성공 메시지
    showToast("페이지 동기화", `${pageData.page}페이지로 이동했습니다.`, "info");
    console.log(`페이지 동기화 완료: ${pageData.page}페이지`);
  } else {
    console.error("페이지 렌더링 함수를 찾을 수 없습니다.");
  }
}

// 도장 및 서명 데이터 동기화 요청
function requestSyncStampAndSignature(page) {
  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket 연결이 없어 동기화 요청을 전송할 수 없습니다.");
    return;
  }
  
  try {
    // 동기화 요청 메시지 구성
    const message = {
      type: 'sync_request',
      page: page,
      sessionId: sessionId,
      sender: userRole,
      timestamp: Date.now()
    };
    
    // 동기화 요청 전송
    stompClient.send(`/app/room/${sessionId}/sync`, {
      contentType: 'application/json',
      sessionId: sessionId,
      userRole: userRole
    }, JSON.stringify(message));
    console.log("도장 및 서명 데이터 동기화 요청 전송:", message);
  } catch (e) {
    console.error("동기화 요청 전송 오류:", e);
  }
}

// 페이지 변경 이벤트 전송 함수
function sendPageSync(targetPage) {
  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket 연결이 없어 페이지 동기화를 전송할 수 없습니다.");
    return;
  }
  
  try {
    // 메시지 구조 생성
    const message = {
      type: 'page_sync',
      page: targetPage,
      sender: userRole,
      sessionId: sessionId,
      timestamp: Date.now()
    };
    
    // 메시지 전송
    stompClient.send(`/topic/room/${sessionId}/page`, {}, JSON.stringify(message));
    console.log("페이지 동기화 메시지 전송 완료:", message);
  } catch (e) {
    console.error("페이지 동기화 메시지 전송 오류:", e);
  }
}

// 동기화 요청 처리
function handleSyncRequest(syncData) {
  if (syncData.type !== 'sync_request') return;
  
  console.log("동기화 요청 처리 시작");
  
  // 현재 페이지의 도장 및 서명 데이터 전송
  if (stompClient && stompClient.connected) {
    try {
      // 요청된 페이지의 도장 및 서명 데이터 수집
      const stampData = stampDataPerPage[syncData.page] || [];
      const signatureData = signatureDataPerPage[syncData.page] || [];
      
      // 응답 메시지 구성
      const syncResponse = {
        type: 'sync_response',
        page: syncData.page,
        stampData: stampData,
        signatureData: signatureData,
        sessionId: sessionId,
        sender: userRole,
        timestamp: Date.now()
      };
      
      // 동기화 응답 전송
      stompClient.send(`/app/room/${sessionId}/sync_response`, {
        contentType: 'application/json',
        sessionId: sessionId,
        userRole: userRole
      }, JSON.stringify(syncResponse));
      console.log("동기화 응답 전송:", syncResponse);
    } catch (e) {
      console.error("동기화 응답 전송 오류:", e);
    }
  } else {
    console.error("WebSocket 연결이 없어 동기화 응답을 전송할 수 없습니다.");
  }
}

// 동기화 응답 처리
function handleSyncResponse(syncResponse) {
  if (syncResponse.type !== 'sync_response') return;
  
  console.log("동기화 응답 처리 시작");
  
  // 응답이 현재 페이지에 관한 것인지 확인
  if (syncResponse.page === currentPage) {
    // 도장 데이터 처리
    if (syncResponse.stampData && Array.isArray(syncResponse.stampData)) {
      syncResponse.stampData.forEach(stampData => {
        // 이미 있는 데이터인지 확인 (중복 방지)
        const isDuplicate = stampDataPerPage[currentPage] && 
                          stampDataPerPage[currentPage].some(item => 
                          item.x === stampData.x && 
                          item.y === stampData.y &&
                          item.width === stampData.width &&
                          item.height === stampData.height);
        
        if (!isDuplicate) {
          // 도장 데이터 저장 및 그리기
          if (!stampDataPerPage[currentPage]) {
            stampDataPerPage[currentPage] = [];
          }
          stampDataPerPage[currentPage].push(stampData);
          
          // 도장 이미지 그리기
          const stampImage = new Image();
          if (stampData.imageData) {
            stampImage.src = stampData.imageData;
          } else {
            stampImage.src = '/images/stamp.png';
          }
          
          stampImage.onload = function() {
            drawingContext.drawImage(stampImage, stampData.x, stampData.y, stampData.width, stampData.height);
            console.log(`동기화된 도장 이미지 그리기 완료: (${stampData.x}, ${stampData.y})`);
          };
        }
      });
    }
    
    // 서명 데이터 처리
    if (syncResponse.signatureData && Array.isArray(syncResponse.signatureData)) {
      syncResponse.signatureData.forEach(signatureData => {
        // 이미 있는 데이터인지 확인 (중복 방지)
        const isDuplicate = signatureDataPerPage[currentPage] && 
                          signatureDataPerPage[currentPage].some(item => 
                          item.x === signatureData.x && 
                          item.y === signatureData.y &&
                          item.width === signatureData.width &&
                          item.height === signatureData.height);
        
        if (!isDuplicate) {
          // 서명 데이터 저장 및 그리기
          if (!signatureDataPerPage[currentPage]) {
            signatureDataPerPage[currentPage] = [];
          }
          signatureDataPerPage[currentPage].push(signatureData);
          
          // 서명 이미지 그리기
          if (signatureData.imageData) {
            const signatureImage = new Image();
            signatureImage.src = signatureData.imageData;
            
            signatureImage.onload = function() {
              drawingContext.drawImage(signatureImage, signatureData.x, signatureData.y, 
                                      signatureData.width || 200, signatureData.height || 100);
              console.log(`동기화된 서명 이미지 그리기 완료: (${signatureData.x}, ${signatureData.y})`);
            };
          } else {
            // 이미지 데이터가 없는 경우 기본 서명 그리기
            drawSignature(signatureData.x, signatureData.y);
          }
        }
      });
    }
    
    // 세션 데이터 저장
    saveSessionData();
    console.log("동기화 데이터 처리 완료");
  } else {
    console.log(`동기화 응답 페이지(${syncResponse.page})와 현재 페이지(${currentPage})가 다릅니다.`);
  }
}

// PDF에 도장과 서명 포함해서 저장하는 함수
async function savePdfWithStampAndSignature(forEmail = false) {
  try {
    console.log("PDF 라이브러리 확인 중...");
    
    // PDF 라이브러리 로드 대기
    await checkPdfLibraryLoaded();
    
    const PDFDocument = window.PDFLib.PDFDocument;
    console.log("PDF 라이브러리 로드 확인됨");

    if (!uploadedPdfUrl) {
      console.error("PDF 파일 경로가 없습니다. uploadedPdfUrl:", uploadedPdfUrl);
      throw new Error("PDF 파일 경로를 찾을 수 없습니다. PDF를 먼저 업로드해주세요.");
    }
    
    console.log("PDF 생성 시작 - PDF URL:", uploadedPdfUrl);

    // 로딩 메시지 표시
    if (!forEmail) {
      alert("PDF 저장 중입니다. 잠시만 기다려주세요...");
    } else {
      showToast("PDF 생성 중", "PDF 문서를 생성하고 있습니다...", "info");
    }

    // 원본 PDF 가져오기
    console.log("PDF 파일 다운로드 시작:", uploadedPdfUrl);
    const pdfResponse = await fetch(uploadedPdfUrl);
    
    if (!pdfResponse.ok) {
      console.error("PDF 파일 다운로드 실패:", pdfResponse.status, pdfResponse.statusText);
      throw new Error(`PDF 파일을 가져올 수 없습니다. (${pdfResponse.status})`);
    }
    
    const existingPdfBytes = await pdfResponse.arrayBuffer();
    console.log("PDF 파일 다운로드 완료, 크기:", existingPdfBytes.byteLength, "bytes");
    
    console.log("PDF 문서 로드 시작...");
    const pdfDocLib = await PDFDocument.load(existingPdfBytes);
    console.log("PDF 문서 로드 완료");
    const pages = pdfDocLib.getPages();

    // 페이지별로 도장과 서명 데이터 처리
    for (let i = 0; i < pages.length; i++) {
      const pageNumber = i + 1;
      const page = pages[i];
      const { width, height } = page.getSize();

      // 이 페이지에 있는 도장 목록
      const stampItems = stampDataPerPage[pageNumber] || [];
      console.log(`페이지 ${pageNumber}의 도장 개수: ${stampItems.length}`);

      // 이 페이지에 있는 서명 목록
      const signatureItems = signatureDataPerPage[pageNumber] || [];
      console.log(`페이지 ${pageNumber}의 서명 개수: ${signatureItems.length}`);

      // 도장 추가
      for (const stampItem of stampItems) {
        try {
          // 이미지 데이터 가져오기 (이미지 또는 imageData 필드 둘 다 지원)
          const imageData = stampItem.imageData || stampItem.image;
          if (!imageData) continue;
          
          // Base64 데이터만 추출 (data:image/png;base64, 부분 제거)
          const base64Data = imageData.split(',')[1];
          if (!base64Data) continue;
          
          // Base64 데이터를 바이너리로 변환
          const binaryData = atob(base64Data);
          const bytes = new Uint8Array(binaryData.length);
          for (let j = 0; j < binaryData.length; j++) {
            bytes[j] = binaryData.charCodeAt(j);
          }
          
          // 이미지를 PDF에 임베드
          const stampImage = await pdfDocLib.embedPng(bytes);
          
          // 좌표 변환 (PDF 좌표계는 왼쪽 하단이 원점)
          const scale = 1.5; // PDF.js의 renderPage에서 사용된 스케일과 맞춤
          const pdfX = (stampItem.x / scale);
          const pdfY = height - (stampItem.y / scale);
          
          // 도장 크기
          const stampSize = stampItem.width || 50;
          
          // 도장 추가
          page.drawImage(stampImage, {
            x: pdfX,
            y: pdfY - stampSize,
            width: stampSize,
            height: stampSize
          });
        } catch (err) {
          console.error("도장 추가 중 오류:", err);
        }
      }

      // 서명 추가
      for (const signItem of signatureItems) {
        try {
          // 이미지 데이터 가져오기 (이미지 또는 imageData 필드 둘 다 지원)
          const imageData = signItem.imageData || signItem.image;
          if (!imageData) continue;
          
          // Base64 데이터만 추출 (data:image/png;base64, 부분 제거)
          const base64Data = imageData.split(',')[1];
          if (!base64Data) continue;
          
          // Base64 데이터를 바이너리로 변환
          const binaryData = atob(base64Data);
          const bytes = new Uint8Array(binaryData.length);
          for (let j = 0; j < binaryData.length; j++) {
            bytes[j] = binaryData.charCodeAt(j);
          }
          
          // 이미지를 PDF에 임베드
          const signImage = await pdfDocLib.embedPng(bytes);
          
          // 좌표 변환
          const scale = 1.5;
          const pdfX = (signItem.x / scale);
          const pdfY = height - (signItem.y / scale);
          
          // 서명 크기
          const signWidth = signItem.width || 100;
          const signHeight = signItem.height || 50;
          
          // 서명 추가
          page.drawImage(signImage, {
            x: pdfX,
            y: pdfY - signHeight,
            width: signWidth,
            height: signHeight
          });
        } catch (err) {
          console.error("서명 추가 중 오류:", err);
        }
      }
    }

    // PDF 저장
    const pdfBytes = await pdfDocLib.save();
    const blob = new Blob([pdfBytes], { type: "application/pdf" });
    
    // 현재 날짜와 시간을 파일명에 추가
    const now = new Date();
    const timestamp = `${now.getFullYear()}${(now.getMonth()+1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}_${now.getHours().toString().padStart(2, '0')}${now.getMinutes().toString().padStart(2, '0')}`;
    const fileName = `상담문서_${timestamp}.pdf`;
    
    // 로컬 다운로드 (이메일 전송이 아닌 경우)
    if (!forEmail) {
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = fileName;
      link.click();
      console.log("PDF 저장 완료");
      return null;
    } else {
      // 이메일 전송을 위해 Base64 인코딩된 데이터 반환
      console.log("이메일 전송용 PDF 생성 완료");
      return await blobToBase64(blob);
    }
  } catch (error) {
    console.error("PDF 저장 오류:", error);
    if (!forEmail) {
      alert("PDF 저장 중 오류가 발생했습니다: " + error.message);
    } else {
      showToast("PDF 생성 실패", "PDF 저장 중 오류가 발생했습니다: " + error.message, "error");
    }
    throw error;
  }
}

// Blob을 Base64로 변환하는 함수
function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

// PDF 라이브러리 로드 상태 확인 함수
function checkPdfLibraryLoaded() {
  return new Promise((resolve, reject) => {
    // 이미 로드되어 있으면 바로 resolve
    if (window.PDFLib && window.PDFLib.PDFDocument) {
      console.log("PDF 라이브러리가 이미 로드되어 있습니다.");
      resolve(true);
      return;
    }
    
    // 최대 10초 동안 대기
    let attempts = 0;
    const maxAttempts = 100; // 100ms * 100 = 10초
    
    const checkInterval = setInterval(() => {
      attempts++;
      
      if (window.PDFLib && window.PDFLib.PDFDocument) {
        console.log(`PDF 라이브러리 로드 완료 (${attempts * 100}ms 후)`);
        clearInterval(checkInterval);
        resolve(true);
      } else if (attempts >= maxAttempts) {
        console.error("PDF 라이브러리 로드 타임아웃");
        clearInterval(checkInterval);
        reject(new Error("PDF 라이브러리 로드 타임아웃"));
      }
    }, 100);
  });
}

// PDF 저장 버튼 추가 (UI 초기화 시 호출) - 상담원만
document.addEventListener('DOMContentLoaded', function() {
  setTimeout(addPdfSaveButton, 1000); // 1초 후 버튼 추가 (다른 UI 초기화 이후)
}); 