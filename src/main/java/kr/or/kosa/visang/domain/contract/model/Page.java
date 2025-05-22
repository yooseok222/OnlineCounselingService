package kr.or.kosa.visang.domain.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Page<T> {
    private List<T> content;
    private int total;
    private int currentPage;
    private int paseSize;

    public int getTotalPages() {
        return (int) Math.ceil((double) total / paseSize);
    }
}
