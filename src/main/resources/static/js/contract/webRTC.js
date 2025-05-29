// 간결한 WebRTC 구현 - 양방향 비디오 통화용
let localStream = null;
let remoteStream = null;
let peerConnection = null;
let connectionAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

// 네트워크 및 연결 상태 관리
let networkQuality = 'unknown'; // unknown, poor, medium, good
let lastIceConnectionState = '';
let connectionStabilityTimer = null;

// 미디어 상태 관리
let isMicrophoneEnabled = true;
let isCameraEnabled = true;

// 로깅 향상
function log(msg) {
  console.log(`[WebRTC] ${msg}`);
}

// 개선된 ICE 서버 설정 - 더 많은 TURN 서버 추가
const iceConfig = {
  iceServers: [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
    { urls: "stun:stun2.l.google.com:19302" },
    { urls: "stun:stun3.l.google.com:19302" },
    {
      urls: "turn:openrelay.metered.ca:80",
      username: "openrelayproject",
      credential: "openrelayproject"
    },
    {
      urls: "turn:openrelay.metered.ca:443",
      username: "openrelayproject",
      credential: "openrelayproject"
    },
    {
      urls: "turn:openrelay.metered.ca:443?transport=tcp",
      username: "openrelayproject",
      credential: "openrelayproject"
    },
    // 추가 TURN 서버
    {
      urls: "turn:relay.metered.ca:80",
      username: "83eab0200d0d4803811994e8",
      credential: "OBICwSPArPl5UxE6"
    },
    {
      urls: "turn:relay.metered.ca:443",
      username: "83eab0200d0d4803811994e8",
      credential: "OBICwSPArPl5UxE6"
    },
    {
      urls: "turn:relay.metered.ca:443?transport=tcp",
      username: "83eab0200d0d4803811994e8",
      credential: "OBICwSPArPl5UxE6"
    }
  ],
  iceCandidatePoolSize: 10,
  iceTransportPolicy: 'all'
};

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
  log("WebRTC 초기화 페이지 로드됨");
  
  // WebRTC 초기화
  setTimeout(initWebRTC, 500);  // 0.5초로 단축
  
  // 5초 후 자동 복구 메커니즘 시작
  setTimeout(() => {
    const remoteVideo = document.getElementById('remoteVideo');
    
    // 원격 비디오 상태 확인
    if (remoteVideo) {
      const hasStream = remoteVideo.srcObject !== null;
      const hasTracks = hasStream && remoteVideo.srcObject.getTracks().length > 0;
      const hasVideoTracks = hasStream && remoteVideo.srcObject.getVideoTracks().length > 0;
      
      log(`자동 점검: 원격 비디오 스트림=${hasStream}, 트랙=${hasTracks}, 비디오트랙=${hasVideoTracks}`);
      
      // 원격 비디오가 없거나 문제가 있는 경우 자동 복구
      if (!hasStream || !hasVideoTracks) {
        log("자동 복구: 원격 비디오 문제 감지, 강제 연결 문제 해결 시작");
        
        // 문제 해결 버튼 생성
        createFixButton();
        
        // 강제 문제 해결 시도
        forceSolveConnectionIssue();
        
        // 추가 안정성을 위해 3초 후 한 번 더 시도
        setTimeout(() => {
          log("추가 안정성: 3초 후 두 번째 복구 시도");
          forceApplyRemoteStream();
        }, 3000);
      }
    }
  }, 5000); // 페이지 로드 후 5초 대기 (더 짧게 단축)
  
  // 원격 비디오 요소에 직접 클릭 이벤트 추가
  setTimeout(() => {
    const remoteVideo = document.getElementById('remoteVideo');
    const remoteContainer = document.getElementById('remoteVideoContainer');
    
    if (remoteVideo) {
      // 비디오 클릭 시 재생 시도
      remoteVideo.onclick = function() {
        log("비디오 요소 직접 클릭됨");
        if (remoteVideo.paused && remoteVideo.srcObject) {
          remoteVideo.play().catch(e => log(`클릭 후 재생 실패: ${e.message}`));
        }
        
        // 비디오 요소 스타일 강제 적용
        remoteVideo.style.display = "block";
        remoteVideo.style.opacity = "1";
        remoteVideo.style.visibility = "visible";
      };
    }
    
    if (remoteContainer) {
      // 컨테이너 클릭 시 재생 시도
      remoteContainer.onclick = function() {
        log("비디오 컨테이너 클릭됨");
        if (remoteVideo && remoteVideo.paused && remoteVideo.srcObject) {
          remoteVideo.play().catch(e => log(`컨테이너 클릭 후 재생 실패: ${e.message}`));
        }
        
        // 비디오 요소 스타일 강제 적용
        if (remoteVideo) {
          remoteVideo.style.display = "block";
          remoteVideo.style.opacity = "1";
          remoteVideo.style.visibility = "visible";
        }
      };
    }
  }, 2000);
});

// 초기화 함수
function initWebRTC() {
  log("WebRTC 초기화 시작");
  
  // 비디오 요소 확인 및 초기 상태 설정
  const localVideo = document.getElementById('localVideo');
  const remoteVideo = document.getElementById('remoteVideo');
  
  if (!localVideo || !remoteVideo) {
    log("비디오 요소를 찾을 수 없음, 초기화 중단");
    alert("화상 통화 요소를 찾을 수 없습니다.");
    return;
  }
  
  // 전역 클릭 이벤트 리스너 추가 (자동재생 정책 우회)
  if (!window.globalPlayAttemptListener) {
    window.globalPlayAttemptListener = function() {
      log("문서 클릭 감지됨 - 모든 비디오 재생 시도");
      const remoteVideo = document.getElementById('remoteVideo');
      if (remoteVideo && remoteVideo.paused && remoteVideo.srcObject) {
        log("클릭 감지로 인한 원격 비디오 재생 시도");
        remoteVideo.muted = true; // 음소거로 시작하여 자동재생 정책 우회
        remoteVideo.play()
          .then(() => {
            log("문서 클릭 후 재생 성공");
            remoteVideo.style.opacity = "1";
            
            // 잠시 후 음소거 해제 시도
            setTimeout(() => {
              remoteVideo.muted = false;
              log("문서 클릭 후 음소거 해제");
            }, 500);
            
            // 메시지 제거
            const messages = document.querySelectorAll('.video-play-message');
            messages.forEach(el => el.parentNode && el.parentNode.removeChild(el));
          })
          .catch(err => {
            log(`문서 클릭 후에도 재생 실패: ${err.message}`);
          });
      }
    };
    
    // 문서 전체에 이벤트 리스너 추가 (캡처 단계에서 실행)
    document.addEventListener('click', window.globalPlayAttemptListener, true);
    document.addEventListener('touchstart', window.globalPlayAttemptListener, true);
    log("전역 재생 이벤트 리스너 추가됨");
  }
  
  // 요소 확인 및 초기 상태 설정
  setupUI();
  
  // 카메라 초기화
  setupCamera().then(() => {
    log("카메라 초기화 완료");
    
    // 비디오 속성 설정 - 명시적으로 모든 속성 설정
    if (localVideo) {
      localVideo.muted = true; // 자신의 소리는 들리지 않게
      localVideo.autoplay = true;
      localVideo.playsInline = true;
      localVideo.controls = false; // 컨트롤 표시 안함
      
      // 스타일 정확히 설정
      localVideo.style.display = "block";
      localVideo.style.width = "100%";
      localVideo.style.height = "100%";
      localVideo.style.objectFit = "cover";
    }
    
    if (remoteVideo) {
      remoteVideo.autoplay = true;
      remoteVideo.playsInline = true;
      remoteVideo.muted = false; // 상대방 소리는 들리게
      remoteVideo.controls = false; // 컨트롤 표시 안함
      
      // 스타일 정확히 설정
      remoteVideo.style.display = "block";
      remoteVideo.style.width = "100%";
      remoteVideo.style.height = "100%";
      remoteVideo.style.objectFit = "cover";
    }
    
    // WebSocket 연결 확인
    if (window.stompClient && window.stompClient.connected) {
      log("WebSocket이 이미 연결됨, WebRTC 초기화 계속");
      
      // 역할에 따라 처리
      initializeByRole();
    } else {
      log("WebSocket 연결 대기 중");
      
      // WebSocket 연결 이벤트 대기
      document.addEventListener('websocketConnected', () => {
        log("WebSocket 연결됨, WebRTC 신호 처리 준비");
        
        // 역할에 따라 처리
        initializeByRole();
      }, { once: true }); // 이벤트 리스너가 한 번만 실행되도록 설정
      
      // 10초 타임아웃 후에도 WebSocket 연결이 안되면 수동 초기화
      setTimeout(() => {
        if (!window.stompClient || !window.stompClient.connected) {
          log("WebSocket 연결 타임아웃, 수동 초기화 시도");
          initializeByRole();
        }
      }, 10000);
    }
  }).catch(error => {
    log(`카메라 초기화 실패: ${error.message}`);
    alert(`카메라 액세스 오류: ${error.message}. 브라우저 설정에서 카메라 권한을 확인해주세요.`);
    
    // 카메라 오류가 있더라도 역할별 초기화는 진행
    // (원격 비디오는 받을 수 있어야 하므로)
    initializeByRole();
  });
  
  // 역할별 초기화 함수
  function initializeByRole() {
    // 메시지 리스너 설정
    setupMessageListener();
    
    // 기존 연결 정리 (새로고침 시)
    if (peerConnection) {
      log("기존 피어 연결 종료");
      peerConnection.close();
      peerConnection = null;
    }
    
    // 역할에 따라 처리
    if (userRole === 'agent') {
      // 상담원은 참여 요청 대기
      log("상담원 모드: 고객 참여 대기");
      
      // 상담원의 경우 피어 연결 미리 생성
      setTimeout(() => {
        log("상담원: 피어 연결 사전 생성");
        createPeerConnection();
      }, 1000);
    } else {
      // 고객은 참여 요청 전송
      log("고객 모드: 참여 요청 전송");
      
      // 고객의 경우 1초 후 참여 요청 전송
      setTimeout(() => {
        // 피어 연결 생성 후 참여 요청
        createPeerConnection();
        setTimeout(() => sendJoinRequest(), 500);
      }, 1000);
    }
    
    // 15초 후 연결 상태 확인 및 필요시 자동 복구
    setTimeout(() => {
      const remoteVideo = document.getElementById('remoteVideo');
      if (remoteVideo && (!remoteVideo.srcObject || remoteVideo.srcObject.getVideoTracks().length === 0)) {
        log("15초 후 연결 상태 확인: 원격 비디오 없음, 자동 복구 시도");
        forceSolveConnectionIssue();
      }
    }, 15000);
  }
}

