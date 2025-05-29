package kr.or.kosa.visang.common.config.key;

import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class KeystoreLoader {
    private static InputStream openKeystoreStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            // classpath 리소스로 로드
            String location = path.substring("classpath:".length());
            return new ClassPathResource(location).getInputStream();
        } else {
            // 파일 시스템 경로로 로드
            return new FileInputStream(path);
        }
    }

    public static PrivateKey loadPrivateKey(String path, String password, String alias) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream in = openKeystoreStream(path)) {
            keystore.load(in, password.toCharArray());
        }
        return (PrivateKey) keystore.getKey(alias, password.toCharArray());
    }

    public static Certificate[] loadCertificateChain(String path, String password, String alias) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream in = openKeystoreStream(path)) {
            keystore.load(in, password.toCharArray());
        }
        return keystore.getCertificateChain(alias);
    }
}
