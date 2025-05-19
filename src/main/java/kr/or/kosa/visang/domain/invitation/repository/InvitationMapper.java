package kr.or.kosa.visang.domain.invitation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.invitation.model.Invitation;

@Mapper
public interface InvitationMapper {

	void insertInvitation(Invitation inv);

	void deleteByContractId(@Param("contractId") Long contractId);

	int updateEmailSent(@Param("invitationId") Long invitationId, @Param("emailSent") String emailSent);

	Invitation findByCode(@Param("code") String code);
}