// UI 초기 설정
function setupUI() {
  log("UI 초기화");
  
  // 기존 연결 상태 메시지 요소가 있으면 제거
  const statusEl = document.querySelector('.connection-status');
  if (statusEl && statusEl.parentNode) {
    statusEl.parentNode.removeChild(statusEl);
  }
  
  // 비디오 요소의 스타일 확인 및 조정
  const localVideo = document.getElementById('localVideo');
  const remoteVideo = document.getElementById('remoteVideo');
  
  if (localVideo) {
    // 로컬 비디오 스타일 설정
    localVideo.style.display = "block";
    localVideo.style.width = "100%";
    localVideo.style.height = "100%";
    localVideo.style.objectFit = "cover";
  }
  
  if (remoteVideo) {
    // 원격 비디오 스타일 설정
    remoteVideo.style.display = "block";
    remoteVideo.style.width = "100%";
    remoteVideo.style.height = "100%";
    remoteVideo.style.objectFit = "cover";
  }
}

// 상태 메시지 업데이트
function updateStatus(message) {
  log(`상태 업데이트: ${message}`);
  const statusEl = document.querySelector('.connection-status');
  if (statusEl) {
    // "연결됨" 메시지인 경우 메시지를 숨김
    if (message === "연결됨" || message === "") {
      statusEl.style.display = 'none';
    } else {
      statusEl.textContent = message;
      statusEl.style.display = 'block';
    }
  }
}

// 로컬 카메라 설정 - 낮은 비디오 품질로 시작
async function setupCamera() {
  log("카메라 설정 시작");
  
  // 이미 있는 스트림 정리
  if (localStream) {
    localStream.getTracks().forEach(track => track.stop());
    localStream = null;
  }
  
  try {
    // 낮은 해상도로 시작하여 안정성 확보
    localStream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 640 },
        height: { ideal: 480 },
        frameRate: { ideal: 20 },
        facingMode: 'user'
      },
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    });
    
    log("카메라/마이크 액세스 성공");
    
    // 초기 미디어 상태 설정
    isMicrophoneEnabled = true;
    isCameraEnabled = true;
    
    // 로컬 비디오에 연결
    const localVideo = document.getElementById('localVideo');
    if (localVideo) {
      localVideo.srcObject = localStream;
      localVideo.muted = true; // 자기 목소리 안들리게
      
      // 확실한 재생을 위한 속성 설정
      localVideo.setAttribute('autoplay', '');
      localVideo.setAttribute('playsinline', '');
      
      // 강제 재생 시도
      try {
        await localVideo.play();
        log("로컬 비디오 재생 성공");
      } catch (e) {
        log(`자동 재생 차단됨: ${e.message}, 사용자 상호작용 필요`);
        
        // 클릭으로 재생 유도
        alert("화면을 클릭하여 카메라를 활성화하세요");
        const playVideoOnInteraction = () => {
          localVideo.play().catch(err => log(`재생 시도 실패: ${err.message}`));
          document.removeEventListener('click', playVideoOnInteraction);
          document.removeEventListener('touchstart', playVideoOnInteraction);
        };
        
        document.addEventListener('click', playVideoOnInteraction);
        document.addEventListener('touchstart', playVideoOnInteraction);
      }
    } else {
      log("로컬 비디오 요소를 찾을 수 없음");
    }
    
    // 미디어 컨트롤 버튼 초기 상태 설정
    setTimeout(() => {
      updateMediaControlButtons();
    }, 500);
    
    return localStream;
  } catch (error) {
    log(`카메라 설정 오류: ${error.message}`);
    alert(`카메라 또는 마이크에 접근할 수 없습니다: ${error.message}`);
    throw error;
  }
}

// 미디어 품질 동적 조정
function adjustMediaQuality(quality) {
  if (!localStream) return;
  
  log(`미디어 품질 조정: ${quality}`);
  
  // 비디오 트랙이 있는지 확인
  const videoTrack = localStream.getVideoTracks()[0];
  if (!videoTrack) return;
  
  // 현재 제약 조건 가져오기
  const constraints = videoTrack.getConstraints();
  let newConstraints = {};
  
  // 품질에 따른 설정 조정
  switch(quality) {
    case 'low': // 낮은 품질 (모바일 또는 불안정한 네트워크)
      newConstraints = {
        width: { ideal: 320 },
        height: { ideal: 240 },
        frameRate: { ideal: 15 }
      };
      break;
    case 'medium': // 중간 품질 (적당한 네트워크)
      newConstraints = {
        width: { ideal: 640 },
        height: { ideal: 480 },
        frameRate: { ideal: 20 }
      };
      break;
    case 'high': // 높은 품질 (좋은 네트워크)
      newConstraints = {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        frameRate: { ideal: 30 }
      };
      break;
  }
  
  // 새 제약 조건 적용
  try {
    videoTrack.applyConstraints(newConstraints)
      .then(() => log(`비디오 품질 ${quality}로 조정됨`))
      .catch(e => log(`비디오 품질 조정 실패: ${e.message}`));
  } catch (e) {
    log(`비디오 품질 조정 오류: ${e.message}`);
  }
}

// 메시지 리스너 설정
function setupMessageListener() {
  log("WebRTC 메시지 리스너 설정");
  // 이 함수가 외부에서 호출되는 핸들러 (websocket.js에서 호출됨)
  window.handleRtcMessage = function(data) {
    // 자신이 보낸 메시지는 무시
    if (data.sender === userRole) {
      log(`자신(${userRole})이 보낸 메시지 무시: ${data.type}`);
      return;
    }
    
    log(`수신된 메시지: ${data.type}, 송신자: ${data.sender}`);
    processRtcMessage(data);
  };
}

// 참여 요청 전송
function sendJoinRequest() {
  log("참여 요청 전송");
  sendRtcMessage("join", { requestOffer: true });
}

// 메시지 처리
function processRtcMessage(data) {
  log(`메시지 처리: ${data.type}, 발신자: ${data.sender}`);
  
  switch (data.type) {
    case "join":
      // 참여 요청 - 상담원만 처리
      if (userRole === 'agent' && data.requestOffer) {
        log("고객 참여 요청 수신, 제안 생성 시작");
        updateStatus("연결 시도 중...");
        
        // 기존 연결이 없거나 이미 종료된 경우에만 새로 생성
        if (!peerConnection || peerConnection.connectionState === 'closed') {
          createPeerConnection();
        }
        
        // 제안 생성 (재연결 요청 여부 반영)
        createOffer(data.reconnect || false);
      }
      break;
      
    case "offer":
      // 제안 수신 - 주로 고객이 처리
      log("제안 수신, 응답 생성 시작");
      updateStatus("연결 시도 중...");
      
      // 기존 연결이 없거나 이미 종료된 경우에만 새로 생성
      if (!peerConnection || peerConnection.connectionState === 'closed') {
        createPeerConnection();
      }
      
      // 원격 설명 설정 및 응답 생성
      setRemoteDescAndCreateAnswer(data.sdp);
      break;
      
    case "answer":
      // 응답 수신 - 주로 상담원이 처리
      log("응답 수신, 원격 설명 설정");
      if (peerConnection) {
        peerConnection.setRemoteDescription(new RTCSessionDescription(data.sdp))
          .then(() => {
            log("원격 설명 설정 완료");
            
            // remoteVideo 요소 디버깅
            const remoteVideo = document.getElementById('remoteVideo');
            if (remoteVideo && remoteVideo.srcObject) {
              const videoTracks = remoteVideo.srcObject.getVideoTracks();
              const audioTracks = remoteVideo.srcObject.getAudioTracks();
              log(`원격 비디오 트랙 상태: 비디오=${videoTracks.length}, 오디오=${audioTracks.length}`);
            }
          })
          .catch(err => {
            log(`원격 설명 설정 오류: ${err.message}`);
            restartConnection();
          });
      }
      break;
      
    case "ice":
      // ICE 후보 수신
      if (peerConnection) {
        log("ICE 후보 수신");
        if (data.ice) {
          peerConnection.addIceCandidate(new RTCIceCandidate(data.ice))
            .then(() => log("ICE 후보 추가됨"))
            .catch(err => log(`ICE 후보 추가 오류: ${err.message}`));
        } else {
          log("비어있는 ICE 후보 무시");
        }
      }
      break;
      
    case "restart":
      // 재시작 요청
      if (userRole === 'agent' && data.requestRestart) {
        log("재시작 요청 수신, ICE 재시작 제안 생성");
        createOffer(true); // ICE 재시작 플래그 설정
      }
      break;
      
    case "quality":
      // 품질 조정 요청
      if (data.level) {
        log(`상대방이 품질 조정 요청: ${data.level}`);
        adjustMediaQuality(data.level);
      }
      break;
  }
}

