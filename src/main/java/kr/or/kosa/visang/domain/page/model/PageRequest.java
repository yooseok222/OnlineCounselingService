package kr.or.kosa.visang.domain.page.model;

import lombok.Data;

@Data
public class PageRequest {
    private int page;
    private int pageSize;
    public PageRequest(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }
    public int getOffset() {
        return (page - 1) * pageSize;
    }
    // getters
}
