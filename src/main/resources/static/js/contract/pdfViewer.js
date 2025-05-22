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

    // PDF 문서 로드
    try {
      pdfDoc = await pdfjsLib.getDocument(pdfUrl).promise;
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
    const ctx = canvas.getContext('2d');

    // 드로잉 캔버스도 함께 업데이트
    const drawingCanvas = document.getElementById('drawingCanvas');
    const drawCtx = drawingCanvas.getContext('2d');

    // 뷰포트 설정 (페이지 크기에 맞게 스케일 조정)
    const viewport = page.getViewport({ scale: 1.5 });
    canvas.width = viewport.width;
    canvas.height = viewport.height;
    
    // 드로잉 캔버스 크기도 맞춤
    drawingCanvas.width = viewport.width;
    drawingCanvas.height = viewport.height;

    // 렌더링 작업 실행
    renderTask = page.render({
      canvasContext: ctx,
      viewport: viewport
    });

    // 렌더링 완료 대기
    await renderTask.promise;
    console.log(`페이지 ${pageNum} 렌더링 완료`);

    // 현재 페이지 상태 업데이트
    currentPage = pageNum;
    document.getElementById('currentMode').textContent = mode || '커서';

    return page;
  } catch (error) {
    console.error("페이지 렌더링 오류:", error);
    showToast("오류", "페이지 렌더링 중 문제가 발생했습니다.", "error");
    return null;
  }
}

// 이전 페이지로 이동
function prevPage() {
  if (!pdfDoc || currentPage <= 1) return;
  
  // 현재 데이터 저장
  saveDrawingData();
  saveTextData();
  saveStampData();
  
  // 이전 페이지 렌더링
  renderPage(currentPage - 1);
  
  // 페이지 번호 저장
  sessionStorage.setItem("lastPage", currentPage - 1);
  
  // 세션 데이터 저장
  saveSessionData();
}

// 다음 페이지로 이동
function nextPage() {
  if (!pdfDoc || currentPage >= pdfDoc.numPages) return;
  
  // 현재 데이터 저장
  saveDrawingData();
  saveTextData();
  saveStampData();
  
  // 다음 페이지 렌더링
  renderPage(currentPage + 1);
  
  // 페이지 번호 저장
  sessionStorage.setItem("lastPage", currentPage + 1);
  
  // 세션 데이터 저장
  saveSessionData();
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
  fetch('/upload', {
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
      timestamp: Date.now()
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
    stampData: JSON.stringify(stampDataPerPage)
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