// 연결 상태 모니터링
function monitorConnectionStability() {
  if (connectionStabilityTimer) {
    clearInterval(connectionStabilityTimer);
  }
  
  connectionStabilityTimer = setInterval(() => {
    if (!peerConnection) return;
    
    // 연결 상태 확인
    const connState = peerConnection.connectionState;
    const iceState = peerConnection.iceConnectionState;
    
    // 통계 정보 수집
    if (peerConnection.getStats) {
      peerConnection.getStats(null).then(stats => {
        let roundTripTime = 0;
        let packetLoss = 0;
        let jitter = 0;
        let bitrate = 0;
        
        stats.forEach(report => {
          if (report.type === 'remote-inbound-rtp' && report.kind === 'video') {
            if (report.roundTripTime) {
              roundTripTime = report.roundTripTime;
            }
            if (report.packetsLost) {
              packetLoss = report.packetsLost;
            }
            if (report.jitter) {
              jitter = report.jitter;
            }
          }
          
          if (report.type === 'outbound-rtp' && report.kind === 'video') {
            if (report.bytesSent && report.timestamp) {
              // 비트레이트 계산을 위한 로직이 여기 있을 수 있음
              bitrate = report.bytesSent;
            }
          }
        });
        
        // 네트워크 품질 평가
        evaluateNetworkQuality(roundTripTime, packetLoss, jitter, connState, iceState);
      }).catch(e => {
        log(`통계 수집 오류: ${e.message}`);
      });
    }
  }, 3000); // 3초마다 체크
}

// 네트워크 품질 평가
function evaluateNetworkQuality(rtt, packetLoss, jitter, connState, iceState) {
  let quality = 'medium'; // 기본값
  
  // 품질 평가 로직
  if (connState !== 'connected' || iceState !== 'connected') {
    quality = 'poor';
  } else if (rtt > 300 || packetLoss > 5 || jitter > 50) {
    quality = 'poor';
  } else if (rtt > 100 || packetLoss > 2 || jitter > 30) {
    quality = 'medium';
  } else {
    quality = 'good';
  }
  
  // 이전과 다른 품질 상태일 때만 조치
  if (quality !== networkQuality) {
    log(`네트워크 품질 변경: ${networkQuality} -> ${quality}`);
    networkQuality = quality;
    
    // 품질에 따른 조치
    if (quality === 'poor') {
      log("네트워크 품질이 좋지 않음, 낮은 품질로 조정");
      adjustMediaQuality('low');
      
      // 상대방에게도 품질 조정 요청 전송
      sendRtcMessage("quality", { level: 'low' });
    } else if (quality === 'medium' && networkQuality === 'poor') {
      log("네트워크 품질이 개선됨, 중간 품질로 조정");
      adjustMediaQuality('medium');
      
      // 상대방에게도 품질 조정 요청 전송
      sendRtcMessage("quality", { level: 'medium' });
    } else if (quality === 'good' && (networkQuality === 'poor' || networkQuality === 'medium')) {
      log("네트워크 품질이 좋음, 고품질로 조정");
      adjustMediaQuality('high');
      
      // 상대방에게도 품질 조정 요청 전송
      sendRtcMessage("quality", { level: 'high' });
    }
  }
}

