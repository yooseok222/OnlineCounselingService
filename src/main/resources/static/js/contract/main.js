// 즉시 실행 - 스크립트 로드 확인
console.log("🚀 main.js 파일이 로드되었습니다!");
console.log("현재 시간:", new Date().toLocaleString());
console.log("현재 URL:", window.location.href);

// DOM이 로드되면 바로 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log("🔥 DOM 로드 완료 - main.js 실행 중");
});

// HTML에서 이미 선언된 전역 변수들을 사용 (중복 선언 제거)
// let drawingDataPerPage = {}; // HTML에서 이미 선언됨
// let stampDataPerPage = {}; // HTML에서 이미 선언됨  
// let textDataPerPage = {}; // HTML에서 이미 선언됨
// let signatureDataPerPage = {}; // HTML에서 이미 선언됨
// let pdfDoc = null; // HTML에서 이미 선언됨
// let currentPage = 1; // HTML에서 이미 선언됨
// let renderTask = null; // HTML에서 이미 선언됨
// let stompClient = null; // HTML에서 이미 선언됨
// let mode = null; // HTML에서 이미 선언됨
// let drawing = false; // HTML에서 이미 선언됨
// let pendingText = null; // HTML에서 이미 선언됨
// let uploadedPdfUrl = null; // HTML에서 이미 선언됨
// let userRole = null; // HTML에서 이미 선언됨
// let sessionId = null; // HTML에서 이미 선언됨 - 이것이 오류 원인이었음!

// 새로 추가하는 전역 변수
let currentContractId = null;

// 전역 변수로 beforeunload 핸들러 관리
let preventRefreshHandler = null;
let isConsultationEnding = false; // 상담종료 진행 중 플래그

