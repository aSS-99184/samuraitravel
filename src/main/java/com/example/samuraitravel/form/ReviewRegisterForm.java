package com.example.samuraitravel.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRegisterForm {
	
	private Integer house;
	
	private Integer user;
	
	private String name;
	
	@NotNull(message = "評価を入力してください。")
	private Integer rating;
	
	@NotBlank(message = "コメントを入力してください。")
	private String comment;
}
