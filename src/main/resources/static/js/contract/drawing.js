// 드로잉 관련 변수
let drawingCanvas = null;
let drawingContext = null;
let lastX = 0;
let lastY = 0;

// 드로잉 초기화 함수
function initDrawing() {
  drawingCanvas = document.getElementById('drawingCanvas');
  drawingContext = drawingCanvas.getContext('2d');

  // 마우스 이벤트 리스너 설정
  drawingCanvas.addEventListener('mousedown', startDrawing);
  drawingCanvas.addEventListener('mousemove', draw);
  drawingCanvas.addEventListener('mouseup', stopDrawing);
  drawingCanvas.addEventListener('mouseout', stopDrawing);

  // 터치 이벤트 리스너 설정
  drawingCanvas.addEventListener('touchstart', handleTouchStart);
  drawingCanvas.addEventListener('touchmove', handleTouchMove);
  drawingCanvas.addEventListener('touchend', handleTouchEnd);
}

// 페이지 로드 시 드로잉 초기화
document.addEventListener('DOMContentLoaded', function() {
  initDrawing();
});

// 형광펜 모드 설정
function setHighlighter() {
  mode = 'highlight';
  drawing = false;
  document.getElementById('currentMode').textContent = '형광펜';
  updateToolbarButtons('highlighterBtn');
}

// 펜 모드 설정
function setPen() {
  mode = 'pen';
  drawing = false;
  document.getElementById('currentMode').textContent = '펜';
  updateToolbarButtons('penBtn');
}

// 커서 모드 설정
function setCursor() {
  mode = null;
  drawing = false;
  document.getElementById('currentMode').textContent = '커서';
  updateToolbarButtons('cursorBtn');
}

// 스탬프 모드 설정
function setStampMode() {
  mode = 'stamp';
  drawing = false;
  document.getElementById('currentMode').textContent = '도장';
  updateToolbarButtons('stampBtn');
}

// 텍스트 입력 모드 설정 및 팝업 열기
function openTextPopup() {
  if (mode === 'text') {
    closeTextPopup();
    return;
  }

  // 텍스트 모드 설정
  mode = 'text';
  document.getElementById('currentMode').textContent = '텍스트';
  updateToolbarButtons('textBtn');

  // 팝업 초기화 및 표시
  document.getElementById('textInput').value = '';
  pendingText = null;
  document.getElementById('textPopup').style.display = 'block';
}

// 텍스트 팝업 닫기
function closeTextPopup() {
  document.getElementById('textPopup').style.display = 'none';
  setCursor(); // 커서 모드로 돌아가기
}

// 텍스트 확인 및 추가
function confirmText() {
  const textInput = document.getElementById('textInput').value.trim();
  if (!textInput) {
    alert('텍스트를 입력해주세요.');
    return;
  }

  // 텍스트를 캔버스에 추가하기 위한 클릭 이벤트 대기
  pendingText = textInput;
  closeTextPopup();

  // 사용자에게 안내 메시지 표시
  showToast("위치 선택", "텍스트를 추가할 위치를 클릭하세요.", "info");
}

// 도구 모음 버튼 업데이트
function updateToolbarButtons(activeButtonId) {
  // 모든 버튼 비활성화
  const buttons = ['highlighterBtn', 'penBtn', 'cursorBtn', 'textBtn', 'stampBtn'];
  buttons.forEach(id => {
    const button = document.getElementById(id);
    if (button) {
      button.classList.remove('active');
    }
  });

  // 선택된 버튼만 활성화
  const activeButton = document.getElementById(activeButtonId);
  if (activeButton) {
    activeButton.classList.add('active');
  }
}