// 피어 연결 생성
function createPeerConnection() {
  log("피어 연결 생성 시작");
  
  try {
    // 기존 연결 정리
    if (peerConnection) {
      log("기존 피어 연결 종료");
      peerConnection.close();
      peerConnection = null;
      // 잠시 대기하여 이전 연결이 완전히 닫히도록 함
      setTimeout(() => {
        log("새 피어 연결 생성 시작");
        initiatePeerConnection();
      }, 500);
    } else {
      initiatePeerConnection();
    }
  } catch (error) {
    log(`피어 연결 생성 오류: ${error.message}`);
    alert(`연결 오류: ${error.message}`);
  }
  
  // 실제 피어 연결 초기화 함수
  function initiatePeerConnection() {
    try {
      // 연결 생성
      peerConnection = new RTCPeerConnection(iceConfig);
      log("새 RTCPeerConnection 객체 생성됨");
      
      // 연결 상태 변화 디버깅
      window.pc = peerConnection; // 디버깅을 위해 전역으로 저장
      
      // ICE 후보 이벤트
      peerConnection.onicecandidate = event => {
        try {
          if (event && event.candidate) {
            log(`ICE 후보 생성: ${event.candidate.candidate.substr(0, 50)}...`);
            sendRtcMessage("ice", { ice: event.candidate });
          } else {
            log("ICE 후보 수집 완료");
          }
        } catch (e) {
          log(`ICE 후보 처리 오류: ${e.message}`);
        }
      };
      
      // ICE 수집 상태 이벤트 추가
      peerConnection.onicegatheringstatechange = () => {
        if (peerConnection) {
          log(`ICE 수집 상태: ${peerConnection.iceGatheringState}`);
        }
      };
      
      // ICE 연결 상태 변경
      peerConnection.oniceconnectionstatechange = () => {
        try {
          if (!peerConnection) return;
          
          log(`ICE 연결 상태: ${peerConnection.iceConnectionState}`);
          lastIceConnectionState = peerConnection.iceConnectionState;
          
          if (peerConnection.iceConnectionState === 'connected' || 
              peerConnection.iceConnectionState === 'completed') {
            // 상태 메시지 제거 (updateStatus 호출 제거)
            connectionAttempts = 0;
            
            // 연결 안정성 모니터링 시작
            monitorConnectionStability();
            
            // 원격 비디오 상태 확인
            const remoteVideo = document.getElementById('remoteVideo');
            if (remoteVideo && remoteVideo.srcObject) {
              const videoTracks = remoteVideo.srcObject.getVideoTracks();
              log(`연결 성공 후 원격 비디오 트랙 수: ${videoTracks.length}`);
              
              if (videoTracks.length === 0) {
                log("연결은 성공했지만 원격 비디오 트랙이 없음, 상대방의 카메라를 확인해주세요");
                // 비디오 트랙이 없어도 강제 연결 시도
                setTimeout(() => forceApplyRemoteStream(), 1000);
              }
            }
          } else if (peerConnection.iceConnectionState === 'failed') {
            // updateStatus 호출 제거
            
            // ICE 재시작 시도
            tryICERestart();
          } else if (peerConnection.iceConnectionState === 'disconnected') {
            // updateStatus 호출 제거
            
            // ICE 재시작 시도
            setTimeout(() => {
              if (peerConnection && peerConnection.iceConnectionState === 'disconnected') {
                tryICERestart();
              }
            }, 2000); // 2초 후 재시도
          } else if (peerConnection.iceConnectionState === 'closed') {
            log("ICE 연결이 닫힘");
            // 5초 후 문제 해결 버튼 생성
            setTimeout(() => {
              const remoteVideo = document.getElementById('remoteVideo');
              if (remoteVideo && (!remoteVideo.srcObject || remoteVideo.srcObject.getVideoTracks().length === 0)) {
                createFixButton();
              }
            }, 5000);
          }
        } catch (e) {
          log(`ICE 상태 변경 처리 오류: ${e.message}`);
        }
      };
      
      // 연결 상태 변경
      peerConnection.onconnectionstatechange = () => {
        try {
          if (!peerConnection) return;
          
          log(`연결 상태: ${peerConnection.connectionState}`);
          
          if (peerConnection.connectionState === 'connected') {
            log("연결 성공!");
            // updateStatus 호출 제거
            
            // 연결 성공 후 원격 비디오 체크
            setTimeout(() => {
              const remoteVideo = document.getElementById('remoteVideo');
              if (remoteVideo && remoteVideo.paused) {
                log("연결은 됐지만 원격 비디오가 재생되지 않음, 재생 시도");
                playRemoteVideoWithFallback(remoteVideo);
              }
            }, 1000);
          } else if (peerConnection.connectionState === 'failed') {
            log("연결 실패, 완전 재시작");
            restartConnection();
          } else if (peerConnection.connectionState === 'disconnected') {
            log("연결 끊김, 재연결 시도");
            setTimeout(() => restartConnection(), 2000);
          }
        } catch (e) {
          log(`연결 상태 변경 처리 오류: ${e.message}`);
        }
      };
      
      // 시그널링 상태 변경 이벤트 추가
      peerConnection.onsignalingstatechange = () => {
        if (peerConnection) {
          log(`시그널링 상태: ${peerConnection.signalingState}`);
        }
      };
      
      // 트랙 수신 이벤트 - 핵심 개선 부분
      peerConnection.ontrack = event => {
        try {
          // null 체크 추가
          if (!event || !event.track) {
            log("트랙 이벤트 또는 트랙이 null입니다");
            return;
          }
          
          log(`트랙 수신됨: ${event.track.kind}, 스트림 ID: ${event.streams && event.streams[0] ? event.streams[0].id : '스트림 없음'}, 트랙 ID: ${event.track.id}`);
          
          const remoteVideo = document.getElementById('remoteVideo');
          if (!remoteVideo) {
            log("원격 비디오 요소를 찾을 수 없음");
            return;
          }

          // 원격 비디오 상태 로깅
          log(`원격 비디오 현재 상태: ${remoteVideo.srcObject ? '스트림 있음' : '스트림 없음'}`);
          
          // 즉시 재생 시도 플래그 설정
          window.shouldAttemptPlay = true;
          
          // 트랙이 실제로 활성 상태인지 확인
          if (event.track.readyState !== 'live') {
            log(`트랙이 활성 상태가 아님: ${event.track.readyState}`);
            // 트랙 상태 변경 리스너 추가
            event.track.onunmute = () => {
              log(`트랙이 활성화됨: ${event.track.kind}`);
              // 트랙이 활성화되면 다시 처리
              processTrack(event.track, event.streams);
            };
            
            // 비활성 상태라도 일단 시도 (일부 브라우저에서는 상태가 부정확할 수 있음)
            processTrack(event.track, event.streams);
            return;
          }
          
          // 트랙 처리
          processTrack(event.track, event.streams);
        } catch (e) {
          log(`트랙 처리 중 오류: ${e.message}`);
        }
        
        // 내부 함수: 트랙 처리 로직
        function processTrack(track, streams) {
          try {
            // null 체크 추가
            if (!track) {
              log("처리할 트랙이 null입니다");
              return;
            }
            
            const remoteVideo = document.getElementById('remoteVideo');
            if (!remoteVideo) {
              log("원격 비디오 요소를 찾을 수 없음");
              return;
            }
            
            // 이벤트에서 스트림을 직접 사용 (가장 신뢰할 수 있는 방법)
            if (streams && streams.length > 0) {
              log(`이벤트에서 스트림 직접 사용 - 트랙 수: ${streams[0].getTracks().length}`);
              
              // 기존 remoteStream 변수 업데이트
              remoteStream = streams[0];
              
              // 여기가 중요: 반드시 원격 비디오 요소의 srcObject 설정
              remoteVideo.srcObject = remoteStream;
              
              // 추가 디버깅 정보
              const videoTracks = remoteStream.getVideoTracks();
              const audioTracks = remoteStream.getAudioTracks();
              log(`스트림에서 트랙 확인: 비디오=${videoTracks.length}, 오디오=${audioTracks.length}`);
              
              if (videoTracks.length > 0) {
                log(`비디오 트랙 정보: ${videoTracks[0].label}, 활성화=${videoTracks[0].enabled}, 상태=${videoTracks[0].readyState}`);
                
                // 비디오 트랙이 있음을 표시 (updateStatus 호출 제거)
                
                // 비디오 요소 완전히 표시
                remoteVideo.style.opacity = "1";
              }
            } else {
              // 스트림이 없으면 새 스트림 생성
              log("새 미디어 스트림 생성 (이벤트에 스트림 없음)");
              
              // remoteStream이 없거나 비어있으면 새로 생성
              if (!remoteStream) {
                remoteStream = new MediaStream();
              }
              
              // 이미 remoteVideo에 스트림이 연결되어 있는지 확인
              if (remoteVideo.srcObject !== remoteStream) {
                remoteVideo.srcObject = remoteStream;
                log("원격 비디오에 새 스트림 설정");
              }
              
              // 같은 종류의 기존 트랙 제거 (중복 방지)
              const existingTracks = remoteStream.getTracks().filter(t => t && t.kind === track.kind);
              if (existingTracks.length > 0) {
                log(`기존 ${track.kind} 트랙 ${existingTracks.length}개 제거`);
                existingTracks.forEach(t => {
                  try {
                    remoteStream.removeTrack(t);
                  } catch (e) {
                    log(`트랙 제거 오류: ${e.message}`);
                  }
                });
              }
              
              // 새 트랙 추가
              try {
                remoteStream.addTrack(track);
                log(`스트림에 ${track.kind} 트랙 추가됨, 트랙 ID: ${track.id}`);
                
                // 비디오 트랙이 추가되면 상태 업데이트
                if (track.kind === 'video') {
                  // updateStatus 호출 제거
                  remoteVideo.style.opacity = "1";
                }
              } catch (e) {
                log(`트랙 추가 오류: ${e.message}`);
              }
            }
            
            // 비디오 요소 속성 설정 (자동 재생 보장)
            remoteVideo.autoplay = true;
            remoteVideo.playsInline = true;
            remoteVideo.muted = false; // 원격 오디오 재생을 위해 음소거 해제
            
            // 스타일 설정 개선
            remoteVideo.style.display = "block";
            remoteVideo.style.width = "100%";
            remoteVideo.style.height = "100%";
            remoteVideo.style.objectFit = "cover";
            
            // 트랙 상태 모니터링
            track.onunmute = () => {
              log(`원격 트랙 활성화: ${track.kind}`);
              // updateStatus 호출 제거
              remoteVideo.style.opacity = "1"; // 비디오 표시
              
              // 자동 재생 시도
              playRemoteVideoWithFallback(remoteVideo);
            };
            
            track.onmute = () => {
              log(`원격 트랙 음소거: ${track.kind}`);
              if (track.kind === 'video') {
                remoteVideo.style.opacity = "0.5"; // 비디오 흐리게 표시
                // updateStatus 호출 제거
              }
            };
            
            track.onended = () => {
              log(`원격 트랙 종료: ${track.kind}`);
              if (track.kind === 'video') {
                // updateStatus 호출 제거
                remoteVideo.style.opacity = "0.3"; // 비디오 더 흐리게 표시
                
                // 트랙이 종료되면 5초 후 강제 재연결 시도
                setTimeout(() => {
                  if (!remoteVideo.srcObject || 
                      remoteVideo.srcObject.getVideoTracks().length === 0 ||
                      remoteVideo.srcObject.getVideoTracks()[0].readyState !== 'live') {
                    log("비디오 트랙 종료 후 5초 경과, 강제 재연결 시도");
                    forceSolveConnectionIssue();
                  }
                }, 5000);
              }
            };
            
            // 즉시 재생 시도
            if (window.shouldAttemptPlay) {
              window.shouldAttemptPlay = false; // 중복 시도 방지
              log("트랙 수신 후 즉시 비디오 재생 시도");
              playRemoteVideoWithFallback(remoteVideo);
              
              // 100ms 후 한 번 더 시도 (일부 브라우저에서는 즉시 시도가 실패할 수 있음)
              setTimeout(() => {
                if (remoteVideo && remoteVideo.paused) {
                  log("재생 재시도 (지연 시도)");
                  playRemoteVideoWithFallback(remoteVideo);
                }
              }, 100);
              
              // 2초 후에도 재생되지 않으면 다시 시도 (신뢰성 향상)
              setTimeout(() => {
                if (remoteVideo && remoteVideo.paused) {
                  log("2초 후 재생 여전히 안됨, 다시 시도");
                  remoteVideo.muted = true; // 자동 재생 정책 우회
                  remoteVideo.play().then(() => {
                    log("지연 재생 성공");
                    setTimeout(() => {
                      remoteVideo.muted = false;
                      log("지연 재생 후 음소거 해제");
                    }, 500);
                  }).catch(e => {
                    log(`지연 재생 실패: ${e.message}`);
                    // 메시지 표시 코드 제거 - 표시하지 않음
                  });
                }
              }, 2000);
            }
          } catch (e) {
            log(`processTrack 내부 오류: ${e.message}`);
          }
        }
        
        // WebRTC 연결 상태 확인 로그
        if (peerConnection) {
          log(`연결 상태: ${peerConnection.connectionState}, ICE 상태: ${peerConnection.iceConnectionState}`);
        }
      };
      
      // 데이터 채널 설정 (연결 안정성 테스트용)
      try {
        const pingChannel = peerConnection.createDataChannel('ping');
        pingChannel.onopen = () => {
          log("핑 데이터 채널 열림");
          
          // 연결 유지를 위한 핑 메시지 전송
          setInterval(() => {
            if (pingChannel.readyState === 'open') {
              try {
                pingChannel.send(JSON.stringify({type: 'ping', time: Date.now()}));
              } catch(e) {
                log(`핑 전송 오류: ${e.message}`);
              }
            }
          }, 10000); // 10초마다 핑
        };
      } catch (e) {
        log(`데이터 채널 생성 오류: ${e.message}`);
      }
      
      // 데이터 채널 수신 처리
      peerConnection.ondatachannel = event => {
        const channel = event.channel;
        if (channel.label === 'ping') {
          channel.onmessage = e => {
            try {
              const data = JSON.parse(e.data);
              if (data.type === 'ping') {
                // 핑에 응답
                channel.send(JSON.stringify({type: 'pong', time: data.time}));
              }
            } catch(err) {
              log(`핑 데이터 처리 오류: ${err.message}`);
            }
          };
        }
      };
      
      // 로컬 미디어 스트림 추가
      if (localStream) {
        log("로컬 스트림 연결에 추가");
        try {
          localStream.getTracks().forEach(track => {
            if (track) {
              try {
                peerConnection.addTrack(track, localStream);
                log(`로컬 트랙 추가: ${track.kind}`);
              } catch (e) {
                log(`트랙 추가 오류: ${e.message}`);
              }
            }
          });
        } catch (e) {
          log(`로컬 트랙 처리 오류: ${e.message}`);
        }
      } else {
        log("로컬 스트림이 없어 먼저 카메라를 설정합니다");
        setupCamera().then(stream => {
          if (stream && peerConnection) {
            try {
              stream.getTracks().forEach(track => {
                if (track) {
                  try {
                    peerConnection.addTrack(track, stream);
                    log(`후속 로컬 트랙 추가: ${track.kind}`);
                  } catch (e) {
                    log(`후속 트랙 추가 오류: ${e.message}`);
                  }
                }
              });
            } catch (e) {
              log(`후속 로컬 트랙 처리 오류: ${e.message}`);
            }
          }
        }).catch(e => {
          log(`카메라 설정 오류: ${e.message}`);
        });
      }
      
      // 비트레이트 제한 설정
      setLowerBitrateConstraints(peerConnection);
      
      log("피어 연결 생성 완료");
    } catch (e) {
      log(`피어 연결 초기화 오류: ${e.message}`);
    }
  }
}

