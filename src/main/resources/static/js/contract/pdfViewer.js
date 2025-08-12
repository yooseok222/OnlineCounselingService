// PDF 뷰어 관련 기능

// PDF.js 워커 경로 설정
pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.4.120/pdf.worker.min.js';

// PDF 로드 재시도 카운터
let pdfLoadRetries = 0;
const MAX_PDF_LOAD_RETRIES = 3;

// PDF 로드 및 렌더링 함수
async function loadAndRenderPDF(url, targetPage = 1) {
  try {
    console.log("PDF 로드 시작:", url);
    
    // URL이 없으면 오류
    if (!url) {
      console.error("PDF URL이 제공되지 않았습니다.");
      return false;
    }
    
    // 기존 렌더링 작업이 있으면 취소
    if (renderTask) {
      renderTask.cancel();
    }

    // 타임스탬프 매개변수 추가하여 캐싱 방지
    const pdfUrl = url.includes('?') ? 
      `${url}&_t=${Date.now()}` : 
      `${url}?_t=${Date.now()}`;

    // PDF 문서 로드 옵션 추가
    try {
      pdfDoc = await pdfjsLib.getDocument({
        url: pdfUrl,
        cMapUrl: '/static/js/pdf/cmaps/',
        cMapPacked: true,
        disableRange: false,
        disableStream: false,
        disableAutoFetch: false
      }).promise;
      
      console.log(`PDF 로드 완료, 총 ${pdfDoc.numPages} 페이지`);
      
      // 로드 성공 시 재시도 카운터 초기화
      pdfLoadRetries = 0;
      
      // 로드 성공 메시지
      showToast("PDF 로드 완료", `${pdfDoc.numPages}페이지 문서가 로드되었습니다.`, "success");
    } catch (loadError) {
      console.error("PDF 로드 실패:", loadError);
      
      // 재시도 횟수 증가 및 확인
      pdfLoadRetries++;
      if (pdfLoadRetries < MAX_PDF_LOAD_RETRIES) {
        console.log(`PDF 로드 재시도 (${pdfLoadRetries}/${MAX_PDF_LOAD_RETRIES})...`);
        showToast("PDF 로드 재시도", "PDF 문서를 다시 로드합니다...", "info");
        
        // 1초 후 재시도
        setTimeout(() => {
          loadAndRenderPDF(url, targetPage);
        }, 1000);
        return false;
      } else {
        showToast("PDF 로드 실패", "PDF 문서를 불러올 수 없습니다. 다시 업로드해주세요.", "error");
        return false;
      }
    }

    // 현재 페이지 설정 (targetPage 또는 1)
    currentPage = Math.min(Math.max(1, targetPage), pdfDoc.numPages);
    
    // 페이지 렌더링
    await renderPage(currentPage);

    // 현재 페이지 번호 세션 스토리지에 저장 (새로고침 시 복원용)
    sessionStorage.setItem("lastPage", currentPage);
    
    // 서버에 세션 데이터 저장 (PDF URL, 현재 페이지)
    saveSessionData();

    // 드로잉 데이터가 있으면 복원
    if (currentPage in drawingDataPerPage) {
      restoreDrawingData();
    }

    // 텍스트 데이터가 있으면 복원
    if (currentPage in textDataPerPage) {
      restoreTextData();
    }

    // 도장 데이터가 있으면 복원
    if (currentPage in stampDataPerPage) {
      restoreStampData();
    }
    
    // 서명 데이터가 있으면 복원
    if (currentPage in signatureDataPerPage) {
      restoreSignatureData();
    }

    // 페이지 로딩 완료 이벤트 디스패치
    document.dispatchEvent(new CustomEvent('pdfPageLoaded', { detail: { page: currentPage } }));

    return true;
  } catch (error) {
    console.error("PDF 로드 및 렌더링 오류:", error);
    showToast("오류", "PDF 로드 중 문제가 발생했습니다.", "error");
    return false;
  }
}