// 드로잉 시작
function startDrawing(e) {
  if (!mode || mode === 'text' || mode === 'stamp') return;

  // 마우스 좌표 계산
  const rect = drawingCanvas.getBoundingClientRect();
  lastX = e.clientX - rect.left;
  lastY = e.clientY - rect.top;
  
  drawing = true;

  // 스타일 설정
  drawingContext.lineJoin = 'round';
  drawingContext.lineCap = 'round';
  
  if (mode === 'highlight') {
    drawingContext.strokeStyle = 'rgba(255, 255, 0, 0.5)';
    drawingContext.lineWidth = 15;
    drawingContext.globalCompositeOperation = 'multiply';
  } else if (mode === 'pen') {
    drawingContext.strokeStyle = '#0064E1';
    drawingContext.lineWidth = 2;
    drawingContext.globalCompositeOperation = 'source-over';
  }

  // 패스 시작
  drawingContext.beginPath();
  drawingContext.moveTo(lastX, lastY);
}

// 드로잉 중
function draw(e) {
  if (!drawing) return;
  
  // 마우스 위치 계산
  const rect = drawingCanvas.getBoundingClientRect();
  const currentX = e.clientX - rect.left;
  const currentY = e.clientY - rect.top;
  
  // 선 그리기
  drawingContext.lineTo(currentX, currentY);
  drawingContext.stroke();
  
  // WebSocket으로 드로잉 데이터 전송
  if (stompClient && stompClient.connected) {
    const drawingData = {
      type: mode,
      lastX: lastX,
      lastY: lastY,
      currentX: currentX,
      currentY: currentY,
      page: currentPage,
      sessionId: sessionId
    };
    
    stompClient.send(`/app/room/${sessionId}/draw`, {}, JSON.stringify(drawingData));
  }
  
  // 좌표 업데이트
  lastX = currentX;
  lastY = currentY;
}

// 드로잉 종료
function stopDrawing() {
  if (!drawing) return;
  
  drawing = false;
  
  // 현재 페이지 드로잉 데이터 저장
  saveDrawingData();
  
  // 세션 데이터 저장
  saveSessionData();
}

// 터치 이벤트 처리 함수들
function handleTouchStart(e) {
  e.preventDefault();
  
  if (!mode || mode === 'text' || mode === 'stamp') {
    // 텍스트 모드에서 터치 시 텍스트 추가
    if (mode === 'text' && pendingText) {
      handleTextPlacement(e.touches[0]);
    }
    // 도장 모드에서 터치 시 도장 추가
    else if (mode === 'stamp') {
      handleStampPlacement(e.touches[0]);
    }
    return;
  }
  
  const touch = e.touches[0];
  const rect = drawingCanvas.getBoundingClientRect();
  lastX = touch.clientX - rect.left;
  lastY = touch.clientY - rect.top;
  
  drawing = true;
  
  // 스타일 설정
  drawingContext.lineJoin = 'round';
  drawingContext.lineCap = 'round';
  
  if (mode === 'highlight') {
    drawingContext.strokeStyle = 'rgba(255, 255, 0, 0.5)';
    drawingContext.lineWidth = 15;
    drawingContext.globalCompositeOperation = 'multiply';
  } else if (mode === 'pen') {
    drawingContext.strokeStyle = '#0064E1';
    drawingContext.lineWidth = 2;
    drawingContext.globalCompositeOperation = 'source-over';
  }
  
  drawingContext.beginPath();
  drawingContext.moveTo(lastX, lastY);
}

function handleTouchMove(e) {
  e.preventDefault();
  
  if (!drawing) return;
  
  const touch = e.touches[0];
  const rect = drawingCanvas.getBoundingClientRect();
  const currentX = touch.clientX - rect.left;
  const currentY = touch.clientY - rect.top;
  
  drawingContext.lineTo(currentX, currentY);
  drawingContext.stroke();
  
  // WebSocket으로 드로잉 데이터 전송
  if (stompClient && stompClient.connected) {
    const drawingData = {
      type: mode,
      lastX: lastX,
      lastY: lastY,
      currentX: currentX,
      currentY: currentY,
      page: currentPage,
      sessionId: sessionId
    };
    
    stompClient.send(`/app/room/${sessionId}/draw`, {}, JSON.stringify(drawingData));
  }
  
  lastX = currentX;
  lastY = currentY;
}

