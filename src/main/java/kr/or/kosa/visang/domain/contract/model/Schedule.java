package kr.or.kosa.visang.domain.contract.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Data;

@Data
public class Schedule {
	private Long contractId;
	private LocalDateTime createdAt;

	private String email;
	private Long clientId;
	private String clientName;
	private String phoneNumber;

	private String companyId;
	private Long agentId;
	private Long contractTemplateId;
	private LocalDateTime contractTime;
	private String memo;
	private String status;
	private String contractTemplateName;
	private String invitationCode;

	private String sessionId;

	private String time;

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}