// PDF 페이지 렌더링
async function renderPage(pageNum) {
  try {
    // 페이지 번호 유효성 검사
    if (!pdfDoc || pageNum < 1 || pageNum > pdfDoc.numPages) {
      console.warn(`유효하지 않은 페이지 번호: ${pageNum}`);
      return;
    }

    console.log(`페이지 ${pageNum} 렌더링 시작`);

    // PDF 페이지 가져오기
    const page = await pdfDoc.getPage(pageNum);

    // 캔버스 요소 가져오기
    const canvas = document.getElementById('pdfCanvas');
    const ctx = canvas.getContext('2d', { willReadFrequently: true, alpha: true });

    // 드로잉 캔버스도 함께 업데이트
    const drawingCanvas = document.getElementById('drawingCanvas');
    const drawCtx = drawingCanvas.getContext('2d', { willReadFrequently: true, alpha: true });

    // 뷰포트 설정 (페이지 크기에 맞게 스케일 조정)
    const viewport = page.getViewport({ scale: 1.5 });
    canvas.width = viewport.width;
    canvas.height = viewport.height;
    
    // 드로잉 캔버스 크기도 맞춤
    drawingCanvas.width = viewport.width;
    drawingCanvas.height = viewport.height;

    // 캔버스 초기화 - 검은 배경 방지를 위해 명시적으로 초기화
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // 배경을 흰색으로 설정
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // 렌더링 작업 실행 (투명도 처리 개선)
    renderTask = page.render({
      canvasContext: ctx,
      viewport: viewport,
      background: 'transparent' // 투명 배경 설정 시도
    });

    // 렌더링 완료 대기
    await renderTask.promise;
    console.log(`페이지 ${pageNum} 렌더링 완료`);

    // 모든 캔버스 레이어를 투명하게 초기화 후 드로잉 캔버스 준비
    drawCtx.clearRect(0, 0, drawingCanvas.width, drawingCanvas.height);
    
    // 현재 페이지 상태 업데이트
    currentPage = pageNum;
    document.getElementById('currentMode').textContent = mode || '커서';
    
    // 이제 현재 페이지의 모든 데이터 복원
    setTimeout(() => {
      // 드로잉 데이터 복원
      if (typeof restoreDrawingData === 'function' && currentPage in drawingDataPerPage) {
        restoreDrawingData();
      }
      
      // 텍스트 데이터 복원
      if (typeof restoreTextData === 'function' && currentPage in textDataPerPage) {
        restoreTextData();
      }
      
      // 도장 데이터 복원
      if (typeof restoreStampData === 'function' && currentPage in stampDataPerPage) {
        restoreStampData();
      }
      
      // 서명 데이터 복원
      if (typeof restoreSignatureData === 'function' && currentPage in signatureDataPerPage) {
        restoreSignatureData();
      }
      
      console.log("페이지 데이터 복원 완료:", currentPage);
    }, 100);

    return page;
  } catch (error) {
    console.error("페이지 렌더링 오류:", error);
    showToast("오류", "페이지 렌더링 중 문제가 발생했습니다.", "error");
    return null;
  }
}

// 이전 페이지로 이동
function prevPage() {
  if (!pdfDoc || currentPage <= 1) {
    console.log("이전 페이지가 없습니다.");
    return;
  }

  console.log(`이전 페이지로 이동: ${currentPage} → ${currentPage - 1}`);
  
  // 현재 페이지의 모든 데이터 저장
  if (typeof saveCurrentPageData === 'function') {
    saveCurrentPageData();
  }
  
  // 이전 페이지로 이동
  const prevPageNum = currentPage - 1;
  
  // 이전 페이지 렌더링
  renderPage(prevPageNum);
  
  // 페이지 번호 저장
  sessionStorage.setItem("lastPage", prevPageNum);
  
  // 세션 데이터 저장
  saveSessionData();
  
  // 페이지 동기화 메시지 전송
  if (typeof sendPageSync === 'function') {
    sendPageSync(prevPageNum);
  } else {
    console.error("페이지 동기화 함수를 찾을 수 없습니다.");
  }
}