function handleTouchEnd(e) {
  e.preventDefault();
  stopDrawing();
}

// 텍스트 배치 처리
function handleTextPlacement(event) {
  if (!pendingText) return;
  
  const rect = drawingCanvas.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  
  // 텍스트 스타일 설정
  drawingContext.font = '16px Arial';
  drawingContext.fillStyle = '#0064E1';
  drawingContext.globalCompositeOperation = 'source-over';
  
  // 텍스트 그리기
  drawingContext.fillText(pendingText, x, y);
  
  // 텍스트 데이터 저장
  if (!textDataPerPage[currentPage]) {
    textDataPerPage[currentPage] = [];
  }
  
  const textData = {
    text: pendingText,
    x: x,
    y: y,
    font: '16px Arial',
    color: '#0064E1'
  };
  
  textDataPerPage[currentPage].push(textData);
  
  // WebSocket으로 텍스트 데이터 전송
  if (stompClient && stompClient.connected) {
    const textMessage = {
      type: 'text',
      text: pendingText,
      x: x,
      y: y,
      page: currentPage,
      sessionId: sessionId
    };
    
    stompClient.send(`/app/room/${sessionId}/text`, {}, JSON.stringify(textMessage));
  }
  
  // 텍스트 입력 모드 초기화
  pendingText = null;
  
  // 세션 데이터 저장
  saveTextData();
  saveSessionData();
  
  // 커서 모드로 되돌리기
  setCursor();
}

// 도장 배치 처리
function handleStampPlacement(event) {
  if (mode !== 'stamp') return;
  
  const rect = drawingCanvas.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  
  // 도장 스타일 설정
  const stampImage = new Image();
  stampImage.src = '/images/stamp.png';
  
  // 도장 이미지가 로드되면 그리기
  stampImage.onload = function() {
    const stampWidth = 100;
    const stampHeight = 100;
    drawingContext.drawImage(stampImage, x - stampWidth/2, y - stampHeight/2, stampWidth, stampHeight);
    
    // 도장 데이터 저장
    if (!stampDataPerPage[currentPage]) {
      stampDataPerPage[currentPage] = [];
    }
    
    const stampData = {
      x: x - stampWidth/2,
      y: y - stampHeight/2,
      width: stampWidth,
      height: stampHeight
    };
    
    stampDataPerPage[currentPage].push(stampData);
    
    // WebSocket으로 도장 데이터 전송
    if (stompClient && stompClient.connected) {
      const stampMessage = {
        type: 'stamp',
        x: x - stampWidth/2,
        y: y - stampHeight/2,
        width: stampWidth,
        height: stampHeight,
        page: currentPage,
        sessionId: sessionId
      };
      
      stompClient.send(`/app/room/${sessionId}/stamp`, {}, JSON.stringify(stampMessage));
    }
    
    // 세션 데이터 저장
    saveStampData();
    saveSessionData();
    
    // 커서 모드로 되돌리기
    setCursor();
  };
}

// 드로잉 캔버스 클릭 이벤트 (텍스트 및 도장 배치용)
drawingCanvas.addEventListener('click', function(e) {
  if (mode === 'text' && pendingText) {
    handleTextPlacement(e);
  } else if (mode === 'stamp') {
    handleStampPlacement(e);
  }
});

// 드로잉 데이터 저장
function saveDrawingData() {
  // 현재 캔버스 이미지 데이터를 저장
  const imageData = drawingContext.getImageData(0, 0, drawingCanvas.width, drawingCanvas.height);
  drawingDataPerPage[currentPage] = imageData;
}

// 텍스트 데이터 저장
function saveTextData() {
  // 텍스트 데이터는 이미 텍스트 배치 시 저장됨
  console.log("텍스트 데이터 저장:", textDataPerPage);
}

