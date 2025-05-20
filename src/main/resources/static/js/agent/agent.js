  let stompClient = null;
  let contractId = '11';
  let senderType = 'AGENT';
  let senderName = '홍길동';

  function connectChat() {
  	const socket = new SockJS('/ws');
  	stompClient = Stomp.over(socket);
  	stompClient.connect({}, () => {
  		console.log('✅ 채팅 서버 연결됨!');
  		stompClient.subscribe(`/topic/chat/${contractId}`, msg => {
  			const body = JSON.parse(msg.body);
  			showChatMessage(body);
  		});
  	});
  }


document.addEventListener('DOMContentLoaded', function() {
	const calendarEl = document.getElementById('calendar');
	const weeklyTimetable = document.getElementById('weeklyTimetable');
	const timetableBody = document.getElementById('timetableBody');
	const dateRangeEl = document.getElementById('dateRange');
	const scheduleForm = document.getElementById('scheduleForm');
	const saveBtn = document.getElementById('saveBtn');
	connectChat();

	let selectedEvent = null;



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
					// 기존 eventClick 과 동일한 로직 재사용
					selectedEvent = ev;
					const form = document.getElementById('scheduleForm');
					form.date.value = ev.startStr.split('T')[0];
					form.time.value = ev.startStr.split('T')[1].slice(0, 5);
					form.clientName.value = ev.title.replace(' 고객', '');
					form.contractStatus.value = ev.extendedProps.contractStatus || '';
					form.memo.value = ev.extendedProps.memo || '';
					form.email.value = ev.extendedProps.email || '';
					form.phone.value = ev.extendedProps.phone || '';

					// 버튼 토글
					document.getElementById('saveBtn').classList.add('d-none');
					document.getElementById('updateBtn').classList.remove('d-none');
					document.getElementById('deleteBtn').classList.remove('d-none');

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

	// 캘린더 초기화
	const calendar = new FullCalendar.Calendar(calendarEl, {
		initialView: 'dayGridMonth',
		firstDay: 1, // 월요일 시작
		eventTimeFormat: { hour: 'numeric', meridiem: 'narrow' },
		customButtons: {
			addSchedule: {
				text: '스케줄 추가',
				click: () => {
					selectedEvent = null;
					scheduleForm.reset();
					document.getElementById('saveBtn').classList.remove('d-none');
					document.getElementById('updateBtn').classList.add('d-none');
					document.getElementById('deleteBtn').classList.add('d-none');
					new bootstrap.Modal(document.getElementById('scheduleModal')).show();
				}
			}
		},
		headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth listWeek addSchedule' },
		views: { listWeek: { type: 'list', duration: { days: 7 }, buttonText: 'List' } },
		events: [],
		eventClick(info) {
			const ev = info.event;
			selectedEvent = ev;
			const form = document.getElementById('scheduleForm');
			form.date.value = ev.startStr.split('T')[0];
			form.time.value = ev.startStr.split('T')[1].slice(0, 5);
			form.clientName.value = ev.title.replace(' 고객', '');
			form.contractStatus.value = ev.extendedProps.contractStatus || '';
			form.memo.value = ev.extendedProps.memo || '';
			form.email.value = ev.extendedProps.email || '';
			form.phone.value = ev.extendedProps.phone || '';
			document.getElementById('saveBtn').classList.add('d-none');
			document.getElementById('updateBtn').classList.remove('d-none');
			document.getElementById('deleteBtn').classList.remove('d-none');
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
	// ====오늘의 계약====
	loadRecentCompletedContracts();

	// 전체 스케줄 로드
	async function loadSchedules() {
		try {
			const agentId = Number(document.querySelector('input[name="agentId"]').value);
			const res = await fetch(`/agent/schedule/list?agentId=${agentId}`);
			if (!res.ok) throw new Error('스케줄 로드 실패');
			const schedules = await res.json();

			calendar.getEvents().forEach(ev => ev.remove());

			schedules.forEach(c => {
				calendar.addEvent({
					id: String(c.contractId),
					title: `${c.memo ? '' : ''}${c.clientName || ''} 고객`,
					start: c.contractTime,
					backgroundColor: getRandomColor(),
					borderColor: getRandomColor(),
					extendedProps: {
						contractStatus: c.status,
						memo: c.memo,
						email: c.email,
						phone: c.phoneNumber
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
	        <button class="btn btn-sm btn-primary start-webrtc-btn" data-contract-id="${c.contractId}">
	          통화 시작
	        </button>
	      `;
				ul.appendChild(li);
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

			// 중복 예약 체크
			const date = scheduleForm.date.value;
			const time = scheduleForm.time.value;

			// → 기존 코드 중 "중복 예약 체크" 블록 전체를 이걸로 대체
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
					return;  // 여기서 끝
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
		if (saveBtn.disabled) return;

		const form = document.getElementById('scheduleForm');
		const fd = new FormData(form);

		console.log('▶ FormData.clientId =', fd.get('clientId'));


		const payload = {
			email: fd.get('email'),
			contractTime: `${fd.get('date')}T${fd.get('time')}`,
			clientId: Number(fd.get('clientId')),
			agentId: Number(fd.get('agentId')),
			contractTemplateId: Number(fd.get('contractTemplateId')),
			clientName: fd.get('clientName'),
			memo: fd.get('memo')
		};

		const csrfToken = getCookie('XSRF-TOKEN');
		const csrfHeader = 'X-XSRF-TOKEN';

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
			if (!res.ok) throw new Error(result.error || '저장 실패');

			alert(`스케줄이 등록되었습니다.\n초대코드: ${result.invitationCode}`);

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
					phone: fd.get('phone')
				}
			});
			calendar.addEvent({ /* … */ });
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
			console.error('스케줄 저장 에러:', err);
			alert(err.message);
		}
	});

	// 일정 수정
	document.getElementById('updateBtn').addEventListener('click', async e => {
		e.preventDefault();
		const form = document.getElementById('scheduleForm');
		const fd = new FormData(form);

		const newDate = fd.get('date');
		const newTime = fd.get('time');
		const newStart = `${newDate}T${newTime}`;
		const newMemo = fd.get('memo');
		const newClient = fd.get('clientName');

		const payload = {
			contractId: Number(selectedEvent.id),
			clientId: Number(fd.get('clientId')),
			agentId: Number(fd.get('agentId')),
			contractTime: newStart,
			memo: newMemo
		};

		const csrfToken = getCookie('XSRF-TOKEN');

		try {
			const res = await fetch('/agent/schedule/update', {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					'X-XSRF-TOKEN': csrfToken
				},
				body: JSON.stringify(payload)
			});

			const result = await res.json();
			if (!res.ok) throw new Error(result.error || '수정 실패');

			alert('일정이 수정되었습니다.');

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
			alert(err.message);
		}
	});

	document.getElementById('deleteBtn').addEventListener('click', async e => {
		e.preventDefault();
		if (!selectedEvent) return;

		if (!confirm('정말 이 일정을 삭제하시겠습니까?')) return;

		const contractId = selectedEvent.id;
		const csrfToken = getCookie('XSRF-TOKEN');

		try {
			const res = await fetch(`/agent/schedule/delete?contractId=${contractId}`, {
				method: 'DELETE',
				headers: {
					'X-XSRF-TOKEN': csrfToken
				}
			});
			const result = await res.json();
			if (!res.ok) throw new Error(result.error || '삭제 실패');

			selectedEvent.remove();
			updateEventCountBadges();
			loadSchedules();
			if (calendar.view.type === 'listWeek') {
				renderWeeklyTimetable(calendar.view.currentStart);
			}

			loadTodayContracts();

			// 모달 닫기
			const modalEl = document.getElementById('scheduleModal');
			bootstrap.Modal.getInstance(modalEl).hide();
			scheduleForm.reset();
			alert('일정이 삭제되었습니다.');
		} catch (err) {
			alert(err.message);
		}
	});

    // 최근 완료된 계약
    async function loadRecentCompletedContracts() {
    	const agentId = Number(document.querySelector('input[name="agentId"]').value);
    	console.log('[로그] 상담사 ID:', agentId);

    	const res = await fetch(`/agent/recent-completed?agentId=${agentId}`);
    	console.log('[로그] fetch 상태:', res.status);

    	if (!res.ok) {
    		console.error('최근 완료된 계약 로드 실패');
    		return;
    	}

    	const list = await res.json();
    	console.log('[로그] 받아온 계약 리스트:', list);

    	const ul = document.getElementById('recentCompletedList');

        if (!ul) {
            console.warn('[경고] recentCompletedList UL 못 찾음');
            return;
        }

        ul.innerHTML = '';

    	if (list.length === 0) {
    	    console.log('[로그] 완료된 계약 없음');
    		ul.innerHTML =
    			'<li class="list-group-item text-center text-muted py-4">완료된 계약이 없습니다.</li>';
    		return;
    	}

    	list.forEach(c => {
    	    console.log('[로그] 계약 항목:', c);
    		const li = document.createElement('li');
    		li.className =
    			'list-group-item d-flex justify-content-between align-items-center';
    		li.innerHTML = `
    			<div>
    				<strong>${new Date(c.contractTime).toLocaleString()}</strong>
    				<span class="ms-2">${c.clientName}</span>
    				<small class="text-muted ms-2">(${c.email})</small>
    			</div>
    		`;
    		ul.appendChild(li);
    	});
    }

    /* 채팅 구현 */
  function sendChatMessage() {
  	const input = document.getElementById('chatInput');
  	const msg = input.value.trim();
  	if (!msg || !stompClient || !stompClient.connected) {
  		alert('채팅 서버에 연결되어 있지 않습니다.');
  		return;
  	}

  	stompClient.send(`/app/chat/${contractId}`, {}, JSON.stringify({
  		content: msg,
  		senderType,
  		senderName
  	}));

  	input.value = '';
  }

  document.getElementById('sendChatBtn')?.addEventListener('click', sendChatMessage);

  function showChatMessage({ senderType, senderName, content, sentTime }) {
  	const div = document.createElement('div');
  	div.innerHTML = `<strong>${senderType} [${senderName}]</strong>: ${content} <small>${sentTime}</small>`;
  	document.getElementById('chatMessages').appendChild(div);
  }


});