// 다음 페이지로 이동
function nextPage() {
  if (!pdfDoc || currentPage >= pdfDoc.numPages) {
    console.log("다음 페이지가 없습니다.");
    return;
  }

  console.log(`다음 페이지로 이동: ${currentPage} → ${currentPage + 1}`);
  
  // 현재 페이지의 모든 데이터 저장
  if (typeof saveCurrentPageData === 'function') {
    saveCurrentPageData();
  }
  
  // 다음 페이지로 이동
  const nextPageNum = currentPage + 1;
  
  // 다음 페이지 렌더링
  renderPage(nextPageNum);
  
  // 페이지 번호 저장
  sessionStorage.setItem("lastPage", nextPageNum);
  
  // 세션 데이터 저장
  saveSessionData();
  
  // 페이지 동기화 메시지 전송
  if (typeof sendPageSync === 'function') {
    sendPageSync(nextPageNum);
  } else {
    console.error("페이지 동기화 함수를 찾을 수 없습니다.");
  }
}

// 세션 ID 확인 유틸리티 함수
function ensurePdfSessionId() {
  // URL에서 세션 ID 가져오기 (항상 최우선)
  const urlParams = new URLSearchParams(window.location.search);
  const urlSessionId = urlParams.get('session');
  
  if (urlSessionId) {
    // URL에 세션 ID가 있으면 이를 절대적 진실로 사용
    if (sessionId !== urlSessionId) {
      console.log(`PDF 세션 ID 불일치 수정: ${sessionId} → ${urlSessionId}`);
      sessionId = urlSessionId;
      sessionStorage.setItem("sessionId", sessionId);
    }
    return sessionId;
  } else if (sessionStorage.getItem("sessionId")) {
    // URL에 없고 세션 스토리지에 있으면 사용
    sessionId = sessionStorage.getItem("sessionId");
    console.log("PDF - 세션 스토리지에서 세션 ID 가져옴:", sessionId);
    return sessionId;
  }
  
  // 세션 ID가 없으면 null 반환 (오류 상황)
  return null;
}

// 페이지 로드 시 PDF 업로드 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', function() {
  // PDF 업로드 이벤트 핸들러 등록
  const pdfUploadElement = document.getElementById('pdfUpload');
  if (pdfUploadElement) {
    pdfUploadElement.addEventListener('change', handlePdfUpload);
    console.log("PDF 업로드 이벤트 리스너 등록됨");
  } else {
    console.error("PDF 업로드 요소를 찾을 수 없습니다");
  }
});

