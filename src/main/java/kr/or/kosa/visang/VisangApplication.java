package kr.or.kosa.visang;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableEncryptableProperties
@MapperScan("kr.or.kosa.visang.domain")
public class VisangApplication {

	public static void main(String[] args) {
		SpringApplication.run(VisangApplication.class, args);
	}

}