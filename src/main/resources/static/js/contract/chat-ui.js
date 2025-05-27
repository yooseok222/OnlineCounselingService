// 채팅 UI 관리 스크립트

let chatContainer = null;
let chatMessages = null;
let chatInput = null;
let isMinimized = false;

// 채팅 UI 초기화
function initializeChatUI() {
  console.log("채팅 UI 초기화 시작");
  
  // 이미 채팅 UI가 있으면 제거
  const existingChat = document.getElementById('chatContainer');
  if (existingChat) {
    existingChat.remove();
  }
  
  // 채팅 UI HTML 생성
  const chatHTML = `
    <div id="chatContainer" class="chat-container">
      <div class="chat-header">
        <div class="chat-title">
          <i class="fas fa-comments"></i>
          <span>실시간 채팅</span>
        </div>
        <div class="chat-controls">
          <button id="chatMinimizeBtn" class="chat-btn" onclick="toggleChatMinimize()">
            <i class="fas fa-minus"></i>
          </button>
          <button id="chatCloseBtn" class="chat-btn" onclick="closeChatUI()">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
      <div id="chatBody" class="chat-body">
        <div id="chatMessages" class="chat-messages">
          <div class="chat-message system">
            <div class="message-content">
              <span class="message-text">채팅이 시작되었습니다.</span>
            </div>
          </div>
        </div>
        <div class="chat-input-container">
          <input type="text" id="chatInput" class="chat-input" placeholder="메시지를 입력하세요..." maxlength="500">
          <button id="chatSendBtn" class="chat-send-btn" onclick="sendChatFromUI()">
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </div>
  `;
  
  // 채팅 UI를 body에 추가
  document.body.insertAdjacentHTML('beforeend', chatHTML);
  
  // 채팅 UI 스타일 추가
  addChatUIStyles();
  
  // 채팅 UI 요소 참조 저장
  chatContainer = document.getElementById('chatContainer');
  chatMessages = document.getElementById('chatMessages');
  chatInput = document.getElementById('chatInput');
  
  // 채팅 입력 이벤트 설정
  if (chatInput) {
    chatInput.addEventListener('keypress', function(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendChatFromUI();
      }
    });
    
    // 포커스 시 스크롤 하단으로
    chatInput.addEventListener('focus', function() {
      setTimeout(() => scrollChatToBottom(), 100);
    });
  }
  
  console.log("채팅 UI 초기화 완료");
}

// 채팅 UI 스타일 추가
function addChatUIStyles() {
  // 이미 스타일이 추가되어 있는지 확인
  if (document.getElementById('chatUIStyles')) {
    return;
  }
  
  const style = document.createElement('style');
  style.id = 'chatUIStyles';
  style.textContent = `
    .chat-container {
      position: fixed;
      bottom: 20px;
      right: 20px;
      width: 350px;
      height: 450px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      transition: all 0.3s ease;
    }
    
    .chat-container.minimized {
      height: 50px;
    }
    
    .chat-header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 12px 16px;
      border-radius: 12px 12px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
    }
    
    .chat-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      font-size: 14px;
    }
    
    .chat-controls {
      display: flex;
      gap: 4px;
    }
    
    .chat-btn {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      width: 24px;
      height: 24px;
      border-radius: 4px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      transition: background 0.2s;
    }
    
    .chat-btn:hover {
      background: rgba(255, 255, 255, 0.3);
    }
    
    .chat-body {
      height: calc(100% - 50px);
      display: flex;
      flex-direction: column;
    }
    
    .chat-container.minimized .chat-body {
      display: none;
    }
    
    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      max-height: 350px;
    }
    
    .chat-message {
      margin-bottom: 12px;
      display: flex;
      flex-direction: column;
    }
    
    .chat-message.own {
      align-items: flex-end;
    }
    
    .chat-message.other {
      align-items: flex-start;
    }
    
    .chat-message.system {
      align-items: center;
    }
    
    .message-header {
      font-size: 11px;
      color: #666;
      margin-bottom: 4px;
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .message-content {
      max-width: 80%;
      padding: 8px 12px;
      border-radius: 12px;
      word-wrap: break-word;
    }
    
    .chat-message.own .message-content {
      background: #667eea;
      color: white;
      border-bottom-right-radius: 4px;
    }
    
    .chat-message.other .message-content {
      background: #f1f3f4;
      color: #333;
      border-bottom-left-radius: 4px;
    }
    
    .chat-message.system .message-content {
      background: #e8f4f8;
      color: #666;
      border-radius: 16px;
      padding: 6px 12px;
      font-size: 12px;
    }
    
    .message-text {
      font-size: 13px;
      line-height: 1.4;
    }
    
    .message-time {
      font-size: 10px;
      color: #999;
      margin-top: 2px;
    }
    
    .chat-input-container {
      padding: 12px 16px;
      border-top: 1px solid #e0e0e0;
      display: flex;
      gap: 8px;
      align-items: center;
    }
    
    .chat-input {
      flex: 1;
      border: 1px solid #ddd;
      border-radius: 20px;
      padding: 8px 16px;
      font-size: 13px;
      outline: none;
      transition: border-color 0.2s;
    }
    
    .chat-input:focus {
      border-color: #667eea;
    }
    
    .chat-send-btn {
      background: #667eea;
      color: white;
      border: none;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;
    }
    
    .chat-send-btn:hover {
      background: #5a6fd8;
    }
    
    .chat-send-btn:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
    
    /* 스크롤바 스타일 */
    .chat-messages::-webkit-scrollbar {
      width: 4px;
    }
    
    .chat-messages::-webkit-scrollbar-track {
      background: #f1f1f1;
    }
    
    .chat-messages::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 2px;
    }
    
    .chat-messages::-webkit-scrollbar-thumb:hover {
      background: #a1a1a1;
    }
    
    /* 반응형 디자인 */
    @media (max-width: 768px) {
      .chat-container {
        width: calc(100vw - 20px);
        height: 400px;
        right: 10px;
        bottom: 10px;
      }
    }
  `;
  
  document.head.appendChild(style);
}

