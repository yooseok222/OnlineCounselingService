document.addEventListener('DOMContentLoaded', () => {
  const params  = new URLSearchParams(window.location.search);
  const rawRoom = params.get('room');

  // room (contractId) 파라미터 없으면 alert
  if (!rawRoom) {
    Swal.fire({
      icon: 'warning',
      title: '방 번호가 없습니다.',
      text: '올바른 경로로 접속해주세요.',
      confirmButtonText: '확인'
    }).then(() => window.location.href = '/');
    throw new Error('Missing room parameter');
  }

  // 숫자 변환 검증
  const roomId = parseInt(rawRoom, 10);
    if (isNaN(roomId)) {
      Swal.fire({
        icon: 'error',
        title: '잘못된 방 번호입니다.',
        text: '올바른 경로로 접속해주세요.',
        confirmButtonText: '확인'
      }).then(() => window.location.href = '/');
      throw new Error('Invalid room parameter');
    }

  // 화면에 방번호(contractId) 표시
  document.getElementById('roomLabel').textContent = roomId;

  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);

  stompClient.connect({}, () => {
    console.log('STOMP 연결 성공');

    // 해당 방 구독
    stompClient.subscribe(`/topic/chat/${roomId}`, frame => {
      const msg = JSON.parse(frame.body);
      console.log('▶ 파싱된 메시지:', msg);
      showMessage(msg);
      if (msg.type === 'END') {
        console.log('[DEBUG] END detected, calling handleExport()');
        handleExport();
      }
    });

    // "JOIN" 알림 보내기
    stompClient.send('/app/chat.addUser', {},
      JSON.stringify({ roomId, content: '님이 입장했습니다.', type: 'JOIN' })
    );
    console.log('▶ 입장 알림 전송');
  });

     // 전송 버튼 클릭 시
    document.getElementById('sendBtn').addEventListener('click', () => {
        const inputEl = document.getElementById('msgInput');
        const text    = inputEl.value.trim();

        if (!text) return;

        // 3) STOMP로 전송
        stompClient.send('/app/chat.sendMessage', {},
          JSON.stringify({ roomId, content: text, type: 'CHAT' })
        );

        inputEl.value = '';
   });

  // enter 키로도 전송 가능
  document.getElementById('msgInput').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('sendBtn').click();
  });

  // 채팅 종료 & 파일화
  document.getElementById('endBtn').addEventListener('click', () => {
      stompClient.send('/app/chat.endCall', {}, JSON.stringify({ roomId }));
      ['msgInput','sendBtn','endBtn'].forEach(id => document.getElementById(id).disabled = true);
  });

});

// 메시지 표시
function showMessage(msg) {
  console.log('▶ showMessage 호출, msg =', msg);
  const area = document.getElementById('chatArea');
  const div = document.createElement('div');
  div.classList.add('message');

  if (msg.type === 'JOIN' || msg.type === 'END') {
    div.innerHTML = `<em>${msg.sender} ${msg.content}</em>`;
  } else {
    const time = new Date(msg.sendTime).toLocaleTimeString();
    div.innerHTML = `<span class="sender">[${time}] ${msg.sender}:</span>
                     <span class="content">${msg.content}</span>`;
  }

  area.appendChild(div);
  area.scrollTop = area.scrollHeight;
}

// 상담 내역 다운로드 처리
function handleExport() {
  const roomId = document.getElementById('roomLabel').textContent;

  const xsrfCookie = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='));
  const xsrfToken = xsrfCookie && xsrfCookie.split('=')[1];

  fetch(`/api/chat/export/${roomId}`, {
      method: 'GET',
      credentials: 'same-origin',
      headers: { 'X-XSRF-TOKEN': xsrfToken }
  })
  .then(res => res.blob())
  .then(blob => {
    const url = URL.createObjectURL(blob);
    const link = document.getElementById('exportLink');
    link.href = url;
    link.download = `chat_${roomId}.txt`;
    link.style.display = 'inline';
    link.textContent = '상담 내역 다운로드';
  })
  .catch(err => console.error('파일 다운로드 실패:', err));
}