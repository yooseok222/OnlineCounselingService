// 전역 변수 선언 (HTML 헤드에 선언된 변수들 주석 처리)
// let drawingDataPerPage = {};
// let stampDataPerPage = {};
// let textDataPerPage = {};
// let signatureDataPerPage = {};

// let pdfDoc = null;
// let currentPage = 1;
// let renderTask = null;
// let stompClient = null;
// let mode = null; // 'pen' | 'highlight' | null
// let drawing = false;
// let pendingText = null;
// let uploadedPdfUrl = null;
// let userRole = null; // 'agent' | 'client'
// let sessionId = null; // 상담 세션 ID 추가

// 페이지 로드 시 사용자 역할 확인 및 UI 초기화
window.onload = function() {
  console.log("윈도우 로드됨 - 초기화 시작");

  // 새로고침 방지 이벤트 리스너 추가 - 최상위 우선순위로 설정
  const preventRefreshHandler = function(e) {
    // 브라우저마다 표시되는 메시지가 다를 수 있음
    var confirmationMessage = '상담이 진행 중입니다. 페이지를 떠나면 상담이 종료됩니다. 정말 나가시겠습니까?';

    e.preventDefault(); // 기본 동작 방지 (일부 브라우저에서 효과)
    e.returnValue = confirmationMessage;  // 표준
    return confirmationMessage;           // 일부 브라우저용
  };

  // 이벤트 리스너 등록 (캡처 단계에서 실행)
  window.addEventListener('beforeunload', preventRefreshHandler, true);

  // 글로벌 초기화 플래그 설정 (최초 1회만 실행되는 로직 제어용)
  window.isInitialLoad = true;
  window.dataLoaded = false;

  // URL에서 역할 파라미터 확인
  const urlParams = new URLSearchParams(window.location.search);
  const roleParam = urlParams.get('role');
  const sessionParam = urlParams.get('session'); // 세션 ID 파라미터 확인

  // URL 파라미터에 역할이 있으면 세션 스토리지에 저장 및 전역변수 설정
  if (roleParam) {
    userRole = roleParam;
    sessionStorage.setItem("role", roleParam);
    console.log("URL에서 사용자 역할 설정:", userRole);
  } else if (sessionStorage.getItem("role")) {
    userRole = sessionStorage.getItem("role");
    console.log("세션 스토리지에서 사용자 역할 설정:", userRole);
  } else {
    console.error("역할 정보를 찾을 수 없습니다.");
    alert("역할 정보가 필요합니다. 올바른 URL로 접속해주세요.");
    location.href = "/";
    return;
  }

  // 세션 ID 설정 (URL에서 가져오거나 새로 생성)
  if (sessionParam) {
    // URL에 세션 ID가 있는 경우 절대적으로 우선시
    sessionId = sessionParam;
    sessionStorage.setItem("sessionId", sessionId);
    console.log("URL에서 세션 ID 로드 (최우선):", sessionId);
    
    // URL 파라미터에 세션 ID가 없거나 다른 경우 강제로 URL 업데이트
    const url = new URL(window.location.href);
    url.searchParams.set('session', sessionId);
    window.history.replaceState({}, '', url);
  } else if (userRole === "agent") {
    // 상담원인 경우에만 새 세션 ID 생성
    sessionId = generateSessionId();
    console.log("상담원: 새 세션 ID 생성:", sessionId);
    
    // URL에 세션 ID 추가 (페이지 이동 없이 URL 업데이트)
    const url = new URL(window.location.href);
    url.searchParams.set('session', sessionId);
    window.history.replaceState({}, '', url);
    
    // 세션 ID를 세션 스토리지에 저장
    sessionStorage.setItem("sessionId", sessionId);
    
    // 고객 URL 생성 및 표시
    const clientUrl = new URL(window.location.href);
    clientUrl.searchParams.set('role', 'client');
    const clientUrlString = clientUrl.toString();
    
    // URL을 상담원에게 표시 (복사 가능하게)
    setTimeout(() => {
      showToast("고객 접속 URL", "고객에게 다음 URL을 공유하세요", "info", 10000);
      // URL을 복사 가능한 형태로 표시하는 모달 또는 요소 추가
      const urlDisplayDiv = document.createElement('div');
      urlDisplayDiv.className = 'client-url-display';
      urlDisplayDiv.innerHTML = `
        <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0; position: relative;">
          <h4>고객 접속 URL</h4>
          <div style="display: flex;">
            <input type="text" value="${clientUrlString}" 
                   style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;" 
                   readonly id="clientUrlInput">
            <button onclick="copyClientUrl()" 
                    style="margin-left: 5px; padding: 8px 12px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              복사
            </button>
          </div>
        </div>
      `;
      
      // 상담원 컨트롤 영역에 URL 표시 요소 추가
      const controlArea = document.querySelector('.controls') || document.querySelector('.toolbar');
      if (controlArea) {
        controlArea.appendChild(urlDisplayDiv);
      } else {
        document.body.insertBefore(urlDisplayDiv, document.body.firstChild);
      }
      
      // URL 복사 함수 추가
      window.copyClientUrl = function() {
        const urlInput = document.getElementById('clientUrlInput');
        urlInput.select();
        document.execCommand('copy');
        showToast("복사 완료", "고객 URL이 클립보드에 복사되었습니다.", "success");
      };
    }, 2000);
  } else {
    // 고객이면서 세션 ID가 없는 경우 오류 처리
    alert("유효한 상담 세션이 아닙니다. 상담원이 제공한 URL로 접속해주세요.");
    location.href = "/";
    return;
  }

  // 세션 ID 확인 로그
  console.log("최종 설정된 세션 ID:", sessionId);
  console.log("최종 설정된 사용자 역할:", userRole);

  // 사용자 역할 표시
  const userRoleDisplay = document.getElementById('userRoleDisplay');
  if (userRoleDisplay) {
    const roleName = userRole === 'agent' ? '상담원' : '고객';
    const roleIcon = userRole === 'agent' ? '<i class="fas fa-headset"></i>' : '<i class="fas fa-user"></i>';
    userRoleDisplay.innerHTML = `${roleIcon} ${roleName}`;
  }

  // 새로고침 시 이전 페이지 번호를 복원하기 위해 저장
  if (sessionStorage.getItem("lastPage")) {
    currentPage = parseInt(sessionStorage.getItem("lastPage")) || 1;
    console.log("저장된 페이지 번호 복원:", currentPage);
  }

  // 역할에 따라 UI 초기화
  initializeUIByRole();

  // 상담원이 상담실에 입장하면 입장 상태를 자동으로 설정
  if (userRole === "agent") {
    // 상담원 입장 상태 변경
    updateAgentStatus(true);

    // 토스트 메시지 표시
    setTimeout(() => {
      showToast("상담 시작", "고객이 상담실로 입장할 수 있습니다.", "info");
    }, 1000); // 1초 후 표시
  }

  // 고객인 경우에는 상담원 상태 확인 후 활성화되어 있지 않으면 대기실로 이동
  if (userRole === "client") {
    fetch('/api/contract/status')
            .then(response => response.json())
            .then(data => {
              if (!data.present) {
                // 상담원이 입장해 있지 않으면 대기실로 리다이렉트
                console.warn("상담원이 아직 입장하지 않음. 대기실로 이동합니다.");
                location.href = "/waiting-room";
              } else {
                console.log("상담원이 입장해 있습니다.");
              }
            })
            .catch(error => {
              console.error("상담원 상태 확인 오류:", error);
            });
  }

  // 세션 ID가 있으면 서버에서 데이터 로드
  if (sessionId) {
    console.log("세션 데이터 로드 시도:", sessionId);

    // 타임아웃 추가 (모든 것이 준비된 후 데이터 로드)
    setTimeout(() => {
      // 중요: 새로고침 복원 처리
      console.log("세션 데이터 로드 시작 - 강제 복원 모드");
      loadSessionData(true); // 강제 복원 모드
    }, 500);
  }

  // 처음에 커서 버튼을 활성화 상태로 설정
  document.getElementById('cursorBtn').classList.add('active');

  // 웹소켓 및 WebRTC 연결 초기화
  console.log("연결 초기화 시작");
  
  // 웹소켓 연결 초기화
  setTimeout(() => {
    if (typeof initializeWebSocket === 'function') {
      console.log("웹소켓 연결 초기화");
      initializeWebSocket();
    } else {
      console.error("웹소켓 초기화 함수가 로드되지 않았습니다.");
    }
  }, 1000);
};

