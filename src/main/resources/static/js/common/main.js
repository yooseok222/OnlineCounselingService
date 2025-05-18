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
  triggerTabList.forEach((triggerEl) => {
    const tabTrigger = new bootstrap.Tab(triggerEl);
    triggerEl.addEventListener("click", (event) => {
      event.preventDefault();
      tabTrigger.show();
    });
  });

  // swiper 스타일 탭 버튼 active 처리
  const navButtons = document.querySelectorAll(".swiper-nav-style button");
  navButtons.forEach((btn) => {
    btn.addEventListener("click", function () {
      navButtons.forEach((b) => b.classList.remove("active")); // 모두 제거
      btn.classList.add("active"); // 현재만 추가
    });
  });

  // 회원가입 페이지 이벤트 리스너 등록
  initRegisterPageEvents();
});

// 차트 초기화 함수
function initCharts() {
  const style = getComputedStyle(document.documentElement);
  const chartBlue =
    style.getPropertyValue("--chart-blue") || "rgba(0, 87, 215, 0.7)";
  const chartGreen =
    style.getPropertyValue("--chart-green") || "rgba(40, 167, 69, 0.7)";
  const chartRed =
    style.getPropertyValue("--chart-red") || "rgba(220, 53, 69, 0.7)";
  const chartGray =
    style.getPropertyValue("--chart-gray") || "rgba(108, 117, 125, 0.7)";

  const contractStatusEl = document.getElementById("contractStatusChart");
  if (contractStatusEl) {
    const noDataMsgExists =
      contractStatusEl.parentNode.querySelector(".no-data");

    if (noDataMsgExists) {
      contractStatusEl.style.display = "none";
    } else {
      const ctx = contractStatusEl.getContext("2d");
      new Chart(ctx, {
        type: "doughnut",
        data: {
          labels: ["진행중", "완료됨", "취소/거절", "만료됨"],
          datasets: [
            {
              data: [0, 0, 0, 0],
              backgroundColor: [chartBlue, chartGreen, chartRed, chartGray],
              borderColor: [
                chartBlue.replace("0.7", "1"),
                chartGreen.replace("0.7", "1"),
                chartRed.replace("0.7", "1"),
                chartGray.replace("0.7", "1"),
              ],
              borderWidth: 1,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: "70%",
          plugins: {
            legend: {
              display: true,
              position: "bottom",
            },
          },
        },
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
        datasets: [
          {
            label: "서명요청 건수",
            data: [0, 0, 0, 0, 0],
            backgroundColor: Array(5).fill(chartBlue),
            borderColor: Array(5).fill(chartBlue.replace("0.7", "1")),
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1 },
          },
        },
        plugins: {
          legend: { display: false },
        },
      },
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
  const prefersDarkMode = window.matchMedia(
    "(prefers-color-scheme: dark)"
  ).matches;
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

// 회원가입 페이지 이벤트 리스너 초기화
function initRegisterPageEvents() {
  // 회원가입 관련 요소가 있는지 확인
  if (!document.getElementById("registerForm")) {
    return; // 회원가입 폼이 없으면 중단
  }

  console.log("회원가입 페이지 이벤트 리스너 초기화");

  // 이메일 중복 확인
  const checkEmailBtn = document.getElementById("checkEmailBtn");
  if (checkEmailBtn) {
    checkEmailBtn.addEventListener("click", function () {
      checkEmail();
    });
  }

  // 전화번호 중복 확인
  const checkPhoneBtn = document.getElementById("checkPhoneBtn");
  if (checkPhoneBtn) {
    checkPhoneBtn.addEventListener("click", function () {
      checkPhoneNumber();
    });
  }

  // 주민번호 중복 확인
  const checkSsnBtn = document.getElementById("checkSsnBtn");
  if (checkSsnBtn) {
    checkSsnBtn.addEventListener("click", function () {
      checkSsn();
    });
  }
}

// 이메일 중복 확인 함수
function checkEmail() {
  const email = document.getElementById("email").value;
  const emailInput = document.getElementById("email");
  const feedbackElement = document.getElementById("emailFeedback");

  if (!email) {
    emailInput.classList.add("is-invalid");
    setFeedback(feedbackElement, "이메일을 입력해주세요.", "error");
    return;
  }

  // 이메일 형식 검증 (정규식은 등록 페이지에서 정의되어 있다고 가정)
  if (typeof EMAIL_REGEX !== "undefined" && !EMAIL_REGEX.test(email)) {
    emailInput.classList.add("is-invalid");
    setFeedback(feedbackElement, "유효한 이메일 형식이 아닙니다.", "error");
    return;
  }

  // API 호출
  $.ajax({
    url: "/api/email/check",
    type: "GET",
    data: { email: email },
    success: function (response) {
      console.log("이메일 중복 확인 결과:", response);

      // response는 ApiResponse 객체 형식 {duplicated: boolean, message: string}
      if (response.duplicated) {
        emailInput.classList.add("is-invalid");
        emailInput.classList.remove("is-valid");
        setFeedback(feedbackElement, response.message, "error");
        window.emailVerified = false;
      } else {
        emailInput.classList.remove("is-invalid");
        emailInput.classList.add("is-valid");
        setFeedback(feedbackElement, response.message, "success");
        window.emailVerified = true;
      }
    },
    error: function () {
      emailInput.classList.add("is-invalid");
      emailInput.classList.remove("is-valid");
      setFeedback(
        feedbackElement,
        "이메일 확인 중 오류가 발생했습니다. 다시 시도해주세요.",
        "error"
      );
      window.emailVerified = false;
    },
  });
}

// 전화번호 중복 확인 함수
function checkPhoneNumber() {
  const phoneNumber = document.getElementById("phoneNumber").value;
  const phoneInput = document.getElementById("phoneNumber");
  const feedbackElement = document.getElementById("phoneNumberFeedback");

  if (!phoneNumber) {
    window.phoneVerified = true; // 전화번호가 없으면 검증 통과로 처리
    phoneInput.classList.remove("is-invalid");
    phoneInput.classList.remove("is-valid");
    setFeedback(feedbackElement, "", "");
    return;
  }

  // 전화번호 형식 검증 (정규식은 등록 페이지에서 정의되어 있다고 가정)
  if (typeof PHONE_REGEX !== "undefined" && !PHONE_REGEX.test(phoneNumber)) {
    phoneInput.classList.add("is-invalid");
    phoneInput.classList.remove("is-valid");
    setFeedback(feedbackElement, "전화번호 형식이 올바르지 않습니다.", "error");
    window.phoneVerified = false;
    return;
  }

  // API 호출
  $.ajax({
    url: "/api/phone/check",
    type: "GET",
    data: { phoneNumber: phoneNumber },
    success: function (response) {
      console.log("전화번호 중복 확인 결과:", response);

      // response는 ApiResponse 객체 형식 {duplicated: boolean, message: string}
      if (response.duplicated) {
        phoneInput.classList.add("is-invalid");
        phoneInput.classList.remove("is-valid");
        setFeedback(feedbackElement, response.message, "error");
        window.phoneVerified = false;
      } else {
        phoneInput.classList.remove("is-invalid");
        phoneInput.classList.add("is-valid");
        setFeedback(feedbackElement, response.message, "success");
        window.phoneVerified = true;
      }
    },
    error: function () {
      phoneInput.classList.add("is-invalid");
      phoneInput.classList.remove("is-valid");
      setFeedback(
        feedbackElement,
        "전화번호 확인 중 오류가 발생했습니다. 다시 시도해주세요.",
        "error"
      );
      window.phoneVerified = false;
    },
  });
}

// 주민번호 중복 확인 함수
function checkSsn() {
  const ssn = document.getElementById("ssn").value;
  const ssnInput = document.getElementById("ssn");
  const feedbackElement = document.getElementById("ssnFeedback");

  if (!ssn) {
    ssnInput.classList.add("is-invalid");
    ssnInput.classList.remove("is-valid");
    setFeedback(
      feedbackElement,
      "주민등록번호는 필수 입력 항목입니다.",
      "error"
    );
    window.ssnVerified = false;
    return;
  }

  // 주민번호 형식 검증 (정규식은 등록 페이지에서 정의되어 있다고 가정)
  if (typeof SSN_REGEX !== "undefined" && !SSN_REGEX.test(ssn)) {
    ssnInput.classList.add("is-invalid");
    ssnInput.classList.remove("is-valid");
    setFeedback(
      feedbackElement,
      "주민등록번호 형식이 올바르지 않습니다.",
      "error"
    );
    window.ssnVerified = false;
    return;
  }

  // API 호출
  $.ajax({
    url: "/api/ssn/check",
    type: "GET",
    data: { ssn: ssn },
    success: function (response) {
      console.log("주민번호 중복 확인 결과:", response);

      // response는 ApiResponse 객체 형식 {duplicated: boolean, message: string}
      if (response.duplicated) {
        ssnInput.classList.add("is-invalid");
        ssnInput.classList.remove("is-valid");
        setFeedback(feedbackElement, response.message, "error");
        window.ssnVerified = false;
      } else {
        ssnInput.classList.remove("is-invalid");
        ssnInput.classList.add("is-valid");
        setFeedback(feedbackElement, response.message, "success");
        window.ssnVerified = true;
      }
    },
    error: function () {
      ssnInput.classList.add("is-invalid");
      ssnInput.classList.remove("is-valid");
      setFeedback(
        feedbackElement,
        "주민등록번호 확인 중 오류가 발생했습니다. 다시 시도해주세요.",
        "error"
      );
      window.ssnVerified = false;
    },
  });
}

// 피드백 메시지 설정 함수
function setFeedback(element, message, type) {
  if (!element) return;

  // type: 'error', 'success', 'warning'
  const iconMap = {
    error: '<i class="bi bi-exclamation-triangle"></i>',
    success: '<i class="bi bi-check-circle"></i>',
    warning: '<i class="bi bi-info-circle"></i>',
  };

  // HTML 요소 직접 삽입 방식 사용
  if (type && message) {
    element.innerHTML = iconMap[type] + " " + message;
    element.className = "feedback-message feedback-" + type;
  } else {
    element.innerHTML = "";
    element.className = "feedback-message";
  }
}
