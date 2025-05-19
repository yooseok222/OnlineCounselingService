package kr.or.kosa.visang.domain.Client.repository;

import org.apache.ibatis.annotations.Mapper;

import kr.or.kosa.visang.domain.Client.model.Client;

@Mapper
public interface ClientMapper {

	Client findByEmail(String email);
}