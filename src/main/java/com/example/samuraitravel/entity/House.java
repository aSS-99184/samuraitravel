package com.example.samuraitravel.entity;

import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;


@Entity
@Table(name = "houses")
@Data
@ToString(exclude = {"reservations", "reviews"})
public class House {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "image_name")
	private String imageName;
	
	@Column(name = "description")
	private String description;
	
	@Column(name = "price")
	private Integer price;
	
	@Column(name = "capacity")
	private Integer capacity;
	
	@Column(name = "postal_code")
	private String postalCode;
	
	@Column(name = "address")
	private String address;
	
	@Column(name = "phone_number")
	private String phoneNumber;
	
	@Column(name = "created_at",insertable = false, updatable = false)
	private Timestamp createdAt;
	
	@Column(name = "updated_at", insertable = false, updatable = false)
	private Timestamp updatedAt;
	
	// 1つのHouseに対して複数のReservation。民宿を取り出したときに紐づく予約もすぐに一緒に取り出す
	// 民宿が削除されたらその民宿も一緒に削除される
	@OneToMany(mappedBy = "house", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
	private List<Reservation> reservations;
	// 1つのHouseに対して複数のReview。民宿を取り出したときにひもづくレビューもすぐに一緒に取り出す
	// 民宿が削除されたらそのレビューも一緒に削除される
	@OneToMany(mappedBy = "house", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
	private List<Review> reviews;
	
	@OneToMany(mappedBy = "house", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
	private List<Favorite> favorites; 
}