// 세션 ID 생성 함수
function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}

// 역할에 따른 UI 초기화
function initializeUIByRole() {
  // 비디오 요소 참조
  const localVideo = document.getElementById("localVideo");
  const remoteVideo = document.getElementById("remoteVideo");

  // 비디오 요소 초기 설정 (역할 가리지 않고 공통으로 적용)
  if (localVideo) {
    localVideo.muted = true; // 자기 목소리가 스피커로 출력되는 것 방지
  }

  // 비디오 컨테이너 ID 설정 - 역할에 따라 명확하게 구분
  const localVideoContainer = document.getElementById("localVideoContainer");
  const remoteVideoContainer = document.getElementById("remoteVideoContainer");

  if (localVideoContainer) {
    localVideoContainer.setAttribute("data-role", userRole);
  }

  // 비디오 레이블 추가 - 역할에 따라 다르게 표시
  if (localVideoContainer && !localVideoContainer.querySelector('.video-label')) {
    const localVideoLabel = document.createElement("div");
    localVideoLabel.className = "video-label";
    localVideoLabel.innerText = userRole === "agent" ? "상담원 (나)" : "고객 (나)";
    localVideoContainer.appendChild(localVideoLabel);
  }

  if (remoteVideoContainer && !remoteVideoContainer.querySelector('.video-label')) {
    const remoteVideoLabel = document.createElement("div");
    remoteVideoLabel.className = "video-label";
    remoteVideoLabel.innerText = userRole === "agent" ? "고객" : "상담원";
    remoteVideoContainer.appendChild(remoteVideoLabel);
  }

  // 비디오 컨테이너에 CSS 스타일 추가
  if (!document.getElementById('video-container-styles')) {
    const styleElement = document.createElement('style');
    styleElement.id = 'video-container-styles';
    document.head.appendChild(styleElement);
  }

  // 상담원이면 도장 버튼 표시
  if (userRole === 'agent') {
    const stampBtn = document.getElementById('stampBtn');
    if (stampBtn) {
      stampBtn.style.display = 'flex';
    }
  }
}

