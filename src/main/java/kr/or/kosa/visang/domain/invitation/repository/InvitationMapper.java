package kr.or.kosa.visang.domain.invitation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.kosa.visang.domain.invitation.model.Invitation;

@Mapper
public interface InvitationMapper {

	void insertInvitation(Invitation inv);

	void deleteByContractId(@Param("contractId") Long contractId);

	Invitation findByCode(@Param("code") String code);
}
