package kr.or.kosa.visang.domain.agent.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.or.kosa.visang.domain.contractTemplate.model.ContractTemplate;
import kr.or.kosa.visang.domain.contractTemplate.repository.ContractTemplateMapper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.ISpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kr.or.kosa.visang.common.file.FileStorageService;
import kr.or.kosa.visang.domain.agent.model.Agent;
import kr.or.kosa.visang.domain.agent.model.UpdateAgentDto;
import kr.or.kosa.visang.domain.agent.repository.AgentMapper;
import kr.or.kosa.visang.domain.client.model.Client;
import kr.or.kosa.visang.domain.client.repository.ClientMapper;
import kr.or.kosa.visang.domain.contract.model.Contract;
import kr.or.kosa.visang.domain.contract.model.Page;
import kr.or.kosa.visang.domain.contract.model.Schedule;
import kr.or.kosa.visang.domain.contract.repository.ContractMapper;
import kr.or.kosa.visang.domain.invitation.model.Invitation;
import kr.or.kosa.visang.domain.invitation.repository.InvitationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final FileStorageService fileStorageService;

    private final ISpringTemplateEngine templateEngine;

    private final ContractMapper contractMapper;
    private final InvitationMapper invitationMapper;
    private final ClientMapper clientMapper;
    private final JavaMailSender mailSender;
    private final ContractTemplateMapper contractTemplateMapper;

    @Value("${spring.mail.username}")
    private String Email;

    // 에이전트 목록 조회
    public List<Agent> getAgentList() {
        return agentMapper.selectAllAgents();
    }

    // 에이전트 추가
    public void addAgent() {
        System.out.println("addAgent() called");
    }

    // 조건 검색
    public List<Agent> searchAgent(String name, String email, String state) {
        return agentMapper.selectAgentByCondition(name, email, state);
    }

    // 상세정보 조회
    public Agent getAgentInfo(Long id) {
        return agentMapper.selectAgentById(id);
    }

    // 에이전트 상세정보
    public void getAgentDetail(Long id) {
        agentMapper.selectAgentById(id);
    }

    // 에이전트 수정
    public int updateAgent(Long id, UpdateAgentDto agent) {
        // 에이전트를 수정하는 로직을 구현합니다.
        // 프로필 이미지 저장 처리
        if (agent.getProfileImage() != null && !agent.getProfileImage().isEmpty()) {
            try {
                String imageUrl = fileStorageService.saveProfileImage(agent.getProfileImage(), id);
                agent.setProfileImageUrl(imageUrl); // 프로필 경로 설정
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패", e);
            }
        }
        // 이미지가 없는 경우 이전 이미지 유지
//        else {
//            // 이미지가 없으면 기본 이미지 설정
//            agent.setProfileImageUrl("/images/default-profile.png");
//        }

        // Mapper를 통해 수정
        agent.setAgentId(id); // 에이전트 ID 설정
        return agentMapper.updateAgent(agent);
    }

    // 에이전트 상태 변경
    public void updateAgentStatus(Long id, String state) {
        agentMapper.updateAgentStatus(id, state);
    }

    // 에이전트 비밀번호 변경
    public void updateAgentPassword(Long id, String password) {
        agentMapper.updateAgentPassword(id, password);
    }

    public Client findByEmail(String email) {
        return clientMapper.findByEmail(email);
    }

    @Transactional
    public Map<String, String> addSchedule(Schedule dto, HttpServletRequest request) throws MessagingException {

        LocalDateTime time = dto.getContractTime();

        // 고객 존재 확인
        if (dto.getClientId() == null) {
            Client client = clientMapper.findByEmail(dto.getEmail());
            if (client == null) {
                throw new IllegalArgumentException("해당 이메일의 고객이 없습니다.");
            }
            dto.setClientId(client.getClientId());
        }

        int agentExisting = contractMapper.countByAgentAndTime(dto.getAgentId(), time);
        if (agentExisting > 0) {
            throw new IllegalArgumentException(String.format("이미 %s에 다른 상담 일정이 있습니다.",
                    time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }

        int existing = contractMapper.countByClientAndTime(dto.getClientId(), time);
        if (existing > 0) {
            throw new IllegalArgumentException(String.format("이미 %s에 예약된 일정이 있습니다.",
                    time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }

        Contract c = new Contract();
        c.setClientId(dto.getClientId());
        c.setAgentId(dto.getAgentId());
        c.setContractTime(dto.getContractTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        c.setCompanyId(dto.getCompanyId());
        c.setContractTemplateId(dto.getContractTemplateId());
        c.setMemo(dto.getMemo());
        c.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        c.setStatus("PENDING");

        contractMapper.insertSchedule(c);

        // 초대코드 생성
        String timePart = dto.getContractTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String randPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
        String code = timePart + "-" + randPart;

        // 상담 세션 ID 생성 (contractId 기반으로 결정론적 생성)
        String consultingSessionId = generateSessionId(c.getContractId());

        // INVITATION 삽입
        Invitation inv = new Invitation();
        inv.setContractId(c.getContractId());
        inv.setInvitationCode(code);
        inv.setCreatedAt(LocalDateTime.now());
        inv.setExpiredTime(dto.getContractTime().plusHours(1));
        invitationMapper.insertInvitation(inv);

        // 동적 서버 URL 생성
        String serverUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        
        log.info("초대링크 생성 - contractId: {}, 세션 ID: {}, 초대코드: {}", c.getContractId(), consultingSessionId, code);
        
        // 메일 발송
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

        Context ctx = new Context();
        ctx.setVariable("clientName", dto.getClientName());
        ctx.setVariable("reserveTime", dto.getContractTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        ctx.setVariable("code", code);
        ctx.setVariable("contractId", c.getContractId());
        ctx.setVariable("serverUrl", serverUrl);
        ctx.setVariable("consultingSessionId", consultingSessionId); // 세션 ID 추가

        String html = templateEngine.process("invitation/email", ctx);

        helper.setFrom(Email); // 보낼 이메일 넣어서 사용
        helper.setTo(dto.getEmail());
        helper.setSubject("상담 초대 코드 안내");
        helper.setText(html, true);

        mailSender.send(mime);

        log.info("초대링크 생성 완료 - contractId: {}, 세션 ID: {}, 초대코드: {}", c.getContractId(), consultingSessionId, code);
        log.info("상담원은 다음 링크로 입장: /contract/room?contractId={}&role=agent&session={}", c.getContractId(), consultingSessionId);

        // 초대코드와 세션 ID를 모두 반환
        Map<String, String> result = new HashMap<>();
        result.put("invitationCode", code);
        result.put("consultingSessionId", consultingSessionId);
        result.put("contractId", String.valueOf(c.getContractId()));
        
        return result;
    }

    /**
     * contractId 기반으로 항상 동일한 세션 ID 생성
     */
    private String generateSessionId(Long contractId) {
        return "session_" + contractId + "_fixed";
    }

    @Transactional(readOnly = true)
    public List<Schedule> getSchedules(Long agentId) {
        return contractMapper.selectSchedulesByAgent(agentId);
    }

    public boolean isScheduleExists(Long clientId, String contractTime) {
        LocalDateTime ct = LocalDateTime.parse(contractTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        int cnt = contractMapper.countByClientAndTime(clientId, ct);
        return cnt > 0;
    }

    public boolean isAgentScheduleExists(Long agentId, String contractTime) {
        LocalDateTime ct = LocalDateTime.parse(contractTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return contractMapper.countByAgentAndTime(agentId, ct) > 0;
    }

    @Transactional
    public void updateSchedule(Schedule dto) {
        LocalDateTime time = dto.getContractTime();
        Long cid = dto.getContractId();

        // 고객 일정 충돌 검사
        if (contractMapper.countByClientAndTimeExcept(dto.getClientId(), time, cid) > 0) {
            throw new IllegalArgumentException("이미 이 시간에 해당 고객 일정이 있습니다.");
        }
        // 상담사 일정 충돌 검사
        if (contractMapper.countByAgentAndTimeExcept(dto.getAgentId(), time, cid) > 0) {
            throw new IllegalArgumentException("이미 이 시간에 다른 상담 일정이 있습니다.");
        }

        contractMapper.updateSchedule(dto);
    }


    @Transactional(readOnly = true)
    public List<Schedule> getTodayContracts(Long agentId, String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("agentId", agentId);
        params.put("date", date);
        List<Schedule> schedules = contractMapper.findTodayContracts(params);
        
        // 각 스케줄에 대해 동일한 세션 ID 생성 로직 사용
        for (Schedule schedule : schedules) {
            String consultingSessionId = generateSessionId(schedule.getContractId());
            schedule.setSessionId(consultingSessionId);
        }
        
        return schedules;
    }


    public Agent findByEmailSelect(String email) {
        return agentMapper.findByEmailSelect(email);
    }

    public Map<String, Integer> getContractStatusCounts(Long agentId) {

        List<Map<String, Object>> counts = agentMapper.countContractByStatus(agentId);

        Map<String, Integer> result = new HashMap<>();

        result.put("PENDING", 0);
        result.put("IN_PROGRESS", 0);
        result.put("COMPLETED", 0);
        result.put("CANCELLED", 0);

        for (Map<String, Object> row : counts) {
            Object rawStatus = row.get("STATUS");
            Object countObj = row.get("COUNT");

            if (rawStatus == null || countObj == null) {
                continue;
            }

            String status = rawStatus.toString().trim().toUpperCase();
            int count = ((Number) countObj).intValue();


            result.put(status, count);

        }

        return result;
    }

    public Page<Contract> getContractsByStatusPaged(Long agentId, String status, String sort, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Contract> content = contractMapper.selectContractsByAgentIdAndStatusPaged(agentId, status, sort, offset, pageSize);
        int total = contractMapper.countContractsByAgentAndStatus(agentId, status);

        return new Page<>(content, total, page, pageSize);
    }


    public List<ContractTemplate> findByCompanyId(Long companyId) {
        return contractTemplateMapper.selectByCompanyId(companyId);
    }
}