// ICE 재시작 시도
function tryICERestart() {
  log("ICE 재시작 시도");
  
  if (!peerConnection) {
    log("피어 연결이 없어 ICE 재시작을 시도할 수 없음");
    return;
  }
  
  try {
    if (userRole === 'agent') {
      // 상담원이 ICE 재시작 제안 생성
      createOffer(true);
    } else {
      // 고객은 재시작 요청 전송 (상담원이 제안을 생성하도록)
      sendRtcMessage("restart", { requestRestart: true });
    }
  } catch (e) {
    log(`ICE 재시작 오류: ${e.message}`);
    
    // 심각한 오류면 완전 재시작
    restartConnection();
  }
}

// 비트레이트 제한 설정
function setLowerBitrateConstraints(pc) {
  if (!pc) return;
  
  try {
    // 가능한 경우 트랜시버 설정 조정
    pc.getTransceivers().forEach(transceiver => {
      if (transceiver.sender && transceiver.sender.track && transceiver.sender.track.kind === 'video') {
        const params = transceiver.sender.getParameters();
        if (params.encodings && params.encodings.length > 0) {
          // 비트레이트 제한
          params.encodings[0].maxBitrate = 800000; // 800 kbps
          transceiver.sender.setParameters(params)
            .then(() => log("비디오 비트레이트 제한 설정됨"))
            .catch(e => log(`비트레이트 제한 설정 오류: ${e.message}`));
        }
      }
    });
  } catch (e) {
    log(`비트레이트 제한 오류: ${e.message}`);
  }
}

// 연결 재시작
function restartConnection() {
  connectionAttempts++;
  log(`연결 재시도 ${connectionAttempts}/${MAX_RECONNECT_ATTEMPTS}`);
  
  if (connectionAttempts <= MAX_RECONNECT_ATTEMPTS) {
    setTimeout(() => {
      // updateStatus 호출 제거
      
      if (userRole === 'agent') {
        // 상담원이 다시 제안
        createPeerConnection();
        createOffer();
      } else {
        // 고객이 다시 참여 요청
        sendJoinRequest();
      }
    }, 1000 * connectionAttempts); // 지수적 백오프
  } else {
    // updateStatus 호출 제거
    log("최대 재시도 횟수 초과");
  }
}

// 제안 생성
async function createOffer(iceRestart = false) {
  log(`제안 생성 시작 ${iceRestart ? '(ICE 재시작)' : ''}`);
  
  // 피어 연결이 없는 경우 새로 생성하고 잠시 후 다시 시도
  if (!peerConnection) {
    log("피어 연결이 없어 제안을 생성할 수 없음 → 새 피어 연결 생성 후 재시도");
    createPeerConnection();
    // 300ms 정도 대기 후 다시 시도 (피어 연결 생성은 즉시 완료되므로 짧게 대기)
    setTimeout(() => createOffer(iceRestart), 300);
    return;
  }
  
  try {
    // 제안 생성 옵션
    const offerOptions = {
      offerToReceiveAudio: true,
      offerToReceiveVideo: true,
      iceRestart: iceRestart
    };
    
    // 제안 생성
    const offer = await peerConnection.createOffer(offerOptions);
    
    // SDP 변환 및 최적화
    let modifiedSdp = offer.sdp;
    
    // 비디오 관련 설정 추가
    if (modifiedSdp.includes('m=video')) {
      // 비디오 비트레이트 제한 설정
      modifiedSdp = modifiedSdp.replace(/(m=video.*\r\n)/g, '$1b=AS:800\r\n');
    }
    
    // 오디오 비트레이트 제한 설정
    if (modifiedSdp.includes('m=audio')) {
      modifiedSdp = modifiedSdp.replace(/(m=audio.*\r\n)/g, '$1b=AS:50\r\n');
    }
    
    // 수정된 SDP로 새 제안 객체 생성
    const modifiedOffer = {
      type: offer.type,
      sdp: modifiedSdp
    };
    
    // 로컬 설명 설정
    await peerConnection.setLocalDescription(modifiedOffer);
    log("로컬 설명 설정 완료");
    
    // 제안 전송
    sendRtcMessage("offer", { sdp: modifiedOffer, iceRestart: iceRestart });
    log("제안 전송 완료");
  } catch (error) {
    log(`제안 생성 오류: ${error.message}`);
    if (error.name === 'InvalidStateError') {
      // 연결 초기화 중일 수 있으므로 200ms 후 재시도
      setTimeout(() => createOffer(iceRestart), 200);
    }
  }
}

// 원격 설명 설정 및 응답 생성
async function setRemoteDescAndCreateAnswer(sdp) {
  log("원격 제안 설정 및 응답 생성 시작");
  
  if (!peerConnection) {
    log("피어 연결이 없어 응답을 생성할 수 없음");
    return;
  }
  
  try {
    // 원격 설명 설정
    log("원격 SDP 설정 시작");
    await peerConnection.setRemoteDescription(new RTCSessionDescription(sdp));
    log("원격 제안 설정됨, 응답 생성");
    
    // SDP 디버깅 (문제 진단용)
    if (sdp && sdp.sdp) {
      // 비디오 라인이 있는지 확인
      const hasVideo = sdp.sdp.includes('m=video');
      const hasAudio = sdp.sdp.includes('m=audio');
      log(`원격 SDP 확인: 비디오=${hasVideo ? '있음' : '없음'}, 오디오=${hasAudio ? '있음' : '없음'}`);
      
      // 추가 SDP 디버깅 - ICE 옵션 확인
      const hasIceLite = sdp.sdp.includes('ice-lite');
      const hasIceOptions = sdp.sdp.includes('ice-options');
      log(`ICE 옵션: ice-lite=${hasIceLite}, ice-options=${hasIceOptions}`);
      
      // 제안에 있는 ICE 재시작 확인
      const isIceRestart = sdp.iceRestart === true;
      log(`ICE 재시작 요청: ${isIceRestart ? '예' : '아니오'}`);
    }
    
    // 응답 생성
    log("응답 SDP 생성 시작");
    const answer = await peerConnection.createAnswer();
    log("응답 SDP 생성됨");
    
    // SDP 변형 (선택적): 비트레이트 제한 및 기타 향상
    let modifiedSDP = answer.sdp;
    
    // 비디오 비트레이트 제한
    if (modifiedSDP.includes('m=video')) {
      modifiedSDP = modifiedSDP.replace(/(m=video.*\r\n)/g, '$1b=AS:800\r\n');
      log("비디오 비트레이트 제한 설정됨 (800kbps)");
      
      // 비디오 코덱 최적화 (H.264 우선)
      const videoLines = modifiedSDP.split('\r\n');
      const mVideoIndex = videoLines.findIndex(line => line.startsWith('m=video'));
      
      if (mVideoIndex >= 0) {
        // m=video 라인에서 모든 코덱 번호 추출
        const mVideoLine = videoLines[mVideoIndex];
        const codecNumbers = mVideoLine.split(' ').slice(3);
        
        // 각 코덱 번호에 대한 RTP 맵 라인 찾기
        const rtpMapLines = [];
        for (const number of codecNumbers) {
          const rtpMapIndex = videoLines.findIndex(line => 
            line.match(new RegExp(`a=rtpmap:${number} `)));
          if (rtpMapIndex >= 0) {
            rtpMapLines.push({
              index: rtpMapIndex,
              number: number,
              line: videoLines[rtpMapIndex]
            });
          }
        }
        
        // H.264 코덱 찾기
        const h264Lines = rtpMapLines.filter(item => item.line.includes('H264'));
        if (h264Lines.length > 0) {
          // H.264 코덱을 맨 앞으로 이동
          const newCodecOrder = [
            ...h264Lines.map(item => item.number),
            ...codecNumbers.filter(number => 
              !h264Lines.some(item => item.number === number))
          ];
          
          // 새로운 m=video 라인 생성
          const mVideoLineParts = mVideoLine.split(' ');
          const newMVideoLine = [...mVideoLineParts.slice(0, 3), ...newCodecOrder].join(' ');
          videoLines[mVideoIndex] = newMVideoLine;
          
          // 수정된 SDP 재구성
          modifiedSDP = videoLines.join('\r\n');
          log("응답 SDP에서 H.264 코덱 우선순위 설정 완료");
        }
      }
    }
    
    // 오디오 비트레이트 제한
    if (modifiedSDP.includes('m=audio')) {
      modifiedSDP = modifiedSDP.replace(/(m=audio.*\r\n)/g, '$1b=AS:50\r\n');
      log("오디오 비트레이트 제한 설정됨 (50kbps)");
    }
    
    // 수정된 SDP로 응답 객체 생성
    const modifiedAnswer = {
      type: answer.type,
      sdp: modifiedSDP
    };
    
    // 로컬 설명 설정
    log("로컬 SDP 설정 시작");
    await peerConnection.setLocalDescription(modifiedAnswer);
    log("로컬 응답 설정됨");
    
    // 응답 전송
    log("응답 SDP 전송");
    sendRtcMessage("answer", { sdp: modifiedAnswer });
    
    // SDP 교환 완료 후 연결 상태 확인
    log(`SDP 교환 완료, 연결 상태: ${peerConnection.connectionState}, ICE 상태: ${peerConnection.iceConnectionState}`);
    
    // SDP 교환 후 5초 후에도 비디오가 없으면 강제 문제 해결 시도
    setTimeout(() => {
      const remoteVideo = document.getElementById('remoteVideo');
      if (remoteVideo && (!remoteVideo.srcObject || 
          remoteVideo.srcObject.getVideoTracks().length === 0 || 
          remoteVideo.paused)) {
        log("SDP 교환 후 5초 후에도 원격 비디오가 없음, 강제 문제 해결 시도");
        forceApplyRemoteStream();
      }
    }, 5000);
  } catch (error) {
    log(`응답 생성 오류: ${error.message}`);
    // updateStatus 호출 제거
    
    // 문제 발생 시 완전 새로운 연결 시도
    setTimeout(() => {
      log("오류로 인해 연결 재시작");
      restartConnection();
    }, 2000);
  }
}

