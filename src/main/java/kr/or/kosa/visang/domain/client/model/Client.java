package kr.or.kosa.visang.domain.client.model;

import java.sql.Date;

import lombok.Data;

@Data
public class Client {
	private Long clientId;
	private String ssn;
	private String name;
	private String email;
	private String password;
	private String phoneNumber;
	private String address;
	private String role;
	private Date createAt;
}
