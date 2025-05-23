package kr.or.kosa.visang.domain.invitation.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Invitation {
	private Long invitationId;
	private String invitationCode;
	private LocalDateTime createdAt;
	private LocalDateTime expiredTime;
	private Long contractId;
}