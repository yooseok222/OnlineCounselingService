package kr.or.kosa.visang.domain.pdf.service;


import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import kr.or.kosa.visang.common.config.key.KeystoreLoader;
import kr.or.kosa.visang.domain.contract.model.ContractSingedDTO;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
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
            String signedPdfPath,
            ContractSingedDTO contractSingedDTO
    ) throws Exception {
        System.out.println("🔐 등록된 프로바이더 목록:");
        for (Provider provider : Security.getProviders()) {
            System.out.println(" - " + provider.getName());
        }


        // 1. Keystore에서 키/인증서 가져오기
        PrivateKey privateKey = KeystoreLoader.loadPrivateKey(keystorePath, keystorePassword, alias);
        Certificate[] certChain = KeystoreLoader.loadCertificateChain(keystorePath, keystorePassword, alias);

        //2. PDF 열기 + 출력 경로 설정
        Path uploadRoot = Paths.get(signedPdfPath);
        Path outputPath = uploadRoot.resolve(outputPdfPath);


        // 부모 디렉토리 얻기 (항상 uploadRoot가 있으므로 null 아님)
        Path parent = outputPath.getParent();

        // 디렉토리가 존재하지 않으면 생성
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);  // 디렉토리 생성 (없을 경우)
        }

        // 1. InputStream을 복제 (바이트 배열로)
        byte[] pdfBytes = inputStream.readAllBytes();

        // 2. PDF 열고 signer 준비

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)));
        int totalPages = pdfDoc.getNumberOfPages();
        pdfDoc.close();  // 닫아줘야 PdfSigner가 정상 작동함

        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        PdfSigner signer = new PdfSigner(reader, new FileOutputStream(outputPath.toFile()), new StampingProperties());

        // ✅ 3. 서명 외형 설정
        Rectangle rect = new Rectangle(300, 50, 250, 80); // PDF 좌하단에 위치

        // 서명 폰트설정
        PdfFont font = PdfFontFactory.createFont("fonts/malgun.ttf", PdfEncodings.IDENTITY_H);

        // 이미지 리소스 불러오기
        InputStream is = new ClassPathResource("static/images/stamp/visang_stamp.png").getInputStream();
        ImageData imageData = ImageDataFactory.create(StreamUtil.inputStreamToArray(is));


        PdfSignatureAppearance appearance = signer.getSignatureAppearance()
                .setReason("계약서 전자서명")
                .setLocation("서울")
                .setPageRect(rect)
                .setPageNumber(totalPages)
                .setLayer2Font(font) // ★ 폰트 적용
                .setLayer2FontSize(12f) // 글씨 크기 조정
                .setLayer2Text("전자서명자: VISANG" +
                                "\n 상담원 : "+ contractSingedDTO.getAgentName()+
                                "\n 계약자 : " + contractSingedDTO.getClientName() +
                                "\n 서명일 : " + java.time.LocalDate.now() +
                                "\n 문서 무결성 검증됨")
                .setSignatureGraphic(imageData) // ★ 도장 이미지
                .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION); // 텍스트로 서명 표시

        signer.setFieldName("Signature1");

        // 3. 서명 알고리즘 및 설정
        IExternalSignature signature = new PrivateKeySignature(privateKey, "SHA256", "BC");
        IExternalDigest digest = new BouncyCastleDigest();

        // 4. 실제 서명 수행
        signer.signDetached(digest, signature, certChain, null, null, null, 0, PdfSigner.CryptoStandard.CADES);
    }
}
