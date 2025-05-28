package kr.or.kosa.visang.domain.contract.model;

import lombok.Data;

import java.util.Date;

@Data
public class StampDTO {
    private Long stampId;           // stamp_id
    private String stampImageUrl;   // stamp_image_url
    private Date createdAt;         // created_at
    private Long clientId;          // client_id
    private Date lastUsedAt;        // last_used_at
} 