// PDF 업로드 이벤트 핸들러
function handlePdfUpload(event) {
  // 세션 ID 확인
  const validSessionId = ensurePdfSessionId();
  if (!validSessionId) {
    console.error("유효한 세션 ID를 찾을 수 없어 PDF를 업로드할 수 없습니다.");
    showToast("업로드 오류", "세션 ID를 찾을 수 없습니다.", "error");
    return;
  }

  const file = event.target.files[0];
  if (!file || !file.type.includes('pdf')) {
    showToast("오류", "PDF 파일만 업로드할 수 있습니다.", "error");
    return;
  }

  // 토스트 메시지 표시
  showToast("파일 업로드 중", "PDF 파일을 업로드하고 처리 중입니다.", "info");

  // FormData 생성
  const formData = new FormData();
  formData.append('file', file);
  formData.append('sessionId', sessionId);

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

  console.log("PDF 업로드 시작:", file.name, "세션 ID:", sessionId);

  // 서버로 파일 업로드
  fetch('/upload/temp', {
    method: 'POST',
    headers: {
      [header]: token,
    },
    body: formData
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('파일 업로드 실패');
    }
    return response.text();
  })
  .then(data => {
    console.log("파일 업로드 성공:", data);
    
    // 새 PDF 업로드 시 모든 이전 캔버스 데이터 초기화
    if (typeof drawingDataPerPage !== 'undefined') {
      drawingDataPerPage = {};
      console.log("이전 드로잉 데이터 초기화");
    }
    
    if (typeof textDataPerPage !== 'undefined') {
      textDataPerPage = {};
      console.log("이전 텍스트 데이터 초기화");
    }
    
    if (typeof stampDataPerPage !== 'undefined') {
      stampDataPerPage = {};
      console.log("이전 도장 데이터 초기화");
    }
    
    if (typeof signatureDataPerPage !== 'undefined') {
      signatureDataPerPage = {};
      console.log("이전 서명 데이터 초기화");
    }
    
    // 로컬 스토리지의 이전 세션 데이터도 초기화
    try {
      localStorage.removeItem(`drawing_${sessionId}`);
      localStorage.removeItem(`text_${sessionId}`);
      localStorage.removeItem(`stamp_${sessionId}`);
      localStorage.removeItem(`signature_${sessionId}`);
      localStorage.removeItem(`currentPage_${sessionId}`);
      console.log("로컬 스토리지 이전 데이터 초기화 완료");
    } catch (e) {
      console.warn("로컬 스토리지 초기화 중 오류:", e);
    }
    
    // 드로잉 캔버스가 있으면 초기화
    const drawingCanvas = document.getElementById('drawingCanvas');
    if (drawingCanvas) {
      const drawCtx = drawingCanvas.getContext('2d', { alpha: true });
      drawCtx.clearRect(0, 0, drawingCanvas.width, drawingCanvas.height);
      console.log("드로잉 캔버스 초기화 완료");
    }
    
    // 타임스탬프 추가로 캐싱 방지
    const timestamp = Date.now();
    const pdfUrl = data + `?t=${timestamp}`;
    
    // 소켓으로 다른 참여자에게 알림
    notifyPdfUpload(pdfUrl);
    
    // PDF 로드 및 렌더링
    uploadedPdfUrl = pdfUrl;
    currentPage = 1; // 첫 페이지로 초기화
    loadAndRenderPDF(uploadedPdfUrl);
    
    // 성공 토스트 메시지
    showToast("업로드 완료", "PDF 파일 업로드가 완료되었습니다.", "success");
  })
  .catch(error => {
    console.error("파일 업로드 오류:", error);
    showToast("업로드 실패", "파일 업로드 중 오류가 발생했습니다.", "error");
  });
}

// PDF 업로드 알림 전송 함수
function notifyPdfUpload(pdfUrl) {
  // WebSocket 연결 상태 확인
  if (!stompClient || !stompClient.connected) {
    console.warn("WebSocket 연결이 끊어졌습니다. 재연결 시도...");
    
    // 세션 ID 확인
    const validSessionId = ensurePdfSessionId();
    if (!validSessionId) {
      console.error("유효한 세션 ID를 찾을 수 없어 알림을 전송할 수 없습니다.");
      showToast("연결 오류", "세션 ID를 찾을 수 없습니다.", "error");
      return;
    }
    
    // WebSocket 재연결
    if (typeof initializeWebSocket === 'function') {
      initializeWebSocket();
      
      // 재연결 후 일정 시간 대기 후 알림 전송 재시도
      setTimeout(() => {
        if (stompClient && stompClient.connected) {
          sendPdfNotification(pdfUrl);
        } else {
          console.error("WebSocket 재연결에 실패했습니다.");
          // 추가 재시도 (2번째)
          setTimeout(() => {
            if (stompClient && stompClient.connected) {
              sendPdfNotification(pdfUrl);
            } else {
              console.error("WebSocket 재연결 2차 시도 실패");
              showToast("연결 오류", "실시간 통신 연결에 실패했습니다. 페이지를 새로고침해주세요.", "error");
            }
          }, 3000);
        }
      }, 2000);
    } else {
      console.error("WebSocket 초기화 함수를 찾을 수 없습니다.");
      showToast("연결 오류", "WebSocket 연결 기능을 찾을 수 없습니다.", "error");
    }
  } else {
    // 정상 연결 상태이면 바로 알림 전송
    sendPdfNotification(pdfUrl);
  }
}

