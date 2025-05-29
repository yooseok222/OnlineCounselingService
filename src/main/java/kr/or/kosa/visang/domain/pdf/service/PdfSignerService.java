package kr.or.kosa.visang.domain.pdf.service;


import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import kr.or.kosa.visang.common.config.key.KeystoreLoader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;

public class PdfSignerService {

    static {
        // BouncyCastle 프로바이더 등록
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static void signPdf(
            InputStream inputStream,
            String outputPdfPath,
            String keystorePath,
            String keystorePassword,
            String alias,
            String signedPdfPath
    ) throws Exception {
        System.out.println("🔐 등록된 프로바이더 목록:");
        for (Provider provider : Security.getProviders()) {
            System.out.println(" - " + provider.getName());
        }


        // 1. Keystore에서 키/인증서 가져오기
        PrivateKey privateKey = KeystoreLoader.loadPrivateKey(keystorePath, keystorePassword, alias);
        Certificate[] certChain = KeystoreLoader.loadCertificateChain(keystorePath, keystorePassword, alias);

        Path uploadRoot = Paths.get(signedPdfPath);
        Path outputPath = uploadRoot.resolve(outputPdfPath);


        // 2. PDF 열고 signer 준비
        PdfReader reader = new PdfReader(inputStream);

        // 부모 디렉토리 얻기 (항상 uploadRoot가 있으므로 null 아님)
        Path parent = outputPath.getParent();

        // 디렉토리가 존재하지 않으면 생성
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);  // 디렉토리 생성 (없을 경우)
        }

        PdfSigner signer = new PdfSigner(reader, new FileOutputStream(outputPath.toFile()), new StampingProperties());

        // 3. 서명 알고리즘 및 설정
        IExternalSignature signature = new PrivateKeySignature(privateKey, "SHA256", "BC");
        IExternalDigest digest = new BouncyCastleDigest();

        // 4. 실제 서명 수행
        signer.signDetached(digest, signature, certChain, null, null, null, 0, PdfSigner.CryptoStandard.CADES);
    }
}
