package kr.or.kosa.visang.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
    "kr.or.kosa.visang.domain.contract.repository", 
    "kr.or.kosa.visang.domain.agent.repository",
    "kr.or.kosa.visang.domain.user.repository",
    "kr.or.kosa.visang.domain.admin.repository",
    "kr.or.kosa.visang.domain.client.repository",
    "kr.or.kosa.visang.domain.invitation.repository",
    "kr.or.kosa.visang.domain.company.repository",
    "kr.or.kosa.visang.domain.contractTemplate.repository",
    "kr.or.kosa.visang.domain.chat.repository"
})
public class MyBatisConfig {
    // MyBatis 추가 설정이 필요하면 여기에 추가
} 