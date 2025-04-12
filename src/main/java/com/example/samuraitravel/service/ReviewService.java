package com.example.samuraitravel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReviewEditForm;
import com.example.samuraitravel.form.ReviewRegisterForm;
import com.example.samuraitravel.repository.ReviewRepository;

@Service
public class ReviewService {
	private final ReviewRepository reviewRepository;
	
	
	public ReviewService(ReviewRepository reviewRepository) {
		this.reviewRepository = reviewRepository;
	}
	
	// レビューをデータベースに登録するための処理(フォームのデータからレビューを作って返す処理)
	@Transactional 
	public void createReview(ReviewRegisterForm reviewRegisterForm, House house, User user) {
		Review review= new Review();
		
		review.setHouse(house);
		review.setUser(user);
		review.setRating(reviewRegisterForm.getRating());
		review.setComment(reviewRegisterForm.getComment());
		// データベース(reviewsテーブル)にレビューを保存
		reviewRepository.save(review);
	}
	
	// 指定したIDを持つレビューを取得する。
	// Reviewが存在しない場合に対応するため、Optionalを使って値があるかどうか安全に扱えるようにする。
	public Optional<Review> findReviewById(Integer id){
		return reviewRepository.findById(id);
	}
	
	// 指定した民宿のレビューを作成日時が新しい順に6件取得する。
	public List<Review> findTop6ReviewsByHouseOrderByCreatedAtDesc(House house) {
		// Spring Data JPAの命名規則に沿ったメソッド名を使用している。
		return reviewRepository.findTop6ByHouseOrderByCreatedAtDesc(house);	
	}
	
	// 指定した民宿とユーザーのレビューを取得する。
	public Optional<Review> findReviewByHouseAndUser(House house, User user){
		return reviewRepository.findByHouseAndUser(house, user);
	}
	
	// 指定した民宿のレビュー件数を取得する。
	// 大量の件数に対応する可能性もあるためlong型にする(Spring Data JPAの仕様)
	public long countReviewsByHouse(House house) {
		return reviewRepository.countByHouse(house);
	}
	
	// 指定した民宿のすべてのレビューを作成日時が新しい順に並べ替え、ページングされた状態で取得する。
	// ページング処理のため戻り値をPageにし、引数にpageableを追加する。
	public Page<Review> findReviewsByHouseOrderByCreatedAtDesc(House house, Pageable pageable) {
		return reviewRepository.findByHouseOrderByCreatedAtDesc(house,pageable);
	}

	// レビュー編集ページ用のフォームクラスからのデータをもとに、既存のレビューを更新する。
	@Transactional
	public void updateReview(ReviewEditForm reviewEditForm, Review review) {
		// ユーザーが変更できる項目
		review.setRating(reviewEditForm.getRating());
		review.setComment(reviewEditForm.getComment());
		
		reviewRepository.save(review);
		}
	
	
	// 指定したレビューを削除する。
	// 将来拡張する可能性を考えてレビューの中身をもっておくためにReviewを引数にする。
	@Transactional
	public void deleteReview(Review review) {
		reviewRepository.delete(review);
	}
	
	// 指定したユーザーが、指定した民宿のレビューをすでに投稿済みかどうかをチェックする。
	public boolean hasUserAlreadyReviewed(House house, User user) {
		Optional<Review> existingReview = reviewRepository.findByHouseAndUser(house, user);
		return existingReview.isPresent();

	}
}
