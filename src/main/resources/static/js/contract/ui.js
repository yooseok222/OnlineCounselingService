// UI 관련 기능

// 토스트 메시지 표시
function showToast(title, message, type = 'info') {
  // 토스트 컨테이너 가져오기
  let toastContainer = document.getElementById('toastContainer');
  
  // 컨테이너가 없으면 생성
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toastContainer';
    document.body.appendChild(toastContainer);
  }
  
  // 토스트 요소 생성
  const toast = document.createElement('div');
  toast.className = 'toast';
  
  // 타입에 따른 아이콘 설정
  let icon = '';
  switch (type) {
    case 'success':
      icon = '<i class="fas fa-check-circle toast-icon"></i>';
      break;
    case 'error':
      icon = '<i class="fas fa-times-circle toast-icon"></i>';
      break;
    case 'warning':
      icon = '<i class="fas fa-exclamation-triangle toast-icon"></i>';
      break;
    case 'info':
    default:
      icon = '<i class="fas fa-info-circle toast-icon"></i>';
      break;
  }
  
  // 토스트 내용 설정
  toast.innerHTML = `
    ${icon}
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      <div class="toast-message">${message}</div>
    </div>
    <div class="toast-close" onclick="this.parentElement.remove()">
      <i class="fas fa-times"></i>
    </div>
  `;
  
  // 토스트 컨테이너에 추가
  toastContainer.appendChild(toast);
  
  // 애니메이션을 위한 지연
  setTimeout(() => {
    toast.classList.add('show');
  }, 10);
  
  // 일정 시간 후 토스트 제거
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 300);
  }, 5000);
}

// 모달 표시 함수
function showModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.style.display = 'block';
  }
}

// 모달 숨김 함수
function hideModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.style.display = 'none';
  }
}

// 스크롤 위치 조정 함수
function adjustScrollPosition(element, offset = 0) {
  if (element) {
    const rect = element.getBoundingClientRect();
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const targetTop = rect.top + scrollTop - offset;
    
    window.scrollTo({
      top: targetTop,
      behavior: 'smooth'
    });
  }
}

// 페이지 로드 시 UI 초기화
document.addEventListener('DOMContentLoaded', function() {
  // 필요한 UI 초기화 작업 수행
  console.log("UI 초기화 완료");
}); 