package com.example.samuraitravel.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.samuraitravel.entity.Favorite;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.repository.FavoriteRepository;

@Service
public class FavoriteService {
	private final FavoriteRepository favoriteRepository;
	
	public FavoriteService (FavoriteRepository favoriteRepository) {
		this.favoriteRepository = favoriteRepository;
	}
	
	// お気に入りをデータベースに登録するための処理
	@Transactional
	public void createFavorite(House house, User user) {
		Favorite favorite = new Favorite();
		
		favorite.setHouse(house);
		favorite.setUser(user);
		// データベース(favoriteテーブル)にレビューを保存
		favoriteRepository.save(favorite);
	}
	
	// 指定したIDを持つお気に入りを取得する。(Optionalを使うのはidが存在しない可能性もあるため)
	public Optional<Favorite>findFavoriteById(Integer id) {
		return favoriteRepository.findById(id);
	}
	
	// 指定した民宿とユーザーのお気に入りを取得する。(Optionalを使わないのは特定のユーザーと特定の民宿という組み合わせで一つはお気に入りがある前提だから) 
	public Favorite findFavoriteByHouseAndUser(House house, User user){
		return favoriteRepository.findByHouseAndUser(house,user);
		
	}
	
	// 指定したユーザーの全お気に入りを作成日時が新しい順に並べ替え、ページングされた状態で取得する。
	// ページング処理のため戻り値をPageにし、引数にpageableを追加する。
	public Page<Favorite> findFavoritesByUserOrderByCreatedAtDesc(User user, Pageable pageable){
		return favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);
	}
	
	// 指定したお気に入りを削除する。
	public void deleteFavorite(Favorite favorite){
		favoriteRepository.delete(favorite);
	}
	
	// 指定したユーザーが、指定した民宿をすでにお気に入りに追加済みかどうかをチェックする。
	// HouseとUserの組み合わせに対応するFavoriteをDBで探し、あればFavoriteオブジェクト(true)を返す。なければfalseを返す。
	public boolean isFavorite(House house,User user){
		return favoriteRepository.findByHouseAndUser(house,user) !=null;
	}

}
