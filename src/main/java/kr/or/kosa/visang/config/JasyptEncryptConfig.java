//package kr.or.kosa.visang.config;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.jasypt.encryption.StringEncryptor;
//
//@Configuration
//public class JasyptEncryptConfig {
//
//    // 애플리케이션 시작 시 한 번 실행해서 암호문 생성
//    @Bean
//    public CommandLineRunner encryptRunner(StringEncryptor encryptor) {
//        return args -> {
//            String Email = "";
//            String pass = "";
//            String cipher = encryptor.encrypt(Email);
//            String cipher2 = encryptor.encrypt(pass);
//            System.out.println("Encrypted ▶ " + cipher);
//            // 필요하면 decrypt 도 확인
//            System.out.println("Decrypted ▶ " + encryptor.decrypt(cipher));
//            System.out.println("Encrypted ▶ " + cipher2);
//            System.out.println("Decrypted ▶ " + encryptor.decrypt(cipher2));
//        };
//    }
//}
