package com.example.samuraitravel.form;

import java.sql.Timestamp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewEditForm {
	
	private Integer house;
	
	private Integer user;
	
	private String name;
	
	@NotNull(message = "評価を入力してください。")
	private Integer rating;
	
	@NotBlank(message = "コメントを入力してください。")
	private String comment;
	
	private Timestamp createdAt;
	

}
