package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

import java.util.Date;

@Data
public class StampDTO {
    private Long stampId;        // stamp_id
    private String imagePath;     // image_path
    private Date createdAt;       // created_at
    private Long clientId;        // client_id
} 