// WebRTC 메시지 전송
function sendRtcMessage(type, data = {}) {
  if (!stompClient || !stompClient.connected) {
    log("WebSocket 연결이 없어 메시지를 보낼 수 없음");
    return;
  }
  
  // 메시지 준비
  const message = {
    type: type,
    sender: userRole,
    roomId: sessionId,
    timestamp: Date.now(),
    ...data
  };
  
  log(`메시지 전송: ${type}`);
  
  // 메시지 전송 (수정된 토픽 경로)
  stompClient.send("/app/room/" + sessionId + "/rtc", {}, JSON.stringify(message));
}

// 원격 비디오 재생 헬퍼 함수 - 강화된 버전
function playRemoteVideoWithFallback(videoElement) {
  if (!videoElement) return;
  
  log("원격 비디오 재생 시도");
  
  // 전역 이벤트 리스너 삭제 함수 (중복 방지)
  if (window.globalClickListener) {
    document.removeEventListener('click', window.globalClickListener);
    document.removeEventListener('touchstart', window.globalClickListener);
    window.globalClickListener = null;
    log("기존 클릭 리스너 제거");
  }
  
  // 비디오 요소 관련 메시지 모두 제거
  const messages = document.querySelectorAll('.video-play-message');
  messages.forEach(el => el.parentNode && el.parentNode.removeChild(el));
  
  // 이미 재생 중인지 확인
  if (!videoElement.paused) {
    log("원격 비디오가 이미 재생 중입니다.");
    videoElement.style.opacity = "1";
    return;
  }
  
  // 스트림이 있는지 확인
  if (!videoElement.srcObject) {
    log("원격 비디오에 스트림이 없습니다.");
    return;
  }
  
  // 비디오 트랙이 있는지 확인
  const videoTracks = videoElement.srcObject.getVideoTracks();
  const hasVideo = videoTracks && videoTracks.length > 0;
  
  log(`재생 시도 - 비디오 트랙: ${hasVideo ? '있음' : '없음'}`);
  
  // 속성 강제 설정
  videoElement.autoplay = true;
  videoElement.playsInline = true;
  videoElement.muted = true; // 항상 음소거 상태로 시작 (자동재생 정책 우회)
  videoElement.controls = false; // 컨트롤 숨김
  
  // 비디오 요소에 직접 클릭 이벤트 추가 (더 명확한 사용자 상호작용)
  videoElement.onclick = function() {
    log("비디오 요소 직접 클릭됨");
    attemptPlay();
  };
  
  // 재생 시도 함수
  function attemptPlay() {
    const playPromise = videoElement.play();
    
    if (playPromise !== undefined) {
      playPromise.then(() => {
        log("원격 비디오 재생 성공 (음소거 상태)");
        videoElement.style.opacity = "1";
        
        // 재생 성공 후 음소거 해제 (오디오 복원)
        setTimeout(() => {
          videoElement.muted = false;
          log("원격 비디오 음소거 해제");
        }, 500);
        
        // 메시지가 있으면 제거
        const messages = document.querySelectorAll('.video-play-message');
        messages.forEach(el => el.parentNode && el.parentNode.removeChild(el));
      }).catch(e => {
        log(`자동 재생 실패: ${e.message}, 사용자 클릭 유도`);
        showPlayMessage(videoElement);
      });
    } else {
      log("재생 시도가 정의되지 않음, 브라우저가 Promise를 반환하지 않음");
      showPlayMessage(videoElement);
    }
  }
  
  // 첫 번째 재생 시도
  attemptPlay();
  
  // 전역 클릭 리스너 등록 (한 번의 클릭으로 모든 비디오 재생 시도)
  window.globalClickListener = function playVideoOnClick(e) {
    log("사용자 클릭 감지됨, 모든 비디오 재생 시도");
    
    // 메시지 제거
    const messages = document.querySelectorAll('.video-play-message');
    messages.forEach(el => el.parentNode && el.parentNode.removeChild(el));
    
    // 이벤트가 비디오 요소 자체에서 발생한 경우 중복 처리 방지
    if (e && (e.target === videoElement || videoElement.contains(e.target))) {
      log("비디오 요소 내에서 클릭 발생, 중복 처리 방지");
      return;
    }
    
    // 원격 비디오 재생 시도
    if (videoElement && videoElement.paused) {
      videoElement.muted = true; // 음소거로 시작하여 자동재생 정책 우회
      videoElement.play()
        .then(() => {
          log("클릭 후 재생 성공");
          videoElement.style.opacity = "1";
          
          // 잠시 후 음소거 해제 시도
          setTimeout(() => {
            videoElement.muted = false;
            log("클릭 후 음소거 해제");
          }, 500);
        })
        .catch(err => {
          log(`클릭 후에도 재생 실패: ${err.message}, 다시 시도`);
          // 다시 시도
          setTimeout(() => {
            videoElement.play().catch(e => {
              log(`재시도 후에도 실패: ${e.message}`);
            });
          }, 100);
          // 실패 시 화면 표시만이라도
          videoElement.style.opacity = "1";
        });
    }
  };
  
  // 문서 전체에 이벤트 리스너 추가 (캡처 단계에서 실행)
  document.addEventListener('click', window.globalClickListener, true);
  document.addEventListener('touchstart', window.globalClickListener, true);
  
  // 메시지 표시 헬퍼 함수
  function showPlayMessage(videoElement) {
    // 메시지를 표시하지 않고 대신 클릭 이벤트 리스너만 등록
    
    // 전역 클릭 리스너가 동작할 수 있도록 설정
    if (!window.messageClickAttempted) {
      window.messageClickAttempted = true;
      
      // 페이지 내 아무 곳이나 클릭 시 자동으로 비디오 재생 시도
      document.documentElement.addEventListener('click', function documentClickHandler(event) {
        log("문서 클릭됨, 모든 비디오 재생 시도");
        if (videoElement && videoElement.paused) {
          attemptPlay();
        }
        document.documentElement.removeEventListener('click', documentClickHandler);
      }, { once: true });
    }
  }
}

// 로컬 미디어 스트림 강제 재설정 함수 - 문제 해결을 위한 추가 함수
function forceResetLocalStream() {
  log("로컬 미디어 강제 재설정 시작");
  
  return new Promise((resolve, reject) => {
    try {
      // 기존 스트림 정리
      if (localStream) {
        localStream.getTracks().forEach(track => {
          if (track) {
            log(`로컬 트랙 정지: ${track.kind}`);
            track.stop();
          }
        });
        localStream = null;
      }
      
      // 새로운 미디어 스트림 획득
      navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          frameRate: { ideal: 20 }
        },
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      }).then(stream => {
        try {
          localStream = stream;
          log("새 로컬 미디어 스트림 획득 성공");
          
          // 로컬 비디오에 연결
          const localVideo = document.getElementById('localVideo');
          if (localVideo) {
            localVideo.srcObject = stream;
            localVideo.muted = true;
            
            // 속성 설정
            localVideo.autoplay = true;
            localVideo.playsInline = true;
            localVideo.style.display = "block";
            
            try {
              localVideo.play()
                .then(() => {
                  log("로컬 비디오 재생 성공");
                  resolve(stream);
                })
                .catch(err => {
                  log(`로컬 비디오 재생 실패: ${err.message}`);
                  // 실패해도 스트림은 성공적으로 획득했으므로 resolve
                  resolve(stream);
                });
            } catch (e) {
              log(`로컬 비디오 재생 중 예외: ${e.message}`);
              // 실패해도 스트림은 성공적으로 획득했으므로 resolve
              resolve(stream);
            }
          } else {
            log("로컬 비디오 요소를 찾을 수 없음");
            // 비디오 요소가 없어도 스트림은 획득했으므로 resolve
            resolve(stream);
          }
        } catch (e) {
          log(`로컬 스트림 처리 오류: ${e.message}`);
          // 스트림은 획득했으므로 resolve
          resolve(stream);
        }
      }).catch(error => {
        log(`미디어 획득 실패: ${error.message}`);
        reject(error);
      });
    } catch (e) {
      log(`forceResetLocalStream 오류: ${e.message}`);
      reject(e);
    }
  });
}

