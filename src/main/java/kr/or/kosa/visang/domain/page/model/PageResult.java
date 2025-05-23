package kr.or.kosa.visang.domain.page.model;

import lombok.Data;

import java.util.List;

// PageResult.java
@Data
public class PageResult<T> {
    private List<T> content;
    private int totalCount;
    private int page;
    private int pageSize;
    private int totalPages;
    public int getTotalPages() {
        return (totalCount + pageSize - 1) / pageSize;
    }
    // getters/setters
}