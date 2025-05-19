// DOM이 로드된 후 실행
document.addEventListener("DOMContentLoaded", function () {
  // 테마 설정 불러오기
  loadTheme();

  // 차트 초기화
  initCharts();

  // 테마 토글 이벤트 리스너 추가
  const themeToggleBtn = document.getElementById("themeToggle");
  if (themeToggleBtn) {
    themeToggleBtn.addEventListener("click", toggleTheme);
  }

  // 사이드바 토글 동작 개선
  const sidebarToggleBtn = document.querySelector(".navbar-toggler");
  const sidebarMenu = document.getElementById("sidebarMenu");

  if (sidebarToggleBtn && sidebarMenu) {
    sidebarToggleBtn.addEventListener("click", function () {
      document.body.classList.toggle("sidebar-open");
    });

    // 화면 크기가 변경될 때 사이드바 자동 조정
    window.addEventListener("resize", function () {
      if (window.innerWidth >= 768) {
        sidebarMenu.classList.add("show");
      } else {
        sidebarMenu.classList.remove("show");
      }
    });
  }

  // 탭 버튼 수동 활성화 
  const triggerTabList = document.querySelectorAll('[data-bs-toggle="pill"]');
  triggerTabList.forEach(triggerEl => {
    const tabTrigger = new bootstrap.Tab(triggerEl);
    triggerEl.addEventListener('click', event => {
      event.preventDefault();
      tabTrigger.show();
    });
  });

  // swiper 스타일 탭 버튼 active 처리
  const navButtons = document.querySelectorAll('.swiper-nav-style button');
  navButtons.forEach((btn) => {
    btn.addEventListener('click', function () {
      navButtons.forEach((b) => b.classList.remove('active')); // 모두 제거
      btn.classList.add('active'); // 현재만 추가
    });
  });
});



// 차트 초기화 함수
function initCharts() {
  const style = getComputedStyle(document.documentElement);
  const chartBlue = style.getPropertyValue("--chart-blue") || "rgba(0, 87, 215, 0.7)";
  const chartGreen = style.getPropertyValue("--chart-green") || "rgba(40, 167, 69, 0.7)";
  const chartRed = style.getPropertyValue("--chart-red") || "rgba(220, 53, 69, 0.7)";
  const chartGray = style.getPropertyValue("--chart-gray") || "rgba(108, 117, 125, 0.7)";

  const contractStatusEl = document.getElementById("contractStatusChart");
  if (contractStatusEl) {
    const noDataMsgExists = contractStatusEl.parentNode.querySelector(".no-data");

    if (noDataMsgExists) {
      contractStatusEl.style.display = "none";
    } else {
      const ctx = contractStatusEl.getContext("2d");
      new Chart(ctx, {
        type: "doughnut",
        data: {
          labels: ["진행중", "완료됨", "취소/거절", "만료됨"],
          datasets: [{
            data: [0, 0, 0, 0],
            backgroundColor: [chartBlue, chartGreen, chartRed, chartGray],
            borderColor: [
              chartBlue.replace("0.7", "1"),
              chartGreen.replace("0.7", "1"),
              chartRed.replace("0.7", "1"),
              chartGray.replace("0.7", "1")
            ],
            borderWidth: 1,
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: "70%",
          plugins: {
            legend: {
              display: true,
              position: "bottom"
            }
          }
        }
      });
    }
  }

  const signRequestEl = document.getElementById("signRequestChart");
  if (signRequestEl) {
    const ctx = signRequestEl.getContext("2d");
    new Chart(ctx, {
      type: "bar",
      data: {
        labels: ["1월", "2월", "3월", "4월", "5월"],
        datasets: [{
          label: "서명요청 건수",
          data: [0, 0, 0, 0, 0],
          backgroundColor: Array(5).fill(chartBlue),
          borderColor: Array(5).fill(chartBlue.replace("0.7", "1")),
          borderWidth: 1,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1 }
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }
}

// 테마 토글 함수
function toggleTheme() {
  const html = document.documentElement;
  const themeIconLight = document.querySelector(".theme-icon-light");
  const themeIconDark = document.querySelector(".theme-icon-dark");

  if (html.getAttribute("data-bs-theme") === "dark") {
    html.setAttribute("data-bs-theme", "light");
    localStorage.setItem("theme", "light");
    if (themeIconLight) themeIconLight.classList.remove("d-none");
    if (themeIconDark) themeIconDark.classList.add("d-none");
  } else {
    html.setAttribute("data-bs-theme", "dark");
    localStorage.setItem("theme", "dark");
    if (themeIconLight) themeIconLight.classList.add("d-none");
    if (themeIconDark) themeIconDark.classList.remove("d-none");
  }
}

// 테마 초기 설정
function loadTheme() {
  const savedTheme = localStorage.getItem("theme");
  const prefersDarkMode = window.matchMedia("(prefers-color-scheme: dark)").matches;
  const html = document.documentElement;
  const themeIconLight = document.querySelector(".theme-icon-light");
  const themeIconDark = document.querySelector(".theme-icon-dark");

  let theme = "light";
  if (savedTheme) {
    theme = savedTheme;
  } else if (prefersDarkMode) {
    theme = "dark";
  }

  html.setAttribute("data-bs-theme", theme);
  if (theme === "dark") {
    if (themeIconLight) themeIconLight.classList.add("d-none");
    if (themeIconDark) themeIconDark.classList.remove("d-none");
  } else {
    if (themeIconLight) themeIconLight.classList.remove("d-none");
    if (themeIconDark) themeIconDark.classList.add("d-none");
  }
}

