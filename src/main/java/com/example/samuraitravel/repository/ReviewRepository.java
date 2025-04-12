package com.example.samuraitravel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
	// 特定の民宿に対する最新のレビュー6件だけを取得し、リスト形式で出力する。
	// ページングが不要で6件固定のため戻り値にList<Review>を使う
	public List<Review> findTop6ByHouseOrderByCreatedAtDesc(House house);
	
	// 特定の民宿とユーザーに紐づくレビューを取得し、該当エンティティを出力する。
	public Optional<Review> findByHouseAndUser(House house, User user);
	
	// 特定の民宿に対するレビュー総数をカウントし、その結果を出力する。
	// 大量の件数に対応するためlong型にする(Spring Data JPAの仕様)
	public long countByHouse(House house);
	
	// 特定の民宿に関するレビューを作成日時の降順で取得し、ページネーション形式で出力する。
	public Page<Review> findByHouseOrderByCreatedAtDesc(House house, Pageable pageable);

}