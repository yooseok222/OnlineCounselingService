package kr.or.kosa.visang.domain.pdf.enums;

public enum PDFTYPE {
    TEMPLATE_PDF("템플릿 계약서"),
    SIGNED_PDF("서명된 계약서");


    private final String description;

    PDFTYPE(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