// 상담원 상태 업데이트 함수
function updateAgentStatus(isPresent) {
  if (userRole !== "agent") return; // 상담원만 상태 업데이트 가능

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

  fetch('/api/contract/status', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify({ present: isPresent })
  })
  .then(response => {
    if (response.ok) {
      console.log("상담원 상태 업데이트 성공:", isPresent ? "입장" : "퇴장");
    } else {
      console.error("상담원 상태 업데이트 실패");
    }
  })
  .catch(error => {
    console.error("상담원 상태 업데이트 오류:", error);
  });
}

// 상담 종료 처리
function endConsult() {
  // 사용자에게 확인 요청
  if (!confirm("상담을 종료하시겠습니까? 상담 데이터는 자동으로 저장됩니다.")) {
    return;
  }

  // 상담원인 경우 상태 업데이트
  if (userRole === "agent") {
    updateAgentStatus(false);
  }

  // 녹음 중이면 중지
  if (mediaRecorder && mediaRecorder.state === "recording") {
    mediaRecorder.stop();
  }

  // WebRTC 연결 종료
  if (pc) {
    pc.close();
    pc = null;
  }

  // WebSocket 연결 종료
  if (stompClient) {
    stompClient.disconnect();
    stompClient = null;
  }

  // 로컬 트랙 종료
  const localVideo = document.getElementById("localVideo");
  if (localVideo && localVideo.srcObject) {
    localVideo.srcObject.getTracks().forEach(track => track.stop());
  }

  // 완료 모달 표시
  document.getElementById('completeModal').style.display = 'block';
}

// 홈페이지로 이동
function goToHomePage() {
  // 세션 스토리지 클리어 (상담 관련 데이터)
  sessionStorage.removeItem("sessionId");
  // 홈으로 이동
  window.location.href = "/";
}

// 세션 데이터 로드
function loadSessionData(forceRestore = false) {
  if (!sessionId) {
    console.error("세션 ID가 없어 데이터를 로드할 수 없습니다.");
    return;
  }

  // 이미 데이터가 로드되었으면 중복 로드 방지
  if (window.dataLoaded && !forceRestore) {
    console.log("데이터가 이미 로드되었습니다. 중복 로드 방지.");
    return;
  }

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

  console.log(`세션 데이터 로드 요청: ${sessionId}`);

  // 세션 데이터 요청
  fetch(`/api/contract/session/${sessionId}`, {
    method: 'GET',
    headers: {
      [header]: token
    }
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`세션 데이터 로드 실패: ${response.status}`);
    }
    return response.json();
  })
  .then(data => {
    console.log("세션 데이터 로드 성공:", data);

    // PDF URL이 있으면 PDF 로드
    if (data.pdfUrl) {
      console.log("저장된 PDF URL이 있습니다. PDF 로드 시도:", data.pdfUrl);
      
      // PDF URL 설정 및 로드
      uploadedPdfUrl = data.pdfUrl;
      loadAndRenderPDF(uploadedPdfUrl, data.currentPage || 1);
      
      // 연관 데이터 복원
      if (data.drawingData) {
        try {
          drawingDataPerPage = JSON.parse(data.drawingData);
          console.log("드로잉 데이터 복원됨");
        } catch (e) {
          console.error("드로잉 데이터 파싱 오류:", e);
        }
      }
      
      if (data.textData) {
        try {
          textDataPerPage = JSON.parse(data.textData);
          console.log("텍스트 데이터 복원됨");
        } catch (e) {
          console.error("텍스트 데이터 파싱 오류:", e);
        }
      }
      
      if (data.stampData) {
        try {
          stampDataPerPage = JSON.parse(data.stampData);
          console.log("도장 데이터 복원됨");
        } catch (e) {
          console.error("도장 데이터 파싱 오류:", e);
        }
      }
      
      // 데이터 로드 완료 표시
      window.dataLoaded = true;
    } else {
      console.log("저장된 PDF URL이 없습니다.");
    }
  })
  .catch(error => {
    // 첫 세션이거나 데이터가 없는 경우는 오류가 아님
    console.log("세션 데이터가 없거나 첫 세션입니다:", error);
  });
} 