package kr.or.kosa.visang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@SpringBootApplication
@EnableEncryptableProperties
@MapperScan("kr.or.kosa.visang.domain")
public class VisangApplication {

	public static void main(String[] args) {
		SpringApplication.run(VisangApplication.class, args);
	}

}