// PDF 알림 전송 함수
function sendPdfNotification(pdfUrl) {
  // 세션 ID 확인
  const validSessionId = ensurePdfSessionId();
  if (!validSessionId) {
    console.error("유효한 세션 ID를 찾을 수 없어 PDF 알림을 전송할 수 없습니다.");
    return;
  }
  
  try {
    // 메시지 구조 생성
    const message = {
      pdfUrl: pdfUrl,
      type: 'pdf_upload',
      sender: userRole,
      sessionId: sessionId,
      timestamp: Date.now(),
      clearPreviousData: true // 이전 데이터 초기화 플래그 추가
    };
    
    // 메시지 전송
    stompClient.send(`/topic/room/${sessionId}/pdf`, {}, JSON.stringify(message));
    console.log("PDF 업로드 알림 전송 완료:", message);
    
    // 3초 후 재확인 메시지 전송 (안정성 향상)
    setTimeout(() => {
      if (stompClient && stompClient.connected) {
        message.timestamp = Date.now(); // 타임스탬프 갱신
        message.isRetry = true; // 재시도 표시
        stompClient.send(`/topic/room/${sessionId}/pdf`, {}, JSON.stringify(message));
        console.log("PDF 알림 재확인 메시지 전송");
      }
    }, 3000);
  } catch (e) {
    console.error("PDF 알림 전송 오류:", e);
  }
}

