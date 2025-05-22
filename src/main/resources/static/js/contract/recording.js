// 녹음 관련 기능

// 녹음 관련 변수
let mediaRecorder = null;
let recordedChunks = [];
let audioContext = null;
let isRecording = false;

// 녹음 초기화
function initializeRecording() {
  const startRecButton = document.getElementById('startRec');
  const stopRecButton = document.getElementById('stopRec');
  
  // 버튼 이벤트 리스너 등록
  if (startRecButton) {
    startRecButton.addEventListener('click', startRecording);
  }
  
  if (stopRecButton) {
    stopRecButton.addEventListener('click', stopRecording);
  }
}

// 녹음 시작
async function startRecording() {
  try {
    console.log("녹음 시작 시도...");
    
    // 이미 녹음 중이면 중단
    if (isRecording) {
      console.warn("이미 녹음 중입니다.");
      return;
    }
    
    // 오디오 스트림 획득
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    
    // MediaRecorder 생성
    mediaRecorder = new MediaRecorder(stream, {
      mimeType: 'audio/webm;codecs=opus'
    });
    
    // 데이터 이벤트 핸들러
    mediaRecorder.ondataavailable = function(e) {
      if (e.data.size > 0) {
        recordedChunks.push(e.data);
      }
    };
    
    // 녹음 완료 이벤트 핸들러
    mediaRecorder.onstop = function() {
      console.log("녹음 완료, 데이터 처리 중...");
      
      // 녹음 데이터를 Blob으로 변환
      const blob = new Blob(recordedChunks, {
        type: 'audio/webm;codecs=opus'
      });
      
      // 녹음 데이터 업로드
      uploadRecording(blob);
      
      // 녹음 버퍼 초기화
      recordedChunks = [];
      
      // 녹음 상태 업데이트
      isRecording = false;
      
      // 버튼 상태 업데이트
      updateRecordingButtons();
    };
    
    // 녹음 시작
    mediaRecorder.start();
    
    // 녹음 상태 업데이트
    isRecording = true;
    
    // 버튼 상태 업데이트
    updateRecordingButtons();
    
    // 토스트 메시지 표시
    showToast("녹음 시작", "상담 내용이 녹음되고 있습니다.", "info");
    
    console.log("녹음이 시작되었습니다.");
  } catch (error) {
    console.error("녹음 시작 오류:", error);
    showToast("녹음 오류", "마이크 접근에 실패했습니다. 브라우저 설정을 확인해주세요.", "error");
  }
}

// 녹음 중지
function stopRecording() {
  if (!mediaRecorder || !isRecording) {
    console.warn("녹음 중이 아닙니다.");
    return;
  }
  
  console.log("녹음 중지 중...");
  
  // MediaRecorder 중지
  mediaRecorder.stop();
  
  // 트랙 중지
  if (mediaRecorder.stream) {
    mediaRecorder.stream.getTracks().forEach(track => track.stop());
  }
  
  // 토스트 메시지 표시
  showToast("녹음 완료", "녹음이 완료되었습니다. 파일을 업로드 중입니다.", "success");
}

// 녹음 파일 업로드
function uploadRecording(blob) {
  // FormData 생성
  const formData = new FormData();
  formData.append('file', blob, `recording_${sessionId}_${Date.now()}.webm`);
  formData.append('sessionId', sessionId);
  
  // CSRF 토큰 가져오기
  const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
  const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
  
  console.log("녹음 파일 업로드 중...");
  
  // 서버로 파일 업로드
  fetch('/api/contract/upload-recording', {
    method: 'POST',
    headers: {
      [header]: token
    },
    body: formData
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('녹음 파일 업로드 실패');
    }
    return response.json();
  })
  .then(data => {
    console.log("녹음 파일 업로드 성공:", data);
    showToast("업로드 완료", "녹음 파일이 성공적으로 업로드되었습니다.", "success");
  })
  .catch(error => {
    console.error("녹음 파일 업로드 오류:", error);
    showToast("업로드 실패", "녹음 파일 업로드 중 오류가 발생했습니다.", "error");
  });
}

// 녹음 버튼 상태 업데이트
function updateRecordingButtons() {
  const startRecButton = document.getElementById('startRec');
  const stopRecButton = document.getElementById('stopRec');
  
  if (startRecButton) {
    startRecButton.disabled = isRecording;
  }
  
  if (stopRecButton) {
    stopRecButton.disabled = !isRecording;
  }
}

// 페이지 로드 시 녹음 초기화
document.addEventListener('DOMContentLoaded', function() {
  initializeRecording();
}); 