// 채팅 메시지를 UI에 추가하는 함수
function addChatMessageToUI(messageData) {
  if (!chatMessages) {
    console.warn("채팅 메시지 컨테이너를 찾을 수 없습니다.");
    return;
  }
  
  const isOwnMessage = messageData.sender === userRole;
  const messageClass = messageData.type === 'SYSTEM' ? 'system' : 
                      isOwnMessage ? 'own' : 'other';
  
  const timestamp = messageData.timestamp ? 
    new Date(messageData.timestamp).toLocaleTimeString('ko-KR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    }) : 
    new Date().toLocaleTimeString('ko-KR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  
  const senderName = messageData.senderName || 
    (messageData.sender === 'agent' ? '상담원' : 
     messageData.sender === 'client' ? '고객' : '시스템');
  
  const messageHTML = `
    <div class="chat-message ${messageClass}">
      ${messageClass !== 'system' && messageClass !== 'own' ? 
        `<div class="message-header">
          <span class="sender-name">${senderName}</span>
          <span class="message-time">${timestamp}</span>
        </div>` : ''}
      <div class="message-content">
        <span class="message-text">${escapeHtml(messageData.content)}</span>
        ${messageClass === 'own' ? `<div class="message-time">${timestamp}</div>` : ''}
      </div>
    </div>
  `;
  
  chatMessages.insertAdjacentHTML('beforeend', messageHTML);
  scrollChatToBottom();
  
  // 새 메시지 알림 효과 (최소화 상태일 때)
  if (isMinimized && !isOwnMessage) {
    showChatNotification();
  }
}

// UI에서 채팅 메시지 전송
function sendChatFromUI() {
  if (!chatInput) {
    console.error("채팅 입력 필드를 찾을 수 없습니다.");
    return;
  }
  
  const content = chatInput.value.trim();
  if (content === "") {
    return;
  }
  
  // WebSocket을 통해 메시지 전송
  if (typeof sendChatMessage === 'function') {
    sendChatMessage(content);
    chatInput.value = '';
  } else {
    console.error("sendChatMessage 함수를 찾을 수 없습니다.");
  }
}

// 채팅창 최소화/최대화 토글
function toggleChatMinimize() {
  if (!chatContainer) return;
  
  isMinimized = !isMinimized;
  chatContainer.classList.toggle('minimized', isMinimized);
  
  const minimizeBtn = document.getElementById('chatMinimizeBtn');
  if (minimizeBtn) {
    minimizeBtn.querySelector('i').className = isMinimized ? 'fas fa-plus' : 'fas fa-minus';
  }
  
  if (!isMinimized) {
    scrollChatToBottom();
    // 포커스를 입력 필드로
    if (chatInput) {
      setTimeout(() => chatInput.focus(), 100);
    }
  }
}

// 채팅 UI 닫기
function closeChatUI() {
  if (chatContainer) {
    chatContainer.remove();
    chatContainer = null;
    chatMessages = null;
    chatInput = null;
  }
}

// 채팅창 하단으로 스크롤
function scrollChatToBottom() {
  if (chatMessages) {
    setTimeout(() => {
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }, 50);
  }
}

// 새 메시지 알림 (최소화 상태)
function showChatNotification() {
  if (!chatContainer) return;
  
  chatContainer.style.animation = 'none';
  setTimeout(() => {
    chatContainer.style.animation = 'pulse 0.5s ease-in-out 2';
  }, 10);
}

// HTML 이스케이프 함수
function escapeHtml(text) {
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  };
  return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// 페이지 로드 시 채팅 UI 자동 초기화 (필요시)
document.addEventListener('DOMContentLoaded', function() {
  // WebSocket 연결 후 채팅 UI 초기화하도록 이벤트 리스너 등록
  document.addEventListener('websocketConnected', function() {
    setTimeout(() => {
      console.log("WebSocket 연결 완료 - 채팅 UI 초기화");
      initializeChatUI();
    }, 1000);
  });
});

// CSS 애니메이션 추가
const pulseAnimation = document.createElement('style');
pulseAnimation.textContent = `
  @keyframes pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.05); }
    100% { transform: scale(1); }
  }
`;
document.head.appendChild(pulseAnimation); 