// 원격 스트림 강제 적용 함수
function forceApplyRemoteStream() {
  log("원격 스트림 강제 적용 시도");
  
  try {
    const remoteVideo = document.getElementById('remoteVideo');
    if (!remoteVideo) {
      log("원격 비디오 요소를 찾을 수 없음");
      return false;
    }
    
    // 이미 스트림이 있는 경우 확인
    if (remoteVideo.srcObject) {
      const videoTracks = remoteVideo.srcObject.getVideoTracks();
      const audioTracks = remoteVideo.srcObject.getAudioTracks();
      log(`기존 원격 스트림 상태: 비디오=${videoTracks?.length || 0}, 오디오=${audioTracks?.length || 0}`);
      
      // 트랙은 있지만 재생 중이 아니면 재생 시도
      if (!remoteVideo.paused) {
        log("원격 비디오가 이미 재생 중입니다");
        return true;
      }
      
      // 비디오 트랙이 있고 활성 상태면 재생 시도
      if (videoTracks && videoTracks.length > 0 && videoTracks[0].readyState === 'live') {
        log("활성 비디오 트랙이 있음, 재생 시도");
        playRemoteVideoWithFallback(remoteVideo);
        return true;
      }
    }
    
    // peerConnection에서 수신자 확인
    if (peerConnection && peerConnection.getReceivers) {
      try {
        const receivers = peerConnection.getReceivers();
        log(`피어 연결 수신자 수: ${receivers.length}`);
        
        if (receivers.length > 0) {
          // 새 MediaStream 생성 및 모든 트랙 추가
          const newStream = new MediaStream();
          let hasVideoTrack = false;
          
          receivers.forEach(receiver => {
            try {
              if (receiver && receiver.track && receiver.track.readyState === 'live') {
                newStream.addTrack(receiver.track);
                log(`수신자 트랙 추가: ${receiver.track.kind}`);
                
                if (receiver.track.kind === 'video') {
                  hasVideoTrack = true;
                }
              }
            } catch (e) {
              log(`수신자 트랙 추가 오류: ${e.message}`);
            }
          });
          
          // 트랙이 추가된 경우에만 적용
          if (newStream.getTracks().length > 0) {
            remoteStream = newStream;
            remoteVideo.srcObject = newStream;
            
            // 비디오 속성 설정
            remoteVideo.autoplay = true;
            remoteVideo.playsInline = true;
            remoteVideo.muted = false;
            
            // 스타일 설정
            remoteVideo.style.display = "block";
            remoteVideo.style.width = "100%";
            remoteVideo.style.height = "100%";
            remoteVideo.style.objectFit = "cover";
            
            // 재생 시도
            log("수신자에서 가져온 스트림으로 원격 비디오 설정");
            playRemoteVideoWithFallback(remoteVideo);
            
            // 비디오 트랙이 있으면 상태 업데이트
            if (hasVideoTrack) {
              updateStatus("");
              remoteVideo.style.opacity = "1";
            }
            
            return true;
          }
        }
      } catch (e) {
        log(`수신자 처리 오류: ${e.message}`);
      }
    }
    
    // 마지막 시도: 피어 연결 재설정
    if (userRole === 'agent') {
      log("최종 시도: 상담원이 새 제안 생성");
      setTimeout(() => createOffer(true), 500);
    } else {
      log("최종 시도: 고객이 참여 요청 재전송");
      setTimeout(() => sendJoinRequest(), 500);
    }
    
    log("원격 스트림을 찾을 수 없거나 적용할 수 없음");
    return false;
  } catch (e) {
    log(`forceApplyRemoteStream 오류: ${e.message}`);
    return false;
  }
}

// 연결 문제를 강제로 해결하는 함수
function forceSolveConnectionIssue() {
  log("연결 문제 강제 해결 시도");
  
  try {
    // 1. 로컬 미디어 재설정
    forceResetLocalStream()
      .then(stream => {
        log("로컬 스트림 재설정 성공");
        
        try {
          // 2. 새로운 피어 연결 생성
          if (peerConnection) {
            log("기존 피어 연결 종료");
            peerConnection.close();
            peerConnection = null;
          }
          
          // 3. 새 연결 생성
          log("새 피어 연결 생성");
          createPeerConnection();
          
          // 새 로컬 스트림 추가
          if (stream && peerConnection) {
            stream.getTracks().forEach(track => {
              if (track) {
                try {
                  peerConnection.addTrack(track, stream);
                  log(`새 로컬 트랙 추가: ${track.kind}`);
                } catch (e) {
                  log(`트랙 추가 오류: ${e.message}`);
                }
              }
            });
          }
          
          // 4. 역할에 따라 다른 동작
          setTimeout(() => {
            try {
              if (userRole === 'agent') {
                log("상담원 역할 - 새 제안 생성");
                createOffer(true);
              } else {
                log("고객 역할 - 참여 요청 재전송");
                sendJoinRequest();
              }
            } catch (e) {
              log(`역할 기반 동작 오류: ${e.message}`);
            }
            
            // 5. 일정 시간 후 원격 스트림 강제 적용 시도
            setTimeout(() => {
              try {
                log("강제 원격 스트림 적용 시도");
                forceApplyRemoteStream();
                
                // 6. 추가로 2초 후 다시 한번 시도
                setTimeout(() => {
                  try {
                    log("2초 후 추가 원격 스트림 적용 시도");
                    forceApplyRemoteStream();
                    
                    // 7. 마지막 시도
                    setTimeout(() => {
                      try {
                        const remoteVideo = document.getElementById('remoteVideo');
                        if (remoteVideo && (!remoteVideo.srcObject || 
                            remoteVideo.srcObject.getVideoTracks().length === 0)) {
                          log("최종 복구 시도: 연결 완전 재시작");
                          // 상대방에게 재참여 요청
                          if (userRole === 'agent') {
                            log("상담원: 고객에게 재참여 요청");
                            sendRtcMessage("restart", { requestRestart: true });
                          } else {
                            log("고객: 재참여 요청");
                            sendJoinRequest();
                          }
                        }
                      } catch (e) {
                        log(`최종 복구 시도 오류: ${e.message}`);
                      }
                    }, 5000);
                  } catch (e) {
                    log(`추가 스트림 적용 오류: ${e.message}`);
                  }
                }, 2000);
              } catch (e) {
                log(`강제 스트림 적용 오류: ${e.message}`);
              }
            }, 3000);
          }, 1000);
        } catch (e) {
          log(`피어 연결 재생성 오류: ${e.message}`);
        }
      })
      .catch(error => {
        log(`강제 문제 해결 실패: ${error.message}`);
        
        // 로컬 미디어 획득에 실패해도 원격 비디오 복구 시도
        setTimeout(() => {
          log("미디어 획득 실패 후 원격 비디오만 복구 시도");
          forceApplyRemoteStream();
        }, 1000);
      });
  } catch (e) {
    log(`강제 문제 해결 함수 오류: ${e.message}`);
  }
}

// 문제 해결 버튼 생성 함수 - 상대방 비디오가 안 보일 때 사용자가 직접 해결할 수 있도록 함
function createFixButton() {
  log("문제 해결 버튼 생성");
  
  try {
    // 이미 버튼이 있는지 확인
    if (document.getElementById('fixConnectionButton')) {
      return;
    }
    
    // 버튼 컨테이너 생성 (여러 버튼을 담기 위함)
    const buttonContainer = document.createElement('div');
    buttonContainer.id = 'fixButtonContainer';
    buttonContainer.style.position = 'absolute';
    buttonContainer.style.bottom = '10px';
    buttonContainer.style.left = '0';
    buttonContainer.style.right = '0';
    buttonContainer.style.display = 'flex';
    buttonContainer.style.justifyContent = 'center';
    buttonContainer.style.gap = '10px';
    buttonContainer.style.zIndex = '1000';
    
    // 1. 상대방 화면 강제 연결 버튼
    /*const fixButton = document.createElement('button');
    fixButton.id = 'fixConnectionButton';
    fixButton.textContent = '상대방 화면 강제 연결';
    fixButton.style.padding = '8px 16px';
    fixButton.style.backgroundColor = '#0064E1';
    fixButton.style.color = 'white';
    fixButton.style.border = 'none';
    fixButton.style.borderRadius = '4px';
    fixButton.style.cursor = 'pointer';*/
    
    // 클릭 이벤트 핸들러
    fixButton.onclick = function() {
      log("문제 해결 버튼 클릭됨");
      forceSolveConnectionIssue();
      this.disabled = true;
      this.textContent = '연결 시도 중...';
      
      // 5초 후 버튼 다시 활성화
      setTimeout(() => {
        this.disabled = false;
        this.textContent = '상대방 화면 강제 연결';
      }, 5000);
    };
    
    // 2. 연결 완전 재시작 버튼
    /*const restartButton = document.createElement('button');
    restartButton.id = 'restartConnectionButton';
    restartButton.textContent = '연결 완전 재시작';
    restartButton.style.padding = '8px 16px';
    restartButton.style.backgroundColor = '#dc3545';
    restartButton.style.color = 'white';
    restartButton.style.border = 'none';
    restartButton.style.borderRadius = '4px';
    restartButton.style.cursor = 'pointer';*/
    
    // 클릭 이벤트 핸들러
    restartButton.onclick = function() {
      log("연결 완전 재시작 버튼 클릭됨");
      this.disabled = true;
      this.textContent = '재시작 중...';
      
      // 로컬 스트림 정리
      if (localStream) {
        localStream.getTracks().forEach(track => {
          if (track) track.stop();
        });
        localStream = null;
      }
      
      // 원격 스트림 정리
      if (remoteStream) {
        remoteStream.getTracks().forEach(track => {
          if (track) track.stop();
        });
        remoteStream = null;
      }
      
      // 기존 피어 연결 종료
      if (peerConnection) {
        peerConnection.close();
        peerConnection = null;
      }
      
      // 로컬/원격 비디오 요소 초기화
      const localVideo = document.getElementById('localVideo');
      const remoteVideo = document.getElementById('remoteVideo');
      
      if (localVideo) localVideo.srcObject = null;
      if (remoteVideo) remoteVideo.srcObject = null;
      
      // 1초 대기 후 WebRTC 다시 초기화
      setTimeout(() => {
        initWebRTC();
        
        // 5초 후 버튼 다시 활성화
        setTimeout(() => {
          this.disabled = false;
          this.textContent = '연결 완전 재시작';
        }, 5000);
      }, 1000);
    };
    
    // 버튼 추가
    buttonContainer.appendChild(fixButton);
    buttonContainer.appendChild(restartButton);
    
    // 상태 표시 텍스트는 생성하지 않음 (기존 코드 제거)
    
    // 버튼만 컨테이너에 추가
    const remoteContainer = document.getElementById('remoteVideoContainer');
    if (remoteContainer) {
      remoteContainer.appendChild(buttonContainer);
    } else {
      document.body.appendChild(buttonContainer);
    }
  } catch (e) {
    log(`문제 해결 버튼 생성 오류: ${e.message}`);
  }
}