// 도장 데이터 저장
function saveStampData() {
  // 도장 데이터는 이미 도장 배치 시 저장됨
  console.log("도장 데이터 저장:", stampDataPerPage);
}

// 드로잉 데이터 복원
function restoreDrawingData() {
  const imageData = drawingDataPerPage[currentPage];
  if (imageData) {
    drawingContext.putImageData(imageData, 0, 0);
  }
}

// 텍스트 데이터 복원
function restoreTextData() {
  const textData = textDataPerPage[currentPage];
  if (!textData || !textData.length) return;
  
  textData.forEach(item => {
    drawingContext.font = item.font || '16px Arial';
    drawingContext.fillStyle = item.color || '#0064E1';
    drawingContext.globalCompositeOperation = 'source-over';
    drawingContext.fillText(item.text, item.x, item.y);
  });
}

// 도장 데이터 복원
function restoreStampData() {
  const stampData = stampDataPerPage[currentPage];
  if (!stampData || !stampData.length) return;
  
  const stampImage = new Image();
  stampImage.src = '/images/stamp.png';
  
  stampImage.onload = function() {
    stampData.forEach(item => {
      drawingContext.drawImage(stampImage, item.x, item.y, item.width, item.height);
    });
  };
}

// 원격 드로잉 처리 (WebSocket으로 수신한 드로잉 데이터 처리)
function handleRemoteDrawing(data) {
  if (data.page !== currentPage) return;
  
  // 원격 드로잉을 현재 캔버스에 적용
  if (data.type === 'highlight') {
    drawingContext.strokeStyle = 'rgba(255, 255, 0, 0.5)';
    drawingContext.lineWidth = 15;
    drawingContext.globalCompositeOperation = 'multiply';
  } else if (data.type === 'pen') {
    drawingContext.strokeStyle = '#0064E1';
    drawingContext.lineWidth = 2;
    drawingContext.globalCompositeOperation = 'source-over';
  }
  
  drawingContext.lineJoin = 'round';
  drawingContext.lineCap = 'round';
  
  drawingContext.beginPath();
  drawingContext.moveTo(data.lastX, data.lastY);
  drawingContext.lineTo(data.currentX, data.currentY);
  drawingContext.stroke();
  
  // 드로잉 데이터 저장
  saveDrawingData();
}

// 원격 텍스트 처리
function handleRemoteText(data) {
  if (data.page !== currentPage) return;
  
  // 원격 텍스트를 현재 캔버스에 적용
  drawingContext.font = '16px Arial';
  drawingContext.fillStyle = '#0064E1';
  drawingContext.globalCompositeOperation = 'source-over';
  drawingContext.fillText(data.text, data.x, data.y);
  
  // 텍스트 데이터 저장
  if (!textDataPerPage[currentPage]) {
    textDataPerPage[currentPage] = [];
  }
  
  textDataPerPage[currentPage].push({
    text: data.text,
    x: data.x,
    y: data.y,
    font: '16px Arial',
    color: '#0064E1'
  });
  
  // 텍스트 데이터 저장
  saveTextData();
}

// 원격 도장 처리
function handleRemoteStamp(data) {
  if (data.page !== currentPage) return;
  
  // 원격 도장을 현재 캔버스에 적용
  const stampImage = new Image();
  stampImage.src = '/images/stamp.png';
  
  stampImage.onload = function() {
    drawingContext.drawImage(stampImage, data.x, data.y, data.width, data.height);
    
    // 도장 데이터 저장
    if (!stampDataPerPage[currentPage]) {
      stampDataPerPage[currentPage] = [];
    }
    
    stampDataPerPage[currentPage].push({
      x: data.x,
      y: data.y,
      width: data.width,
      height: data.height
    });
    
    // 도장 데이터 저장
    saveStampData();
  };
} 