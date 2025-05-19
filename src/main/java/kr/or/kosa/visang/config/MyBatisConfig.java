package kr.or.kosa.visang.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {"kr.or.kosa.visang.domain.contract.repository", "kr.or.kosa.visang.domain.agent.repository"})
public class MyBatisConfig {
    // MyBatis 추가 설정이 필요하면 여기에 추가
} 