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
  if (!stompClient || !stompClient.connected || !sessionId) {
    console.error("구독할 WebSocket 연결이 없습니다.");
    return;
  }
  
  console.log("메시지 토픽 구독 시작:", sessionId);
  
  // 드로잉 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/draw`, function(message) {
    try {
      const drawData = JSON.parse(message.body);
      console.log("드로잉 데이터 수신:", drawData);
      
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
      console.error("드로잉 데이터 처리 오류:", e);
    }
  });
  
  // 텍스트 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/text`, function(message) {
    try {
      const textData = JSON.parse(message.body);
      console.log("텍스트 데이터 수신:", textData);
      handleRemoteText(textData);
    } catch (e) {
      console.error("텍스트 데이터 처리 오류:", e);
    }
  });
  
  // 도장 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/stamp`, function(message) {
    try {
      const stampData = JSON.parse(message.body);
      console.log("도장 데이터 수신:", stampData);
      handleRemoteStamp(stampData);
    } catch (e) {
      console.error("도장 데이터 처리 오류:", e);
    }
  });
  
  // 서명 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/signature`, function(message) {
    try {
      const signatureData = JSON.parse(message.body);
      console.log("서명 데이터 수신:", signatureData);
      handleRemoteSignature(signatureData);
    } catch (e) {
      console.error("서명 데이터 처리 오류:", e);
    }
  });
  
  // PDF 업로드 이벤트 구독
  stompClient.subscribe(`/topic/room/${sessionId}/pdf`, function(message) {
    try {
      const pdfData = JSON.parse(message.body);
      console.log("PDF 데이터 수신:", pdfData);
      
      // 본인이 보낸 메시지가 아닌 경우에만 처리
      if (pdfData.sender !== userRole) {
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

// 페이지 접속 시 WebSocket 연결 초기화
document.addEventListener('DOMContentLoaded', function() {
  // DOM이 로드된 후 실행
  setTimeout(() => {
    checkAndReconnectWebSocket();
  }, 1000);
}); 