// 서버에 세션 데이터 저장
function saveSessionData() {
  // 세션 ID 확인
  const validSessionId = ensurePdfSessionId();
  if (!validSessionId || !uploadedPdfUrl) {
    console.log("저장할 세션 데이터가 없습니다.");
    return;
  }

  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

  // 저장할 데이터 구성
  const sessionData = {
    sessionId: sessionId,
    pdfUrl: uploadedPdfUrl,
    currentPage: currentPage,
    drawingData: JSON.stringify(drawingDataPerPage),
    textData: JSON.stringify(textDataPerPage),
    stampData: JSON.stringify(stampDataPerPage),
    signatureData: JSON.stringify(signatureDataPerPage)
  };

  console.log("세션 데이터 저장 시도:", sessionId);

  // 서버로 데이터 전송
  fetch(`/api/contract/session/${sessionId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify(sessionData)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('세션 데이터 저장 실패');
    }
    return response.json();
  })
  .then(data => {
    console.log("세션 데이터 저장 성공:", data);
  })
  .catch(error => {
    console.error("세션 데이터 저장 오류:", error);
  });
}

// 스크롤 동기화 관련 변수
let isScrollSyncEnabled = true;
let isReceivingScrollSync = false;
let scrollSyncTimeout = null;

// 스크롤 동기화 초기화 함수
function initializeScrollSync() {
  const scrollWrapper = document.getElementById('scrollWrapper');
  if (!scrollWrapper) {
    console.warn("scrollWrapper 요소를 찾을 수 없습니다.");
    return;
  }

  // 상담원만 스크롤 이벤트를 전송할 수 있도록 설정
  if (userRole === 'agent') {
    scrollWrapper.addEventListener('scroll', handleAgentScroll);
    console.log("상담원 스크롤 동기화 이벤트 리스너 등록 완료");
  } else if (userRole === 'client') {
    // 고객은 스크롤을 비활성화
    disableClientScroll();
    console.log("고객 스크롤 비활성화 완료");
  }
}

// 상담원 스크롤 이벤트 처리
function handleAgentScroll(event) {
  if (!isScrollSyncEnabled || isReceivingScrollSync) {
    return;
  }

  // 스크롤 이벤트 디바운싱
  if (scrollSyncTimeout) {
    clearTimeout(scrollSyncTimeout);
  }

  scrollSyncTimeout = setTimeout(() => {
    const scrollWrapper = event.target;
    const scrollData = {
      scrollTop: scrollWrapper.scrollTop,
      scrollLeft: scrollWrapper.scrollLeft,
      scrollHeight: scrollWrapper.scrollHeight,
      scrollWidth: scrollWrapper.scrollWidth,
      clientHeight: scrollWrapper.clientHeight,
      clientWidth: scrollWrapper.clientWidth
    };

    sendScrollSync(scrollData);
  }, 50); // 50ms 디바운싱
}

// 스크롤 동기화 데이터 전송
function sendScrollSync(scrollData) {
  if (!stompClient || !stompClient.connected || !sessionId) {
    console.warn("WebSocket 연결이 없어 스크롤 동기화를 전송할 수 없습니다.");
    return;
  }

  try {
    const message = {
      type: 'scroll_sync',
      sender: userRole,
      sessionId: sessionId,
      scrollData: scrollData,
      timestamp: Date.now()
    };

    stompClient.send(`/topic/room/${sessionId}/scroll`, {}, JSON.stringify(message));
    console.log("스크롤 동기화 데이터 전송:", scrollData);
  } catch (error) {
    console.error("스크롤 동기화 전송 오류:", error);
  }
}

// 원격 스크롤 동기화 처리
function handleRemoteScrollSync(scrollData) {
  if (userRole !== 'client') {
    return; // 고객만 스크롤 동기화를 받음
  }

  const scrollWrapper = document.getElementById('scrollWrapper');
  if (!scrollWrapper) {
    console.warn("scrollWrapper 요소를 찾을 수 없습니다.");
    return;
  }

  // 무한 루프 방지
  isReceivingScrollSync = true;

  try {
    // 스크롤 위치 동기화
    scrollWrapper.scrollTop = scrollData.scrollTop;
    scrollWrapper.scrollLeft = scrollData.scrollLeft;
    
    console.log("고객 스크롤 위치 동기화 완료:", scrollData);
  } catch (error) {
    console.error("스크롤 동기화 처리 오류:", error);
  } finally {
    // 100ms 후 플래그 해제
    setTimeout(() => {
      isReceivingScrollSync = false;
    }, 100);
  }
}

// 고객 스크롤 비활성화
function disableClientScroll() {
  const scrollWrapper = document.getElementById('scrollWrapper');
  if (!scrollWrapper) {
    return;
  }

  // CSS로 스크롤 비활성화
  scrollWrapper.style.overflow = 'hidden';
  
  // 스크롤 이벤트 차단
  scrollWrapper.addEventListener('wheel', preventScroll, { passive: false });
  scrollWrapper.addEventListener('touchmove', preventScroll, { passive: false });
  scrollWrapper.addEventListener('keydown', preventScrollKeys, { passive: false });
  
  console.log("고객 스크롤 비활성화 적용 완료");
}

// 스크롤 이벤트 차단 함수
function preventScroll(event) {
  event.preventDefault();
  event.stopPropagation();
  return false;
}

// 키보드 스크롤 차단 함수
function preventScrollKeys(event) {
  const scrollKeys = [32, 33, 34, 35, 36, 37, 38, 39, 40]; // Space, Page Up/Down, Home, End, Arrow keys
  if (scrollKeys.includes(event.keyCode)) {
    event.preventDefault();
    event.stopPropagation();
    return false;
  }
}

// 고객 스크롤 활성화 (필요시 사용)
function enableClientScroll() {
  const scrollWrapper = document.getElementById('scrollWrapper');
  if (!scrollWrapper) {
    return;
  }

  // CSS 스크롤 활성화
  scrollWrapper.style.overflow = 'auto';
  
  // 이벤트 리스너 제거
  scrollWrapper.removeEventListener('wheel', preventScroll);
  scrollWrapper.removeEventListener('touchmove', preventScroll);
  scrollWrapper.removeEventListener('keydown', preventScrollKeys);
  
  console.log("고객 스크롤 활성화 완료");
} 