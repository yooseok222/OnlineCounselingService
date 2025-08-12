package kr.or.kosa.visang.domain.company.repository;

import org.apache.ibatis.annotations.Mapper;

/**
 * 초대코드 Mapper - Redis 사용으로 DB 처리 불필요
 * 더 이상 사용하지 않으나 의존성 문제로 유지
 * 이름 충돌 문제로 Legacy 접두사 추가
 */
@Mapper
public interface LegacyInvitationMapper {
    // Redis로 대체되어 DB 작업 메서드 제거됨
} 