// 페이지 로드 시 사용자 역할 확인 및 UI 초기화
window.onload = function() {
  console.log("=== 윈도우 로드됨 - 초기화 시작 ===");

  // 새로고침 방지 이벤트 리스너 추가 - 최상위 우선순위로 설정
  preventRefreshHandler = function(e) {
    // 상담종료 진행 중이면 이벤트를 무시
    if (isConsultationEnding) {
      return;
    }
    
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

  console.log("URL 파라미터 - role:", roleParam, "session:", sessionParam);
  console.log("현재 URL:", window.location.href);

  // 서버에서 전달받은 role 정보 확인
  const serverRole = (typeof window.serverRole !== 'undefined') ? window.serverRole : null;
  console.log("서버에서 전달받은 role:", serverRole);

  // 사용자 역할 설정 로직 개선 - 우선순위: 서버 전달 > URL 파라미터 > sessionStorage
  if (serverRole) {
    userRole = serverRole;
    sessionStorage.setItem("role", serverRole);
    console.log("서버에서 사용자 역할 설정:", userRole);
  } else if (roleParam) {
    userRole = roleParam;
    sessionStorage.setItem("role", roleParam);
    console.log("URL에서 사용자 역할 설정:", userRole);
  } else if (sessionStorage.getItem("role")) {
    userRole = sessionStorage.getItem("role");
    console.log("세션 스토리지에서 사용자 역할 설정:", userRole);
  } else {
    // URL 파라미터가 없는 경우 기본값 설정
    if (window.location.pathname.includes('/contract/room')) {
      // /contract/room 경로라면 기본적으로 상담원으로 설정
      userRole = 'agent';
      sessionStorage.setItem("role", 'agent');
      console.log("기본값으로 상담원 역할 설정:", userRole);
      
      // URL에 role 파라미터 추가
      const newUrl = new URL(window.location.href);
      newUrl.searchParams.set('role', 'agent');
      window.history.replaceState({}, '', newUrl);
  } else {
    console.error("역할 정보를 찾을 수 없습니다.");
    showError("역할 정보가 필요합니다. 올바른 URL로 접속해주세요.");
    location.href = "/";
    return;
    }
  }

  // 세션 ID 설정 로직 개선
  if (sessionParam) {
    // URL에 세션 ID가 있는 경우 - 우선순위 1
    sessionId = sessionParam;
    sessionStorage.setItem("sessionId", sessionId);
    console.log("URL에서 세션 ID 로드:", sessionId);
  } else if (sessionStorage.getItem("sessionId")) {
    // 세션 스토리지에 세션 ID가 있는 경우 - 우선순위 2
    sessionId = sessionStorage.getItem("sessionId");
    console.log("세션 스토리지에서 세션 ID 로드:", sessionId);
    
    // URL에도 세션 ID 추가 (동기화)
    const newUrl = new URL(window.location.href);
    newUrl.searchParams.set('session', sessionId);
    window.history.replaceState({}, '', newUrl);
  } else {
    // 세션 ID가 없는 경우 새로 생성 - 우선순위 3
    sessionId = generateSessionId();
    sessionStorage.setItem("sessionId", sessionId);
    console.log("새 세션 ID 생성:", sessionId);
    
    // URL에 세션 ID 추가
    const newUrl = new URL(window.location.href);
    newUrl.searchParams.set('session', sessionId);
    window.history.replaceState({}, '', newUrl);
  }
    
    // 고객 URL 표시 기능 제거됨 (불필요)

  // 세션 ID 확인 로그
  console.log("=== 세션 정보 확인 ===");
  console.log("최종 설정된 세션 ID:", sessionId);
  console.log("최종 설정된 사용자 역할:", userRole);
  console.log("URL 파라미터 세션:", sessionParam);
  console.log("sessionStorage 세션:", sessionStorage.getItem("sessionId"));
  console.log("=====================");

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
    // 상담원 입장 상태 변경 (세션 ID 포함)
    console.log('상담원 입장 - 상태를 true로 설정');
    updateAgentStatusWithSession(true, sessionId);
    
    // 추가 보장: 일정 시간 후 다시 한 번 설정
    setTimeout(() => {
      console.log('상담원 입장 상태 재확인 및 설정');
      updateAgentStatusWithSession(true, sessionId);
    }, 2000);
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

  // 스크롤 동기화 초기화
  setTimeout(() => {
    if (typeof initializeScrollSync === 'function') {
      console.log("스크롤 동기화 초기화");
      initializeScrollSync();
    } else {
      console.error("스크롤 동기화 초기화 함수가 로드되지 않았습니다.");
    }
  }, 1500);

  // 상담방 참여 - 한 번만 실행
  console.log("=== 상담방 참여 시작 ===");
  console.log("세션 ID:", sessionId);
  console.log("사용자 역할:", userRole);
  console.log("현재 Contract ID:", currentContractId);
  
  if (sessionId) {
    console.log("상담방 참여 시도");
    joinConsultationRoom(sessionId).then(() => {
      // 상담방 참여 후 Contract ID 확인
      console.log("상담방 참여 완료 후 Contract ID:", currentContractId);
      
      // Contract ID가 여전히 없으면 세션 ID로 조회 시도
      if (!currentContractId) {
        console.warn("상담방 참여 후에도 Contract ID가 없습니다. 세션 ID로 조회를 시도합니다.");
        setTimeout(() => {
          fetch(`/api/consultation/session/${sessionId}`)
            .then(response => response.json())
            .then(data => {
              if (data.success && data.contractId) {
                currentContractId = data.contractId;
                console.log("세션 ID로 Contract ID 설정 완료:", currentContractId);
              } else {
                console.error("세션 ID로 Contract ID 조회 실패:", data.message);
              }
            })
            .catch(error => {
              console.error("세션 ID로 Contract ID 조회 오류:", error);
            });
        }, 2000);
      }
    }).catch(error => {
      console.error("상담방 참여 실패:", error);
    });
  } else {
    console.error("세션 ID가 없어서 상담방 참여 불가");
  }

  // 상담 종료 버튼 이벤트 리스너 추가 (초기화 완료 후)
  setTimeout(() => {
    const endConsultBtn = document.querySelector('.end-consult-btn');
    if (endConsultBtn) {
      console.log('상담 종료 버튼 이벤트 리스너 추가');
      endConsultBtn.addEventListener('click', showEndConsultationModal);
    } else {
      console.log('상담 종료 버튼을 찾을 수 없습니다.');
    }
  }, 3000);
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

  // URL에서 entryType 파라미터 확인
  const urlParams = new URLSearchParams(window.location.search);
  const urlEntryType = urlParams.get('entryType');
  
  // 서버에서 전달받은 entryType 정보 확인 (전역 변수로 설정되어 있음)
  const serverEntryType = (typeof window.serverEntryType !== 'undefined') ? window.serverEntryType : null;
  
  // entryType 우선순위: 서버 전달 > URL 파라미터 > sessionStorage
  const entryType = serverEntryType || urlEntryType || sessionStorage.getItem('entryType');
  
  console.log('entryType 확인:');
  console.log('- 서버 전달:', serverEntryType);
  console.log('- URL 파라미터:', urlEntryType);
  console.log('- sessionStorage:', sessionStorage.getItem('entryType'));
  console.log('- 최종 선택:', entryType);

  // 역할에 따른 UI 설정
  if (userRole === 'agent') {
    // 상담원은 도장 버튼만 표시
    const stampBtn = document.getElementById('stampBtn');
    if (stampBtn) {
      stampBtn.style.display = 'inline-block';
    }
    console.log('상담원 UI 설정 완료 - 도장 버튼 표시');
  } else if (userRole === 'client') {
    // 고객의 경우 선택한 입장 방식에 따라 버튼 표시
    console.log('고객 UI 설정 시작 - entryType:', entryType);
    
    const stampBtn = document.getElementById('stampBtn');
    
    if (entryType === 'stamp') {
      // 도장으로 입장한 경우 - 도장 버튼만 표시
      if (stampBtn) {
        stampBtn.style.display = 'inline-block';
        stampBtn.innerHTML = '<i class="fas fa-stamp"></i> 도장';
        console.log('고객 도장 모드 - 도장 버튼만 표시');
      }
      
      // sessionStorage에서 고객의 도장 데이터 복원
      setTimeout(() => {
        restoreClientStampData();
      }, 1000);
      
    } else if (entryType === 'signature') {
      // 서명으로 입장한 경우 - 서명 버튼만 표시
      if (stampBtn) {
        // 내서명 버튼(stampBtn)을 숨김
        stampBtn.style.display = 'none';
        console.log('고객 서명 모드 - 내서명 버튼 숨김');
      }
      
      // signatureBtn 표시
      const signatureBtn = document.getElementById('signatureBtn');
      if (signatureBtn) {
        signatureBtn.style.display = 'inline-block';
        console.log('고객 서명 모드 - 서명 버튼만 표시');
      }
      
      // sessionStorage에서 고객의 서명 데이터 복원
      setTimeout(() => {
        restoreClientSignatureData();
      }, 1000);
      
    } else {
      // entryType이 없는 경우 (기존 사용자) - 도장 버튼 숨김
      if (stampBtn) {
        stampBtn.style.display = 'none';
        console.log('고객 기본 모드 - 도장 버튼 숨김');
      }
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

// 세션 ID를 포함한 상담원 상태 업데이트 함수
function updateAgentStatusWithSession(isPresent, sessionId) {
  if (userRole !== "agent") return; // 상담원만 상태 업데이트 가능

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

  const requestBody = { 
    present: isPresent,
    sessionId: sessionId
  };

  console.log("상담원 상태 업데이트 요청:", requestBody);

  fetch('/api/contract/status', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify(requestBody)
  })
  .then(response => {
    if (response.ok) {
      console.log("상담원 상태 업데이트 성공:", isPresent ? "입장" : "퇴장", "세션:", sessionId);
    } else {
      console.error("상담원 상태 업데이트 실패");
    }
  })
  .catch(error => {
    console.error("상담원 상태 업데이트 오류:", error);
  });
}

// 상담 종료 함수 (기존 함수를 새로운 기능으로 대체)
function endConsult() {
    showEndConsultationModal();
  }

/**
 * 현재 모드 해제
 */
function clearMode() {
  mode = null;
  document.getElementById('currentMode').textContent = '커서';
  
  // 모든 버튼 비활성화
  const buttons = ['highlighterBtn', 'penBtn', 'cursorBtn', 'textBtn', 'stampBtn', 'signatureBtn'];
  buttons.forEach(id => {
    const button = document.getElementById(id);
    if (button) {
      button.classList.remove('active');
    }
  });
  
  // 커서 버튼 활성화
  const cursorBtn = document.getElementById('cursorBtn');
  if (cursorBtn) {
    cursorBtn.classList.add('active');
  }
  
  // 커서 스타일 초기화
  const drawingCanvas = document.getElementById('drawingCanvas');
  if (drawingCanvas) {
    drawingCanvas.style.cursor = 'default';
  }
}

/**
 * 홈페이지로 이동 (기존 함수 유지)
 */
function goToHomePage() {
    // 세션 스토리지 클리어 (상담 관련 데이터)
    sessionStorage.removeItem("sessionId");
    sessionStorage.removeItem("role");

  // WebRTC 연결 종료
    if (typeof pc !== 'undefined' && pc) {
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

  // 로컬 스토리지에서 데이터 복원 시도
  try {
    // 로컬 스토리지에서 데이터 가져오기
    const savedDrawingData = localStorage.getItem(`drawing_${sessionId}`);
    const savedTextData = localStorage.getItem(`text_${sessionId}`);
    const savedStampData = localStorage.getItem(`stamp_${sessionId}`);
    const savedSignatureData = localStorage.getItem(`signature_${sessionId}`);
    const savedCurrentPage = localStorage.getItem(`currentPage_${sessionId}`);
    
    let restoredFromLocal = false;
    
    if (savedDrawingData) {
      drawingDataPerPage = JSON.parse(savedDrawingData);
      restoredFromLocal = true;
      console.log("로컬 스토리지에서 드로잉 데이터 복원됨");
    }
    
    if (savedTextData) {
      textDataPerPage = JSON.parse(savedTextData);
      restoredFromLocal = true;
      console.log("로컬 스토리지에서 텍스트 데이터 복원됨");
    }
    
    if (savedStampData) {
      stampDataPerPage = JSON.parse(savedStampData);
      restoredFromLocal = true;
      console.log("로컬 스토리지에서 도장 데이터 복원됨");
    }
    
    if (savedSignatureData) {
      signatureDataPerPage = JSON.parse(savedSignatureData);
      restoredFromLocal = true;
      console.log("로컬 스토리지에서 서명 데이터 복원됨");
    }
    
    if (savedCurrentPage) {
      currentPage = parseInt(savedCurrentPage);
      console.log("로컬 스토리지에서 현재 페이지 복원됨:", currentPage);
    }
    
    if (restoredFromLocal) {
      window.dataLoaded = true;
      
      // 현재 PDF가 로드되어 있으면 화면에 데이터 표시
      if (pdfDoc) {
        queueRenderPage(currentPage);
        
        // 모든 데이터 복원
        setTimeout(() => {
          if (typeof restoreDrawingData === 'function') restoreDrawingData();
          if (typeof restoreTextData === 'function') restoreTextData();
          if (typeof restoreStampData === 'function') restoreStampData();
          if (typeof restoreSignatureData === 'function') restoreSignatureData();
        }, 500);
      }
    }
  } catch (e) {
    console.error("로컬 스토리지에서 데이터 복원 중 오류:", e);
  }

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']") ? 
                document.querySelector("meta[name='_csrf']").getAttribute("content") : '';
  const header = document.querySelector("meta[name='_csrf_header']") ? 
                document.querySelector("meta[name='_csrf_header']").getAttribute("content") : '';

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
      
      if (data.signatureData) {
        try {
          signatureDataPerPage = JSON.parse(data.signatureData);
          console.log("서명 데이터 복원됨");
        } catch (e) {
          console.error("서명 데이터 파싱 오류:", e);
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

/**
 * 상담방 참여
 */
async function joinConsultationRoom(sessionId) {
    try {
        console.log('=== 상담방 참여 API 호출 시작 ===');
        console.log('세션 ID:', sessionId);
        console.log('사용자 역할:', userRole);
        
        // CSRF 토큰 가져오기
        const csrfToken = document.querySelector("meta[name='_csrf']");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']");
        
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        };
        
        // CSRF 토큰이 있으면 추가
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
            console.log('CSRF 토큰 추가됨');
        }
        
        console.log('API 요청 헤더:', headers);
        console.log('API 요청 URL:', '/api/consultation/room/join');
        console.log('API 요청 바디:', `sessionId=${encodeURIComponent(sessionId)}`);
        
        const response = await fetch('/api/consultation/room/join', {
            method: 'POST',
            headers: headers,
            body: `sessionId=${encodeURIComponent(sessionId)}`
        });
        
        console.log('API 응답 상태:', response.status, response.statusText);
        
        if (!response.ok) {
            console.error('API 응답 실패:', response.status, response.statusText);
            const errorText = await response.text();
            console.error('에러 내용:', errorText);
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const result = await response.json();
        console.log('API 응답 데이터:', result);
        
        if (result.success) {
            currentContractId = result.contractId;
            console.log('상담방 참여 성공:', result);
            console.log('Contract ID 설정됨:', currentContractId);
            
            // 사용자 역할 표시 업데이트
            updateUserRoleDisplay(result.userRole, result.userEmail);
            
            // 상담원인 경우 상담원 상태 업데이트 (세션 ID 포함)
            if (userRole === "agent") {
                console.log('상담원 입장 - 상태를 true로 설정');
                updateAgentStatusWithSession(true, sessionId);
                
                // 추가 보장: 일정 시간 후 다시 한 번 설정
                setTimeout(() => {
                  console.log('상담원 입장 상태 재확인 및 설정');
                  updateAgentStatusWithSession(true, sessionId);
                }, 2000);
            }
        } else {
            console.error('상담방 참여 실패:', result.message);
        }
    } catch (error) {
        console.error('=== 상담방 참여 오류 ===');
        console.error('오류:', error);
        console.error('오류 스택:', error.stack);
    }
}

/**
 * 사용자 역할 표시 업데이트
 */
function updateUserRoleDisplay(role, email) {
    const userRoleDisplay = document.getElementById('userRoleDisplay');
    if (userRoleDisplay) {
        const roleText = role === 'AGENT' ? '상담원' : '고객';
        userRoleDisplay.innerHTML = `
            <span class="user-info">
                <i class="fas fa-user"></i> ${roleText} (${email})
            </span>
        `;
    }
}

/**
 * 상담 종료 모달 표시
 */
function showEndConsultationModal() {
    console.log('=== 상담 종료 모달 표시 시작 ===');
    console.log('현재 Contract ID:', currentContractId);
    console.log('현재 세션 ID:', sessionId);
    
    // 진행 중인 상담 ID가 없을 때 자동으로 API를 통해 현재 상담 정보 가져오기 시도
    if (!currentContractId) {
        console.log('상담 ID가 없어 상담 정보 조회 시도');
        
        // 세션 ID로 상담 정보 조회
        if (sessionId) {
            console.log('세션 ID로 상담 정보 조회 시작:', sessionId);
            
            // CSRF 토큰 가져오기
            const csrfToken = document.querySelector("meta[name='_csrf']");
            const csrfHeader = document.querySelector("meta[name='_csrf_header']");
            
            const headers = {
                'X-Requested-With': 'XMLHttpRequest'
            };
            
            // CSRF 토큰이 있으면 추가
            if (csrfToken && csrfHeader) {
                headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
                console.log('CSRF 토큰 추가됨');
            }
            
            console.log('API 요청 URL:', `/api/consultation/session/${sessionId}`);
            console.log('API 요청 헤더:', headers);
            
            fetch(`/api/consultation/session/${sessionId}`, {
                method: 'GET',
                headers: headers
            })
            .then(response => {
                console.log('상담 정보 조회 응답 상태:', response.status, response.statusText);
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('상담 정보 조회 응답 데이터:', data);
                console.log('응답 success 값:', data.success);
                console.log('응답 contractId 값:', data.contractId);
                console.log('응답 message 값:', data.message);
                
                if (data.success && data.contractId) {
                    console.log('상담 정보 조회 성공:', data);
                    currentContractId = data.contractId;
                    console.log('Contract ID 설정됨:', currentContractId);
                    
                    // 상담 정보를 가져온 후 모달 표시 처리 계속
                    showEndConsultationModalInternal();
                } else {
                    console.error('상담 정보 조회 실패:', data);
                    console.error('실패 이유:', data.message || '알 수 없는 오류');
                    alert('상담 정보를 찾을 수 없습니다. 상담방에 다시 입장해주세요.');
                }
            })
            .catch(error => {
                console.error('상담 정보 조회 오류:', error);
                console.error('오류 스택:', error.stack);
                alert('상담 정보 조회 중 오류가 발생했습니다: ' + error.message);
            });
        } else {
            console.log('세션 정보가 없어 상담을 종료할 수 없습니다.');
            alert('세션 정보가 없습니다. 상담방에 다시 입장해주세요.');
        }
        
        return;
    }
    
    // Contract ID가 있는 경우 바로 모달 표시
    console.log('Contract ID가 있어 바로 모달 표시');
    showEndConsultationModalInternal();
}

/**
 * 상담 종료 모달 내부 표시 함수
 * (상담 ID가 확인된 후 호출됨)
 */
function showEndConsultationModalInternal() {
    // 기존 모달이 있으면 제거
    const existingModal = document.getElementById('endConsultationModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 모달 HTML 생성
    const modalHTML = `
        <div id="endConsultationModal" class="modal-overlay">
            <div class="modal-content">
                <h3><i class="fas fa-clipboard-check"></i> 상담 종료</h3>
                <p>상담을 종료하시겠습니까?</p>
                <div class="memo-section">
                    <label for="consultationMemo">상담 메모:</label>
                    <textarea id="consultationMemo" placeholder="상담 내용을 간단히 기록해주세요..." rows="4"></textarea>
                </div>
                <div class="modal-buttons">
                    <button onclick="endConsultation()" class="btn-confirm">
                        <i class="fas fa-check"></i> 확인
                    </button>
                    <button onclick="closeEndConsultationModal()" class="btn-cancel">
                        <i class="fas fa-times"></i> 취소
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // 모달을 body에 추가
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    
    // 모달 스타일 추가
    addModalStyles();
}

/**
 * 상담 종료 모달 닫기
 */
function closeEndConsultationModal() {
    const modal = document.getElementById('endConsultationModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 상담 종료 실행
 */
async function endConsultation() {
    // 상담종료 진행 중 플래그 설정 (beforeunload 이벤트 비활성화)
    isConsultationEnding = true;
    
    const memo = document.getElementById('consultationMemo').value.trim();
    
    if (!memo) {
        // 메모가 없으면 플래그 해제하고 종료
        isConsultationEnding = false;
        showError('상담 메모를 입력해주세요.');
        return;
    }
    
    try {
        console.log('=== 상담 종료 시작 ===');
        console.log('Contract ID:', currentContractId);
        console.log('Session ID:', sessionId);
        console.log('User Role:', userRole);
        console.log('Memo:', memo);
        
        // 로딩 메시지 표시
        const confirmButton = document.querySelector('#endConsultationModal .btn-confirm');
        if (confirmButton) {
            confirmButton.disabled = true;
            confirmButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
        }
        
        // Contract ID 재확인 및 설정
        if (!currentContractId) {
            console.warn('Contract ID가 없습니다. 세션 ID로 다시 조회를 시도합니다.');
            try {
                const sessionResponse = await fetch(`/api/consultation/session/${sessionId}`);
                if (sessionResponse.ok) {
                    const sessionResult = await sessionResponse.json();
                    if (sessionResult.success && sessionResult.contractId) {
                        currentContractId = sessionResult.contractId;
                        console.log('세션 ID로 Contract ID 재설정 완료:', currentContractId);
                    }
                }
            } catch (sessionError) {
                console.error('세션 ID로 Contract ID 조회 실패:', sessionError);
            }
        }
        
        // 1. 녹음 중인 경우 자동 저장 (실패해도 상담 종료는 진행)
        try {
            if (typeof saveRecordingOnConsultationEnd === 'function') {
                await saveRecordingOnConsultationEnd();
                console.log('상담 종료 시 녹음 저장 완료');
            }
        } catch (recordingError) {
            console.error('상담 종료 시 녹음 저장 실패 (상담 종료는 계속 진행):', recordingError);
            if (typeof showToast === 'function') {
                showToast("녹음 저장 실패", "녹음 저장에 실패했지만 상담은 정상적으로 종료됩니다.", "warning");
            }
        }

        // 2. PDF 생성 및 이메일 전송 (실패해도 상담 종료는 진행)
        try {
            await generateAndSendPdf();
            console.log('PDF 생성 및 메일 발송 완료');
        } catch (pdfError) {
            console.error('PDF 생성 및 메일 발송 실패 (상담 종료는 계속 진행):', pdfError);
            
                         // 오류 유형에 따른 메시지 구분
             let errorMessage = "PDF 생성 또는 메일 발송에 실패했습니다.";
             if (pdfError.message.includes('PDF가 업로드되지 않았습니다')) {
                 errorMessage = "상담 문서(PDF)가 업로드되지 않아 메일 발송을 건너뜁니다.";
             } else if (pdfError.message.includes('PDF 라이브러리')) {
                 errorMessage = "PDF 처리 라이브러리 오류로 메일 발송에 실패했습니다.";
             } else if (pdfError.message.includes('PDF 파일을 가져올 수 없습니다')) {
                 errorMessage = "업로드된 PDF 파일에 접근할 수 없어 메일 발송에 실패했습니다.";
             } else if (pdfError.message.includes('계약 ID가 없습니다') || pdfError.message.includes('계약 정보를 가져올 수 없습니다')) {
                 errorMessage = "계약 정보를 찾을 수 없어 메일 발송에 실패했습니다.";
             } else if (pdfError.message.includes('계약 정보 조회 HTTP 오류')) {
                 errorMessage = "서버 오류로 인해 메일 발송에 실패했습니다.";
             }
            
            // PDF 발송 실패 알림 (상담 종료는 계속 진행)
            if (typeof showToast === 'function') {
                showToast("PDF 발송 실패", errorMessage + " 상담은 정상적으로 종료됩니다.", "warning");
            } else {
                console.warn('PDF 생성/발송 실패: ' + errorMessage + ' 상담은 정상적으로 종료됩니다.');
                showWarning('PDF 발송 실패: ' + errorMessage + ' 상담은 정상적으로 종료됩니다.');
            }
        }
        
        // 3. 상담 종료 처리
        // Contract ID 최종 확인
        if (!currentContractId) {
            console.error('Contract ID가 여전히 없습니다. 상담 종료를 진행할 수 없습니다.');
            throw new Error('계약 ID를 찾을 수 없어 상담을 종료할 수 없습니다. 페이지를 새로고침 후 다시 시도해주세요.');
        }
        
        console.log('상담 종료 API 호출 준비 - Contract ID:', currentContractId);
        
        // CSRF 토큰 가져오기
        const csrfToken = document.querySelector("meta[name='_csrf']");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']");
        
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        };
        
        // CSRF 토큰이 있으면 추가
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
        }
        
        const endConsultationBody = `contractId=${currentContractId}&memo=${encodeURIComponent(memo)}&sessionId=${encodeURIComponent(sessionId)}`;
        console.log('상담 종료 API 요청 바디:', endConsultationBody);
        
        const response = await fetch('/api/consultation/end', {
            method: 'POST',
            headers: headers,
            body: endConsultationBody
        });
        
        console.log('상담 종료 API 응답 상태:', response.status);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const result = await response.json();
        console.log('상담 종료 API 응답 데이터:', result);
        
        if (result.success) {
            console.log('상담 종료 성공:', result);
            
            // 모달 닫기
            closeEndConsultationModal();
            
            // 상담원 상태 리셋을 가장 먼저 실행 (매우 중요!)
            if (userRole === 'agent') {
                console.log('상담원 상태를 false로 리셋합니다.');
                try {
                    // 동기식으로 상담원 상태 리셋
                    await fetch('/api/contract/status', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [csrfHeader.getAttribute('content')]: csrfToken.getAttribute('content')
                        },
                        body: JSON.stringify({ 
                            present: false,
                            sessionId: sessionId 
                        })
                    });
                    console.log('상담원 상태 리셋 완료');
                } catch (resetError) {
                    console.error('상담원 상태 리셋 실패:', resetError);
                }
                
                // 추가 보장: AgentStatusService도 직접 리셋
                try {
                    await fetch('/api/agent/reset', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [csrfHeader.getAttribute('content')]: csrfToken.getAttribute('content')
                        }
                    });
                    console.log('AgentStatusService 강제 리셋 완료');
                } catch (serviceResetError) {
                    console.error('AgentStatusService 리셋 실패:', serviceResetError);
                }
            }
            
            // WebSocket을 통해 상대방에게 상담 종료 알림 전송 (추가 보장)
            if (typeof stompClient !== 'undefined' && stompClient && stompClient.connected) {
                try {
                    const endMessage = {
                        message: "상담이 종료되었습니다. 메인 페이지로 이동합니다.",
                        contractId: currentContractId,
                        redirectUrl: userRole === 'agent' ? "/agent/dashboard" : "/",
                        sessionId: sessionId,
                        sender: userRole
                    };
                    
                    // 세션별 상담 종료 메시지 전송
                    stompClient.send(`/app/sync/endConsult`, {}, JSON.stringify(endMessage));
                    console.log('WebSocket 상담 종료 메시지 전송 완료');
                } catch (wsError) {
                    console.error('WebSocket 메시지 전송 오류:', wsError);
                }
            }
            
            // 상담 종료 완료
            console.log('상담 종료 완료');
            
            // 상담종료 성공 메시지 표시 후 페이지 이동
            const successMessage = userRole === 'agent' ? 
                '상담이 종료되었습니다. 대시보드로 이동합니다.' : 
                '상담이 종료되었습니다. 메인 페이지로 이동합니다.';
            
            showSuccess(successMessage).then(() => {
                const redirectUrl = userRole === 'agent' ? '/agent/dashboard' : '/';
                console.log(`상담 종료 후 페이지 이동: ${redirectUrl}`);
                window.location.href = redirectUrl;
            });
        } else {
            console.error('상담 종료 실패:', result.message);
            isConsultationEnding = false; // 실패 시 플래그 해제
            showError('상담 종료에 실패했습니다: ' + result.message);
            
            // 실패 시 버튼 복원
            const confirmButton = document.querySelector('#endConsultationModal .btn-confirm');
            if (confirmButton) {
                confirmButton.disabled = false;
                confirmButton.innerHTML = '<i class="fas fa-check"></i> 확인';
            }
        }
    } catch (error) {
        console.error('상담 종료 오류:', error);
        isConsultationEnding = false; // 오류 시 플래그 해제
        showError('상담 종료 중 오류가 발생했습니다: ' + error.message);
        
        // 오류 발생 시 버튼 복원
        const confirmButton = document.querySelector('#endConsultationModal .btn-confirm');
        if (confirmButton) {
            confirmButton.disabled = false;
            confirmButton.innerHTML = '<i class="fas fa-check"></i> 확인';
        }
    }
}

/**
 * PDF 생성 및 이메일 전송
 */
async function generateAndSendPdf() {
    try {
        console.log('PDF 생성 및 이메일 전송 시작');
        
        // PDF 생성 (기존 함수 사용)
        console.log('PDF 생성 함수 호출 시작');
        console.log('현재 업로드된 PDF URL:', uploadedPdfUrl);
        
        // PDF가 업로드되지 않은 경우 처리
        if (!uploadedPdfUrl) {
            console.warn('PDF가 업로드되지 않았습니다. PDF 생성을 건너뜁니다.');
            throw new Error('PDF가 업로드되지 않았습니다. 상담 문서가 없어 PDF를 생성할 수 없습니다.');
        }
        
        const pdfData = await savePdfWithStampAndSignature(true); // forEmail = true
        console.log('PDF 생성 함수 호출 완료');
        console.log('PDF 데이터 존재 여부:', !!pdfData);
        console.log('PDF 데이터 타입:', typeof pdfData);
        
        if (!pdfData) {
            console.error('PDF 생성 결과가 null 또는 undefined');
            throw new Error('PDF 생성에 실패했습니다.');
        }
        
        if (typeof pdfData !== 'string') {
            console.error('PDF 데이터가 문자열이 아님:', typeof pdfData);
            throw new Error('PDF 데이터 형식이 올바르지 않습니다.');
        }
        
        console.log('PDF 생성 성공 - 데이터 길이:', pdfData.length);
        
        // 계약 정보 조회하여 고객 이메일 가져오기
        console.log('계약 정보 조회 시작 - Contract ID:', currentContractId);
        
        if (!currentContractId) {
            console.error('Contract ID가 null 또는 undefined입니다.');
            throw new Error('계약 ID가 없습니다. 상담방에 다시 입장해주세요.');
        }
        
        const contractApiUrl = `/api/consultation/contract/${currentContractId}`;
        console.log('계약 정보 API 호출 URL:', contractApiUrl);
        
        const contractResponse = await fetch(contractApiUrl);
        console.log('계약 정보 API 응답 상태:', contractResponse.status, contractResponse.statusText);
        
        if (!contractResponse.ok) {
            const errorText = await contractResponse.text();
            console.error('계약 정보 API HTTP 오류:', contractResponse.status, errorText);
            throw new Error(`계약 정보 조회 HTTP 오류 (${contractResponse.status}): ${errorText}`);
        }
        
        const contractResult = await contractResponse.json();
        console.log('계약 정보 API 응답 데이터:', contractResult);
        
        if (!contractResult.success) {
            console.error('계약 정보 조회 실패:', contractResult.message);
            throw new Error('계약 정보를 가져올 수 없습니다: ' + (contractResult.message || '알 수 없는 오류'));
        }
        
        const clientEmail = contractResult.contract.clientEmail; // DB JOIN으로 조회된 고객 이메일
        
        if (!clientEmail) {
            throw new Error('고객 이메일을 찾을 수 없습니다.');
        }
        
        console.log('고객 이메일:', clientEmail);
        
        // PDF 이메일 전송
        console.log('PDF 이메일 전송 요청 시작');
        console.log('- Contract ID:', currentContractId);
        console.log('- Client Email:', clientEmail);
        console.log('- PDF Data 길이:', pdfData ? pdfData.length : 'null');
        
        // CSRF 토큰 가져오기
        const csrfToken = document.querySelector("meta[name='_csrf']");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']");
        
        const emailHeaders = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        };
        
        // CSRF 토큰이 있으면 추가
        if (csrfToken && csrfHeader) {
            emailHeaders[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
            console.log('CSRF 토큰 추가됨');
        }
        
        const emailResponse = await fetch('/api/consultation/send-pdf', {
            method: 'POST',
            headers: emailHeaders,
            body: `contractId=${currentContractId}&clientEmail=${encodeURIComponent(clientEmail)}&pdfData=${encodeURIComponent(pdfData)}`
        });
        
        console.log('PDF 이메일 전송 응답 상태:', emailResponse.status, emailResponse.statusText);
        
        if (!emailResponse.ok) {
            const errorText = await emailResponse.text();
            console.error('PDF 이메일 전송 HTTP 오류:', errorText);
            throw new Error(`PDF 전송 HTTP 오류 (${emailResponse.status}): ${errorText}`);
        }
        
        const emailResult = await emailResponse.json();
        console.log('PDF 이메일 전송 결과:', emailResult);
        
        if (emailResult.success) {
            console.log('PDF 이메일 전송 성공');
            // 성공 알림 (선택적)
            if (typeof showToast === 'function') {
                showToast("PDF 발송 완료", "상담 문서가 고객 이메일로 전송되었습니다.", "success");
            }
        } else {
            console.error('PDF 이메일 전송 실패:', emailResult.message);
            throw new Error('PDF 이메일 전송 실패: ' + emailResult.message);
        }
    } catch (error) {
        console.error('PDF 생성 및 전송 오류:', error);
        throw error; // 오류를 다시 던져서 상위에서 처리할 수 있도록 함
    }
}

/**
 * 고객의 도장 데이터 복원
 */
function restoreClientStampData() {
  try {
    const savedStampImage = sessionStorage.getItem("stampImage");
    const savedStampId = sessionStorage.getItem("selectedStampId");
    
    if (savedStampImage && savedStampId) {
      console.log('고객 도장 데이터 복원 시작');
      
      // 현재 페이지에 도장 데이터 설정
      if (!stampDataPerPage[currentPage]) {
        stampDataPerPage[currentPage] = [];
      }
      
      // 기존에 복원된 도장이 있는지 확인
      const existingStamp = stampDataPerPage[currentPage].find(stamp => 
        stamp.type === 'client_stamp' && stamp.stampId === savedStampId
      );
      
      if (!existingStamp) {
        // 도장 데이터를 페이지 중앙에 미리 배치
        const canvas = document.getElementById('pdfCanvas');
        const centerX = canvas ? canvas.width / 2 : 400;
        const centerY = canvas ? canvas.height / 2 : 300;
        
        stampDataPerPage[currentPage].push({
          type: 'client_stamp',
          stampId: savedStampId,
          imageData: savedStampImage,
          x: centerX - 50, // 중앙에서 약간 왼쪽
          y: centerY - 25, // 중앙에서 약간 위쪽
          width: 100,
          height: 50,
          timestamp: Date.now()
        });
        
        console.log('고객 도장 데이터 복원 완료');
        
        // 화면에 표시
        if (typeof restoreStampData === 'function') {
          restoreStampData();
        }
      }
    } else {
      console.log('복원할 고객 도장 데이터가 없습니다.');
    }
  } catch (error) {
    console.error('고객 도장 데이터 복원 오류:', error);
  }
}

/**
 * 고객의 서명 데이터 복원
 */
function restoreClientSignatureData() {
  try {
    const savedSignatureImage = sessionStorage.getItem("signatureImage");
    
    if (savedSignatureImage) {
      console.log('고객 서명 데이터 복원 시작');
      
      // 현재 페이지에 서명 데이터 설정
      if (!signatureDataPerPage[currentPage]) {
        signatureDataPerPage[currentPage] = [];
      }
      
      // 기존에 복원된 서명이 있는지 확인
      const existingSignature = signatureDataPerPage[currentPage].find(sig => 
        sig.type === 'client_signature'
      );
      
      if (!existingSignature) {
        // 서명 데이터를 페이지 하단에 미리 배치
        const canvas = document.getElementById('pdfCanvas');
        const bottomX = canvas ? canvas.width - 200 : 600;
        const bottomY = canvas ? canvas.height - 100 : 500;
        
        signatureDataPerPage[currentPage].push({
          type: 'client_signature',
          imageData: savedSignatureImage,
          x: bottomX,
          y: bottomY,
          width: 150,
          height: 75,
          timestamp: Date.now()
        });
        
        console.log('고객 서명 데이터 복원 완료');
        
        // 화면에 표시
        if (typeof restoreSignatureData === 'function') {
          restoreSignatureData();
        }
      }
    } else {
      console.log('복원할 고객 서명 데이터가 없습니다.');
    }
  } catch (error) {
    console.error('고객 서명 데이터 복원 오류:', error);
  }
}

/**
 * 서명 모드 설정 (고객용)
 */
function setSignatureMode() {
  console.log('서명 모드 설정');
  
  // 기존 모드 해제
  clearMode();
  
  // 서명 모드 설정
  mode = 'signature';
  document.getElementById('currentMode').textContent = '서명';
  
  // 서명 버튼 활성화 표시
  const stampBtn = document.getElementById('stampBtn');
  if (stampBtn) {
    stampBtn.classList.add('active');
  }
  
  // 커서 스타일 변경
  const drawingCanvas = document.getElementById('drawingCanvas');
  if (drawingCanvas) {
    drawingCanvas.style.cursor = 'crosshair';
  }
}

/**
 * 모달 스타일 추가
 */
function addModalStyles() {
    // 이미 스타일이 추가되어 있는지 확인
    if (document.getElementById('modalStyles')) {
        return;
    }
    
    const style = document.createElement('style');
    style.id = 'modalStyles';
    style.textContent = `
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 10000;
        }
        
        .modal-content {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            max-width: 500px;
            width: 90%;
            max-height: 80vh;
            overflow-y: auto;
        }
        
        .modal-content h3 {
            margin-top: 0;
            color: #333;
            border-bottom: 2px solid #0057d7;
            padding-bottom: 10px;
        }
        
        .memo-section {
            margin: 20px 0;
        }
        
        .memo-section label {
            display: block;
            margin-bottom: 8px;
            font-weight: bold;
            color: #555;
        }
        
        .memo-section textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-family: inherit;
            resize: vertical;
            min-height: 100px;
        }
        
        .modal-buttons {
            display: flex;
            gap: 10px;
            justify-content: flex-end;
            margin-top: 20px;
        }
        
        .btn-confirm, .btn-cancel {
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background-color 0.3s;
        }
        
        .btn-confirm {
            background-color: #0057d7;
            color: white;
        }
        
        .btn-confirm:hover {
            background-color: #004bb5;
        }
        
        .btn-cancel {
            background-color: #6c757d;
            color: white;
        }
        
        .btn-cancel:hover {
            background-color: #545b62;
        }
        
        .user-info {
            background-color: #f8f9fa;
            padding: 8px 12px;
            border-radius: 5px;
            font-size: 14px;
            color: #495057;
        }
    `;
    
    document.head.appendChild(style);
}

// 페이지 이탈 시 상담원 상태 리셋 (매우 중요!)
window.addEventListener('beforeunload', function(e) {
    // 상담 종료 진행 중이면 경고 메시지 비활성화
    if (isConsultationEnding) {
        return;
    }
    
    // 상담원이 페이지를 떠날 때 상태 리셋
    if (userRole === 'agent') {
        console.log('상담원이 페이지를 떠남 - 상태 리셋');
        
        // 방법 1: sendBeacon으로 상태 리셋
        try {
            const resetData = JSON.stringify({ 
                present: false,
                sessionId: sessionId 
            });
            
            const blob = new Blob([resetData], { type: 'application/json' });
            
            // sendBeacon은 페이지 언로드 시에도 요청을 완료할 수 있음
            const success1 = navigator.sendBeacon('/api/contract/status', blob);
            console.log('상담원 상태 리셋 sendBeacon 결과:', success1);
            
            // 방법 2: AgentStatusService 직접 리셋
            const success2 = navigator.sendBeacon('/api/agent/reset', new Blob(['{}'], { type: 'application/json' }));
            console.log('AgentStatusService 리셋 sendBeacon 결과:', success2);
            
        } catch (error) {
            console.error('상담원 상태 리셋 중 오류:', error);
        }
        
        // 방법 3: 동기식 XMLHttpRequest (최후 수단)
        try {
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/agent/reset', false); // 동기식
            xhr.setRequestHeader('Content-Type', 'application/json');
            
            // CSRF 토큰 추가
            const csrfToken = document.querySelector("meta[name='_csrf']");
            const csrfHeader = document.querySelector("meta[name='_csrf_header']");
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader.getAttribute('content'), csrfToken.getAttribute('content'));
            }
            
            xhr.send('{}');
            console.log('동기식 상담원 상태 리셋 완료');
        } catch (xhrError) {
            console.error('동기식 상담원 상태 리셋 실패:', xhrError);
        }
    }
}); 