// 10초 후에 문제 해결 버튼 자동 생성 (상대방 비디오가 계속 보이지 않는 경우)
setTimeout(() => {
  const remoteVideo = document.getElementById('remoteVideo');
  if (remoteVideo && (!remoteVideo.srcObject || remoteVideo.srcObject.getVideoTracks().length === 0 || remoteVideo.paused)) {
    log("10초 후에도 원격 비디오가 정상 작동하지 않아 문제 해결 버튼 생성");
    createFixButton();
    // 문제 자동 해결 시도
    forceSolveConnectionIssue();
  }
}, 10000);

// 마이크 토글 함수
function toggleMicrophone() {
  if (!localStream) {
    log("로컬 스트림이 없어 마이크를 토글할 수 없습니다.");
    if (typeof showToast === 'function') {
      showToast("오류", "마이크를 제어할 수 없습니다. 카메라를 먼저 활성화해주세요.", "error");
    }
    return;
  }

  const audioTracks = localStream.getAudioTracks();
  if (audioTracks.length === 0) {
    log("오디오 트랙이 없습니다.");
    if (typeof showToast === 'function') {
      showToast("오류", "마이크 트랙을 찾을 수 없습니다.", "error");
    }
    return;
  }

  const audioTrack = audioTracks[0];
  isMicrophoneEnabled = !isMicrophoneEnabled;
  audioTrack.enabled = isMicrophoneEnabled;

  // UI 업데이트
  const micBtn = document.getElementById('toggleMicBtn');
  if (micBtn) {
    if (isMicrophoneEnabled) {
      micBtn.className = 'media-btn mic-on';
      micBtn.title = '마이크 끄기';
      micBtn.innerHTML = '<i class="fas fa-microphone"></i>';
    } else {
      micBtn.className = 'media-btn mic-off';
      micBtn.title = '마이크 켜기';
      micBtn.innerHTML = '<i class="fas fa-microphone-slash"></i>';
    }
  }

  // 상대방에게 마이크 상태 알림
  sendMediaStateUpdate('microphone', isMicrophoneEnabled);

  log(`마이크 ${isMicrophoneEnabled ? '켜짐' : '꺼짐'}`);
  if (typeof showToast === 'function') {
    showToast("마이크", `마이크가 ${isMicrophoneEnabled ? '켜졌습니다' : '꺼졌습니다'}`, "info");
  }
}

// 카메라 토글 함수
function toggleCamera() {
  if (!localStream) {
    log("로컬 스트림이 없어 카메라를 토글할 수 없습니다.");
    if (typeof showToast === 'function') {
      showToast("오류", "카메라를 제어할 수 없습니다. 먼저 카메라를 활성화해주세요.", "error");
    }
    return;
  }

  const videoTracks = localStream.getVideoTracks();
  if (videoTracks.length === 0) {
    log("비디오 트랙이 없습니다.");
    if (typeof showToast === 'function') {
      showToast("오류", "카메라 트랙을 찾을 수 없습니다.", "error");
    }
    return;
  }

  const videoTrack = videoTracks[0];
  isCameraEnabled = !isCameraEnabled;
  videoTrack.enabled = isCameraEnabled;

  // UI 업데이트
  const cameraBtn = document.getElementById('toggleCameraBtn');
  if (cameraBtn) {
    if (isCameraEnabled) {
      cameraBtn.className = 'media-btn camera-on';
      cameraBtn.title = '카메라 끄기';
      cameraBtn.innerHTML = '<i class="fas fa-video"></i>';
    } else {
      cameraBtn.className = 'media-btn camera-off';
      cameraBtn.title = '카메라 켜기';
      cameraBtn.innerHTML = '<i class="fas fa-video-slash"></i>';
    }
  }

  // 로컬 비디오 표시/숨김
  const localVideo = document.getElementById('localVideo');
  if (localVideo) {
    if (isCameraEnabled) {
      localVideo.style.opacity = '1';
    } else {
      localVideo.style.opacity = '0.3';
    }
  }

  // 상대방에게 카메라 상태 알림
  sendMediaStateUpdate('camera', isCameraEnabled);

  log(`카메라 ${isCameraEnabled ? '켜짐' : '꺼짐'}`);
  if (typeof showToast === 'function') {
    showToast("카메라", `카메라가 ${isCameraEnabled ? '켜졌습니다' : '꺼졌습니다'}`, "info");
  }
}

// 미디어 상태 업데이트 전송
function sendMediaStateUpdate(mediaType, enabled) {
  if (!stompClient || !stompClient.connected) {
    log("WebSocket 연결이 없어 미디어 상태를 전송할 수 없습니다.");
    return;
  }

  try {
    const message = {
      type: 'media_state',
      mediaType: mediaType, // 'microphone' 또는 'camera'
      enabled: enabled,
      sender: userRole,
      sessionId: sessionId,
      timestamp: Date.now()
    };

    stompClient.send(`/topic/room/${sessionId}/media`, {}, JSON.stringify(message));
    log(`미디어 상태 전송: ${mediaType} = ${enabled}`);
  } catch (e) {
    log(`미디어 상태 전송 오류: ${e.message}`);
  }
}

// 원격 미디어 상태 업데이트 처리
function handleRemoteMediaState(mediaData) {
  if (!mediaData || mediaData.sender === userRole) {
    return; // 자신이 보낸 메시지는 무시
  }

  log(`원격 미디어 상태 수신: ${mediaData.mediaType} = ${mediaData.enabled}`);

  // 상대방의 미디어 상태를 UI에 표시
  const remoteVideoStatus = document.getElementById('remoteVideoStatus');
  if (remoteVideoStatus) {
    let statusText = '';
    
    if (mediaData.mediaType === 'microphone') {
      statusText = mediaData.enabled ? '마이크 켜짐' : '마이크 꺼짐';
    } else if (mediaData.mediaType === 'camera') {
      statusText = mediaData.enabled ? '카메라 켜짐' : '카메라 꺼짐';
      
      // 원격 비디오 표시/숨김
      const remoteVideo = document.getElementById('remoteVideo');
      if (remoteVideo) {
        if (mediaData.enabled) {
          remoteVideo.style.opacity = '1';
        } else {
          remoteVideo.style.opacity = '0.3';
        }
      }
    }
    
    // 상태 메시지 임시 표시
    const originalText = remoteVideoStatus.textContent;
    remoteVideoStatus.textContent = statusText;
    
    // 3초 후 원래 상태로 복원
    setTimeout(() => {
      if (remoteVideoStatus.textContent === statusText) {
        remoteVideoStatus.textContent = originalText;
      }
    }, 3000);
  }

  // 토스트 메시지 표시
  if (typeof showToast === 'function') {
    const roleText = userRole === 'agent' ? '고객' : '상담원';
    const mediaText = mediaData.mediaType === 'microphone' ? '마이크' : '카메라';
    const stateText = mediaData.enabled ? '켜졌습니다' : '꺼졌습니다';
    showToast("상대방 상태", `${roleText}의 ${mediaText}가 ${stateText}`, "info");
  }
}

// 미디어 컨트롤 버튼 상태 업데이트
function updateMediaControlButtons() {
  // 마이크 버튼 상태 업데이트
  const micBtn = document.getElementById('toggleMicBtn');
  if (micBtn) {
    if (isMicrophoneEnabled) {
      micBtn.className = 'media-btn mic-on';
      micBtn.title = '마이크 끄기';
      micBtn.innerHTML = '<i class="fas fa-microphone"></i>';
    } else {
      micBtn.className = 'media-btn mic-off';
      micBtn.title = '마이크 켜기';
      micBtn.innerHTML = '<i class="fas fa-microphone-slash"></i>';
    }
  }

  // 카메라 버튼 상태 업데이트
  const cameraBtn = document.getElementById('toggleCameraBtn');
  if (cameraBtn) {
    if (isCameraEnabled) {
      cameraBtn.className = 'media-btn camera-on';
      cameraBtn.title = '카메라 끄기';
      cameraBtn.innerHTML = '<i class="fas fa-video"></i>';
    } else {
      cameraBtn.className = 'media-btn camera-off';
      cameraBtn.title = '카메라 켜기';
      cameraBtn.innerHTML = '<i class="fas fa-video-slash"></i>';
    }
  }

  log("미디어 컨트롤 버튼 상태 업데이트 완료");
} 
