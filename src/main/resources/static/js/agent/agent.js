function getCsrf() {
  const token  = document.querySelector('meta[name="_csrf"]').content;
  const header = document.querySelector('meta[name="_csrf_header"]').content;
  return { header, token };
}

// 상태 변경용 유틸 함수
function updateContractStatus(contractId, newStatus) {
  const { header, token } = getCsrf();
  return fetch(`/agent/schedule/status/${contractId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify({ status: newStatus })
  })
  .then(res => {
    if (!res.ok) throw new Error(`상태 변경 실패: ${res.status}`);
    return;
  });
}

let currentPage = 1;
let currentStatus = 'PENDING';
let currentSortOrder = 'DESC';

document.addEventListener('DOMContentLoaded', function() {
	const calendarEl = document.getElementById('calendar');
	const weeklyTimetable = document.getElementById('weeklyTimetable');
	const timetableBody = document.getElementById('timetableBody');
	const dateRangeEl = document.getElementById('dateRange');
	const scheduleForm = document.getElementById('scheduleForm');
	const saveBtn = document.getElementById('saveBtn');
	const contractStatusSelect = scheduleForm.querySelector('[name="contractStatus"]');

	let selectedEvent = null;

	loadStatusCounts();
    showFilteredContracts(currentStatus);

	// 랜덤 파스텔 컬러 생성
	function getRandomColor() {
		const hue = Math.floor(Math.random() * 360);
		return `hsl(${hue},70%,80%)`;
	}

	// 날짜 포맷 YYYY.MM.DD
	function formatYMD(d) {
		return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
	}

	// 상단 날짜 범위 업데이트 (월/주)
	function updateDateRange(viewType, startDate) {
		if (viewType === 'listWeek') {
			const d = new Date(startDate);
			const dayOffset = (d.getDay() + 6) % 7;
			const monday = new Date(d); monday.setDate(d.getDate() - dayOffset);
			const friday = new Date(monday); friday.setDate(monday.getDate() + 4);
			dateRangeEl.textContent = `${formatYMD(monday)} ~ ${formatYMD(friday)}`;
		}
	}

	// 예약 시간 옵션 초기화
	function initTimeOptions() {
		const reservationTimeSelect = document.getElementById('reservationTime');
		if (!reservationTimeSelect) return;
		reservationTimeSelect.innerHTML = '';
		const timeSlots = ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00'];
		timeSlots.forEach(time => {
			const option = document.createElement('option');
			option.value = time;
			option.textContent = time;
			reservationTimeSelect.appendChild(option);
		});
	}

	initTimeOptions();

	// 주간 시간표 렌더러
	function renderWeeklyTimetable(baseDate) {
		const slots = [
			{ label: '09:00 - 10:00', time: '09:00' },
			{ label: '10:00 - 11:00', time: '10:00' },
			{ label: '11:00 - 12:00', time: '11:00' },
			{ label: '12:00 - 13:00 점심시간', lunch: true },
			{ label: '13:00 - 14:00', time: '13:00' },
			{ label: '14:00 - 15:00', time: '14:00' },
			{ label: '15:00 - 16:00', time: '15:00' },
			{ label: '16:00 - 17:00', time: '16:00' },
			{ label: '17:00 - 18:00', time: '17:00' },
		];

		// 월요일 기준 계산 
		const td = new Date(baseDate);
		const offset = td.getDay() === 0 ? -6 : 1 - td.getDay();
		const monday = new Date(td);
		monday.setDate(td.getDate() + offset);
		const weekStart = new Date(monday);
		weekStart.setHours(0, 0, 0, 0);
		const weekEnd = new Date(weekStart);
		weekEnd.setDate(weekStart.getDate() + 5);
		weekEnd.setHours(0, 0, 0, 0);

		timetableBody.innerHTML = '';


		slots.forEach(def => {
			const tr = document.createElement('tr');
			if (def.lunch) {
				tr.classList.add('lunch-break-row');
				const td1 = document.createElement('td');
				td1.textContent = def.label;
				const td2 = document.createElement('td');
				td2.colSpan = 5;
				tr.append(td1, td2);
			} else {
				const td1 = document.createElement('td'); td1.textContent = def.label;
				tr.appendChild(td1);
				for (let d = 0; d < 5; d++) {
					const td = document.createElement('td');
					td.className = 'timetable-slot';
					td.dataset.day = d + 1;
					td.dataset.time = def.time;
					tr.appendChild(td);
				}
			}
			timetableBody.appendChild(tr);
		});

		const reservationTimeSelect = document.getElementById('reservationTime');
		reservationTimeSelect.innerHTML = '';
		const timeSlots = ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00', '16:00', '17:00'];
		timeSlots.forEach(time => {
			const option = document.createElement('option');
			option.value = time;
			option.textContent = time;
			reservationTimeSelect.appendChild(option);
		});

		let isSubmitting = false;

        saveBtn.addEventListener('click', function(e) {
          e.preventDefault();

          if (isSubmitting) return;   // 이미 전송 중이면 아무 동작도 안 함
          isSubmitting = true;
          saveBtn.disabled = true;

          // 여러분의 저장 로직(AJAX, fetch, form.submit() 등)
          fetch('/api/schedule', {
            method: 'POST',
            body: new FormData(scheduleForm),
            headers: { 'X-CSRF-TOKEN': getCsrf().token }
          })
          .then(res => {
            // 성공/실패 처리…
          })
          .finally(() => {
            isSubmitting = false;
            saveBtn.disabled = false;
          });
        });

		// 이번 주 범위 스케줄만 삽입
		calendar.getEvents().forEach(ev => {
			const dt = new Date(ev.start);
			const ts = dt.getTime();
			// 이번 주 월요일 00:00 <= 이벤트 < 토요일 00:00
			if (ts < weekStart.getTime() || ts >= weekEnd.getTime()) {
				return;
			}
			const dow = dt.getDay();   // 1=월 … 5=금
			const hh = dt.toTimeString().substr(0, 5);
			const cell = timetableBody.querySelector(
				`td.timetable-slot[data-day="${dow}"][data-time="${hh}"]`
			);
			if (cell) {
				const div = document.createElement('div');
				const col = ev.backgroundColor || ev.borderColor || '#0d6efd';
				div.style.backgroundColor = col;
				div.style.color = '#000';
				div.className = 'rounded p-1';
				div.textContent = ev.title;
				div.style.cursor = 'pointer';
				div.addEventListener('click', () => {
					selectedEvent = ev;
					const form = document.getElementById('scheduleForm');

					form.date.value = ev.startStr.split('T')[0];
					const hhmm = ev.startStr.split('T')[1].slice(0, 5);
					document.getElementById('reservationTime').value = hhmm;

					form.clientName.value = ev.title.replace(' 고객', '');
                    form.contractStatus.value = ev.extendedProps.contractStatus || 'PENDING';
					form.memo.value = ev.extendedProps.memo || '';
					form.email.value = ev.extendedProps.email || '';
					form.phone.value = ev.extendedProps.phone || '';

                    document.getElementById('templateSelect').value = ev.extendedProps.contractTemplateId ?? '';

					// 버튼 토글
					document.getElementById('saveBtn').classList.add('d-none');
					document.getElementById('updateBtn').classList.remove('d-none');

					// 모달 열기
					new bootstrap.Modal(document.getElementById('scheduleModal')).show();
				});

				cell.appendChild(div);
			}
		});
	}
	function updateEventCountBadges() {
		const dayCells = document.querySelectorAll('.fc-daygrid-day');
		dayCells.forEach(cell => {
			const dateStr = cell.getAttribute('data-date');
			if (!dateStr) return;
			const oldBadge = cell.querySelector('.fc-day-count');
			if (oldBadge) oldBadge.remove();
			const count = calendar.getEvents().filter(ev =>
				ev.startStr.startsWith(dateStr)
			).length;
			if (count > 0) {
				const badge = document.createElement('div');
				badge.className = 'fc-day-count';
				badge.innerText = `${count}건`;
				cell.querySelector('.fc-daygrid-day-top')?.appendChild(badge);
			}
		});
	}

	// 통화하기 입장
	function canEnterCall(reservationDateStr, reservationTimeStr) {
      /* 시간 무시하고 테스트 하기 return true 지워야함 */
      return true;
      const now = new Date();

      // 로컬 날짜 문자열 (YYYY-MM-DD) 생성
      const localToday = [
        now.getFullYear(),
        String(now.getMonth() + 1).padStart(2, '0'),
        String(now.getDate()).padStart(2, '0')
      ].join('-');

      const reservation = new Date(`${reservationDateStr}T${reservationTimeStr}:00`);

      // 1) 날짜 확인
      if (localToday !== reservationDateStr) {
         alert('입장 불가: 오늘 날짜의 예약이 아닙니다.');
         return false;
      }

      // 2) 예약 5분 전 확인
      const earliest = new Date(reservation.getTime() - 5 * 60 * 1000);
        if (now < earliest) {
          alert('입장 불가: 예약시간 5분 전부터 입장 가능합니다.');
          return false;
       }

       // 3) 예약시간 30분 경과 후 입장 불가
       const latest = new Date(reservation.getTime() + 30 * 60 * 1000);
         if (now > latest) {
           alert('입장 불가: 예약시간으로부터 30분이 경과하여 더 이상 입장할 수 없습니다.');
           return false;
       }

      return true;
    }


	// 캘린더 초기화
	const calendar = new FullCalendar.Calendar(calendarEl, {
		initialView: 'dayGridMonth',
		firstDay: 1, // 월요일 시작좋
		eventTimeFormat: { hour: 'numeric', meridiem: 'narrow' },
		customButtons: {
			addSchedule: {
				text: '스케줄 추가',
				click: async () => {
                    const form = document.getElementById('scheduleForm');
                    form.reset();
                    selectedEvent = null;

                    const tmplSelect = document.getElementById('templateSelect');
                    tmplSelect.innerHTML = '<option value="">-- 템플릿을 선택하세요 --</option>';
                    tmplSelect.value = '';

                    const companyId = document.getElementById('companyIdInput').value;

                    try {
                             const res = await fetch(`/agent/contract-templates?companyId=${companyId}`);
                             const templates = await res.json();
                             templates.forEach(t => {
                             const opt = document.createElement('option');
                             opt.value = t.contractTemplateId;
                             opt.textContent = t.contractName;
                             tmplSelect.appendChild(opt);
                             });
                           } catch (e) {
                             console.error('템플릿 로드 실패', e);
                           }
					document.getElementById('emailFeedback')?.remove();

					document.querySelector('#scheduleModal .modal-title').textContent = '스케줄 추가';

					contractStatusSelect.value = 'PENDING';
                    contractStatusSelect.closest('.mb-3').style.display = 'none';

					document.getElementById('saveBtn').classList.remove('d-none');
					document.getElementById('updateBtn').classList.add('d-none');
					new bootstrap.Modal(document.getElementById('scheduleModal')).show();
				}
			}
		},
		headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth listWeek addSchedule' },
		views: { listWeek: { type: 'list', duration: { days: 7 }, buttonText: 'List' } },
		events: [],
		eventClick: async function(info) {
			const ev = info.event;
			selectedEvent = ev;

			const companyId = document.querySelector('input[name="companyId"]').value;
            const tmplSelect = document.getElementById('templateSelect');
            tmplSelect.innerHTML = '<option value="">-- 템플릿을 선택하세요 --</option>';


            try {
                  const res = await fetch(`/agent/contract-templates?companyId=${companyId}`);
                  const templates = await res.json();
                  templates.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = t.contractTemplateId;
                    opt.textContent = t.contractName;
                    tmplSelect.appendChild(opt);
                  });
                    tmplSelect.value = ev.extendedProps.contractTemplateId ?? '';
                } catch (e) {
                  console.error('템플릿 로드 실패', e);
            }

			const form = document.getElementById('scheduleForm');

			form.date.value = ev.startStr.split('T')[0];

			const hhmm = ev.startStr.split('T')[1].slice(0, 5);
			document.getElementById('reservationTime').value = hhmm;

			form.clientName.value = ev.title.replace(' 고객', '');
			form.memo.value = ev.extendedProps.memo || '';
			form.email.value = ev.extendedProps.email || '';
			form.phone.value = ev.extendedProps.phone || '';

			document.getElementById('templateSelect').value = ev.extendedProps.contractTemplateId ?? '';
			document.getElementById('emailFeedback')?.remove();

			document.querySelector('#scheduleModal .modal-title').textContent = '스케줄 수정';

	            contractStatusSelect.closest('.mb-3').style.display = 'block';
                const statusValue = ev.extendedProps.contractStatus;
                if (statusValue === 'PENDING' || statusValue === 'CANCELLED') {
                    contractStatusSelect.value = statusValue;
                } else {
                    contractStatusSelect.value = 'PENDING';
                }

            document.querySelector('#scheduleModal .modal-title').textContent = '스케줄 수정';
			contractStatusSelect.closest('.mb-3').style.display = 'block';
			document.getElementById('saveBtn').classList.add('d-none');
			document.getElementById('updateBtn').classList.remove('d-none');

			new bootstrap.Modal(document.getElementById('scheduleModal')).show();
		},

		datesSet(info) {
			if (info.view.type === 'listWeek') {
				// 주간 리스트
				dateRangeEl.style.display = '';
				const harness = calendarEl.querySelector('.fc-view-harness');
				harness && (harness.style.display = 'none');
				weeklyTimetable.style.display = 'block';
				renderWeeklyTimetable(info.start);
				updateDateRange('listWeek', info.start);
				calendarEl.querySelector('.fc-toolbar-title').textContent = dateRangeEl.textContent;
			} else {
				//월간 리스트
				dateRangeEl.style.display = 'none';
				const harness = calendarEl.querySelector('.fc-view-harness');
				harness && (harness.style.display = 'block');
				weeklyTimetable.style.display = 'none';
				calendarEl.querySelector('.fc-toolbar-title').textContent = info.view.title;
				updateEventCountBadges();
			}
		},
		dayCellDidMount(arg) {
			const dateStr = arg.date.toISOString().slice(0, 10);
			const count = calendar.getEvents().filter(ev => ev.startStr.startsWith(dateStr)).length;
			if (count > 0) {
				const badge = document.createElement('div');
				badge.className = 'fc-day-count';
				badge.innerText = `${count}건`;
				arg.el.querySelector('.fc-daygrid-day-top').append(badge);
			}
		},
	});
	calendar.render();

	// 오늘의 계약
	const todayContractsDateEl = document.getElementById('todayContractsDate');
	let currentDate = new Date();

	function renderContractsDate() {
		todayContractsDateEl.textContent = formatYMD(currentDate);
	}

	document.getElementById('prevDayBtn').addEventListener('click', () => {
		currentDate.setDate(currentDate.getDate() - 1);
		renderContractsDate();
		loadTodayContracts();
	});
	document.getElementById('nextDayBtn').addEventListener('click', () => {
		currentDate.setDate(currentDate.getDate() + 1);
		renderContractsDate();
		loadTodayContracts();
	});

	renderContractsDate();
	loadTodayContracts();

	// 전체 스케줄 로드
	async function loadSchedules() {
		 try {
            const agentId = Number(document.querySelector('input[name="agentId"]').value);

			if (!agentId || isNaN(agentId)) {
            		throw new Error('agentId가 유효하지 않습니다: ' + agentId);
                }

            const res = await fetch(`/agent/schedule/list?agentId=${agentId}`);
                if (!res.ok) {
                  const errText = await res.text();
                  throw new Error(`스케줄 로드 실패: ${res.status} ${errText}`);
                }
            const schedules = await res.json();

			calendar.getEvents().forEach(ev => ev.remove());

			 schedules.forEach(c => {
               if (c.status !== 'PENDING') return;
               calendar.addEvent({
                 id:      String(c.contractId),
                 title:   `${c.clientName} 고객`,
                 start:   c.contractTime,
                 backgroundColor: getRandomColor(),
                 borderColor:     getRandomColor(),
                 extendedProps: {
                 contractStatus: c.status,
                 memo:           c.memo,
                 email:          c.email,
                 phone:          c.phoneNumber,
                 contractTemplateId: c.templateId
                 }
               });
             });



			updateEventCountBadges();
		} catch (err) {
			console.error('스케줄 로드 에러:', err);
		}
	}

	loadSchedules();
	loadTodayContracts();

	// 오늘의 계약 불러오기
	async function loadTodayContracts() {

		try {
			const agentId = Number(document.querySelector('input[name="agentId"]').value);
			//const iso = currentDate.toISOString().slice(0, 10); // YYYY-MM-DD
			const year = currentDate.getFullYear();
			const month = String(currentDate.getMonth() + 1).padStart(2, '0');
			const day = String(currentDate.getDate()).padStart(2, '0');
			const iso = `${year}-${month}-${day}`;

			const res = await fetch(
				`/agent/today-contracts?agentId=${agentId}&date=${iso}`
			);

			if (!res.ok) throw new Error('오늘 계약 로드 실패');
			const list = await res.json();

			const ul = document.querySelector('#todayContractsList ul');
			ul.innerHTML = '';
			if (list.length === 0) {
				ul.innerHTML = '<li class="list-group-item text-center text-muted">오늘 계약 예정이 없습니다.</li>';
				return;
			}
			 list.forEach(c => {
                const li = document.createElement('li');
                li.className = 'list-group-item d-flex justify-content-between align-items-center';
                li.innerHTML = `
                  <div>
                    <strong>${c.time}</strong>
                    <span class="ms-2">${c.clientName}</span>
                    <small class="text-muted ms-2">(${c.email})</small>
                    <span class="badge bg-secondary ms-3">${c.invitationCode || '–'}</span>
                  </div>
                  <button
                    class="btn btn-sm btn-primary start-webrtc-btn"
                    data-contract-id="${c.contractId}"
                    data-date="${iso}"
                    data-time="${c.time}">
                    통화 시작
                  </button>
                `;
                ul.appendChild(li);
              });
               document.querySelectorAll('.start-webrtc-btn').forEach(btn => {
                 btn.addEventListener('click', async () => {
                   const contractId = btn.dataset.contractId;
                   const dateStr    = btn.dataset.date;
                   const timeStr    = btn.dataset.time;

                   // 1) 시간/날짜 체크
                   if (!canEnterCall(dateStr, timeStr)) return;

                   try {
                     // 2) PENDING → IN_PROGRESS 로 상태 변경
                     await updateContractStatus(contractId, 'IN_PROGRESS');

                     // 3) 상태 변경 후 방으로 이동
                     window.location.href = `/contract/room?contractId=${contractId}&role=agent`;
                   } catch (err) {
                     console.error('상태 변경 오류', err);
                     alert('통화 상태 변경에 실패했습니다. 다시 시도해주세요.');
                   }
                 });
               });

		} catch (err) {
			console.error(err);
		}
	}



	// 고객 이메일 검색
	document.getElementById('searchClientBtn').addEventListener('click', async () => {
		const emailInput = document.querySelector('input[name="email"]');
		const clientNameInput = document.querySelector('input[name="clientName"]');
		const phoneInput = document.querySelector('input[name="phone"]');
		const clientIdInput = document.querySelector('input[name="clientId"]');
		const email = emailInput.value.trim();
		const feedbackId = 'emailFeedback';

		let fb = document.getElementById(feedbackId);
		if (!fb) {
			fb = document.createElement('div');
			fb.id = feedbackId;

			const formGroup = emailInput.closest('.mb-3') || emailInput.parentElement;
			formGroup.appendChild(fb);
			fb.className = 'form-text mt-1';
		}
		fb.style.display = 'block';

		if (!email) {
			fb.textContent = '이메일을 입력해주세요.';
			fb.style.color = 'red';
			return;
		}
		try {
			const res = await fetch(`/agent/client/search?email=${encodeURIComponent(email)}`);
			if (!res.ok) throw new Error('Not Found');
			const client = await res.json();

			clientNameInput.value = client.name;
			phoneInput.value = client.phoneNumber;
			clientIdInput.value = client.clientId;
			document.getElementById('clientIdInput').value = client.clientId;

			fb.textContent = '고객 정보를 불러왔습니다.';
			fb.style.color = 'green';
			fb.style.display = 'block';

			// 중복 예약 체크
			const date = scheduleForm.date.value;
			const time = scheduleForm.time.value;

			if (date && time) {
				const ct = `${date}T${time}`;

				// 1) 동일 고객 중복 체크
				const clientCheck = await fetch(
					`/agent/schedule/check?clientId=${client.clientId}&contractTime=${encodeURIComponent(ct)}`
				).then(r => r.json());

				if (clientCheck.exists) {
					fb.textContent = '⚠️ 이미 이 시간에 해당 고객으로 잡힌 일정이 있습니다.';
					fb.classList.remove('text-success');
					fb.classList.add('text-danger');
					saveBtn.disabled = true;
					return;
				}

				// 2) 동일 상담사 중복 체크
				const agentId = document.querySelector('input[name="agentId"]').value;
				const agentCheck = await fetch(
					`/agent/schedule/checkAgent?agentId=${agentId}&contractTime=${encodeURIComponent(ct)}`
				).then(r => r.json());

				if (agentCheck.exists) {
					fb.textContent = '⚠️ 이미 이 시간에 상담 일정이 있습니다.';
					fb.classList.remove('text-success');
					fb.classList.add('text-danger');
					saveBtn.disabled = true;
					return;
				}

				// 3) 둘 다 통과했을 때만
				fb.textContent = '📅 예약 가능 시간입니다.';
				fb.classList.remove('text-danger');
				fb.classList.add('text-success');
				saveBtn.disabled = false;
			}


		} catch (err) {
			fb.textContent = '존재하지 않는 이메일입니다.';
			fb.classList.remove('text-success');
			fb.classList.add('text-danger');
			clientNameInput.value = '';
			phoneInput.value = '';
			clientIdInput.value = '';
		}
	});


	// 날짜/시간 변경 시 알림과 버튼 상태 초기화
	scheduleForm.date.addEventListener('change', () => {
		document.getElementById('emailFeedback')?.remove();
		saveBtn.disabled = false;
	});
	scheduleForm.time.addEventListener('change', () => {
		document.getElementById('emailFeedback')?.remove();
		saveBtn.disabled = false;
	});

	// 쿠키에서 name에 해당하는 값 꺼내기
	function getCookie(name) {
		const v = `; ${document.cookie}`;
		const parts = v.split(`; ${name}=`);
		return parts.length === 2 ? parts.pop().split(';').shift() : null;
	}

	// 스케줄 저장
	saveBtn.addEventListener('click', async e => {
		e.preventDefault();
		const { header: csrfHeader, token: csrfToken } = getCsrf();

		if (saveBtn.disabled) return;

		const form = document.getElementById('scheduleForm');
		const fd = new FormData(form);

        const date = fd.get('date');
        const time = fd.get('time');


        if (!date || !time) {
            alert('날짜와 시간을 모두 입력해주세요.');
            return;
        }

		const payload = {
			  email:       fd.get('email'),
              contractTime:`${fd.get('date')}T${fd.get('time')}`,
              clientId:    Number(fd.get('clientId')),
              agentId:     Number(fd.get('agentId')),
              companyId:   Number(fd.get('companyId')),
              templateId: Number(fd.get('contractTemplateId')),
              memo:        fd.get('memo'),
              status:      'PENDING'
		};

		try {

			const res = await fetch('/agent/schedule/add', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					[csrfHeader]: csrfToken
				},
				body: JSON.stringify(payload)
			});

			const result = await res.json();
			const list = result.contracts ?? [];
            const totalPages = result.totalPages ?? 1;
            const ul = document.getElementById('filteredContractsList');

            if (list.length === 0) {
              ul.innerHTML = '<li class="list-group-item text-muted text-center">계약이 없습니다.</li>';
            } else {
              ul.innerHTML = '';
              list.forEach(contract => {
                const li = document.createElement('li');
                li.className = 'list-group-item';
                li.textContent = `${contract.clientName} — ${contract.contractTime}`;
                ul.appendChild(li);
              });
              renderPagination(totalPages);
            }


			if (!res.ok) throw new Error(result.error || '저장 실패');

            await Swal.fire({
                  icon: 'success',
                  title: '일정 등록 완료',
                  html: `초대코드: <strong>${result.invitationCode}</strong>`,
                  confirmButtonText: '확인'
            });


            loadStatusCounts();
            showFilteredContracts(currentStatus);
            loadSchedules();

            bootstrap.Modal.getInstance(scheduleModal).hide();
            scheduleForm.reset();

			// 캘린더에 이벤트 추가
			calendar.addEvent({
				title: `${fd.get('clientName')} 고객`,
				start: payload.contractTime,
				backgroundColor: getRandomColor(),
				borderColor: getRandomColor(),
				extendedProps: {
					contractStatus: payload.status,
					memo: payload.memo,
					email: payload.email,
					phone: fd.get('phone'),
					contractTemplateId: payload.contractTemplateId
				}
			});
			calendar.addEvent({ /* … */ });
			updateEventCountBadges();
			loadSchedules();
			if (calendar.view.type === 'listWeek') {
				renderWeeklyTimetable(calendar.view.currentStart);
			}

			const modalEl = document.getElementById('scheduleModal');
			bootstrap.Modal.getInstance(modalEl).hide();
			form.reset();
		} catch (err) {
            Swal.fire({ icon: 'error', title: '저장 실패', text: err.message });
		}
	});

	// 일정 수정
	document.getElementById('updateBtn').addEventListener('click', async e => {
		e.preventDefault();
	    const { header: csrfHeader, token: csrfToken } = getCsrf();

		const form = document.getElementById('scheduleForm');
		const fd = new FormData(form);

		const newDate = fd.get('date');
		const newTime = fd.get('time');
		const newStart = `${newDate}T${newTime}`;
		const newMemo = fd.get('memo');
		const newClient = fd.get('clientName');

		const payload = {
		     contractId:           Number(selectedEvent.id),
             clientId:             Number(fd.get('clientId')),
             agentId:              Number(fd.get('agentId')),
             companyId:            Number(fd.get('companyId')),
             contractTemplateId:   Number(fd.get('contractTemplateId')),
             contractTime:         `${fd.get('date')}T${fd.get('time')}`,
             memo:                 fd.get('memo'),
             status:               fd.get('contractStatus')
		};

		try {
			const res = await fetch('/agent/schedule/update', {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					[csrfHeader]: csrfToken
				},
				body: JSON.stringify(payload)
			});

			const result = await res.json();
			if (!res.ok) throw new Error(result.error || '수정 실패');

			await Swal.fire({
                  icon: 'success',
                  title: '일정 수정 완료',
                  confirmButtonText: '확인'
            });

            loadStatusCounts();
            showFilteredContracts(currentStatus);
            loadSchedules();

            bootstrap.Modal.getInstance(scheduleModal).hide();
            scheduleForm.reset();

			selectedEvent.setStart(newStart);
			selectedEvent.setProp('title', `${newClient} 고객`);
			selectedEvent.setExtendedProp('memo', newMemo);
			updateEventCountBadges();
			loadSchedules();
			if (calendar.view.type === 'listWeek') {
				renderWeeklyTimetable(calendar.view.currentStart);
			}

			loadTodayContracts();

			const modalEl = document.getElementById('scheduleModal');
			bootstrap.Modal.getInstance(modalEl).hide();
			form.reset();
		} catch (err) {
             Swal.fire({ icon: 'error', title: '수정 실패', text: err.message });
		}
	});



});


// 계약 상태에 따라 count
async function loadStatusCounts() {
  const res = await fetch('/agent/contract-status-counts');
  if (!res.ok) return;

  const counts = await res.json();
  document.querySelector('#count-pending .status-value').textContent = counts.PENDING || 0;
  document.querySelector('#count-inprogress .status-value').textContent = counts.IN_PROGRESS || 0;
  document.querySelector('#count-completed .status-value').textContent = counts.COMPLETED || 0;
  document.querySelector('#count-cancelled .status-value').textContent = counts.CANCELLED || 0;
}

function setSortOrder(order, button) {
  currentSortOrder = order;
  currentPage = 1;
  showFilteredContracts(currentStatus);

  // 버튼 UI 상태 업데이트
  const buttons = document.querySelectorAll('#sortOrderButtons button');
  buttons.forEach(btn => {
    btn.classList.remove('active', 'btn-primary');
    btn.classList.add('btn-outline-secondary');
  });

  button.classList.remove('btn-outline-secondary');
  button.classList.add('btn-primary', 'active');
}

function getStatusColor(status) {
  switch (status) {
    case 'PENDING': return 'warning';     // 노랑
    case 'IN_PROGRESS': return 'info';    // 파랑
    case 'COMPLETED': return 'success';   // 초록
    case 'CANCELLED': return 'danger';    // 빨강
    default: return 'secondary';
  }
}

async function showFilteredContracts(status) {
  currentStatus = status;

  try {
    const res = await fetch(`/agent/contracts-by-status?status=${status}&sort=${currentSortOrder}&page=${currentPage}&size=10`);
    if (!res.ok) throw new Error('계약 목록 조회 실패');

    const result = await res.json();
    const list = result.contracts;
    const totalPages = result.totalPages;

    const ul = document.getElementById('filteredContractsList');
    ul.innerHTML = '';

    if (!list || list.length === 0) {
      ul.innerHTML = '<li class="list-group-item text-muted text-center">계약이 없습니다.</li>';
      ul.innerHTML = `
            <li class="list-group-item">
             <div class="d-flex justify-content-center">
                <small class="text-muted mb-0">계약이 없습니다.</small>
              </div>
            </li>
          `;
      return;
    }

    list.forEach(contract => {
      const li = document.createElement('li');
      li.className = 'list-group-item';
      li.innerHTML = `
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center">
          <div class="mb-2 mb-md-0">
            <strong class="text-primary">${contract.clientName}</strong> 고객
            <br class="d-md-none" />
            <small class="text-muted">${contract.contractTime}</small>
          </div>
          <span class="badge bg-${getStatusColor(contract.status)} ms-md-3">${contract.status}</span>
        </div>
      `;
      ul.appendChild(li);
    });

    renderPagination(totalPages);

  } catch (err) {
    console.error('[계약 목록 조회 오류]', err);
  }
}

function renderPagination(totalPages) {
  const container = document.getElementById('paginationContainer');
  container.innerHTML = '';

  if (totalPages <= 1) return;

  for (let i = 1; i <= totalPages; i++) {
    const btn = document.createElement('button');
    btn.className = `btn btn-sm me-1 ${i === currentPage ? 'btn-primary' : 'btn-outline-secondary'}`;
    btn.textContent = i;
    btn.addEventListener('click', () => {
      currentPage = i;
      showFilteredContracts(currentStatus);
    });
    container.appendChild(btn);
  }
}
