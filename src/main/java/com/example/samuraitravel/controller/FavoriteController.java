package com.example.samuraitravel.controller;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.Favorite;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.FavoriteService;

@Controller
public class FavoriteController {
	// 民宿(House)の情報を取得するために使う
	private final HouseRepository houseRepository;
	// お気に入り(Favorite)に関する操作のために使う
	private final FavoriteService favoriteService;
	
	public FavoriteController(HouseRepository houseRepository, FavoriteService favoriteService){
		this.houseRepository = houseRepository;
		this.favoriteService = favoriteService;
	}
	
	// 指定されたユーザーのお気に入りをページングで取得し、お気に入り一覧ページへ受け渡して表示する。
	@GetMapping("/favorites")
	public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
						@PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable,RedirectAttributes redirectAttributes,
						Model model)
	{
	
	// 現在ログイン中のユーザー情報をUserエンティティとして取得する
	User user = userDetailsImpl.getUser();
	// ユーザーのお気に入り情報を作成日順に取得。pageableを使ってページネーションで表す
	Page<Favorite> favoritePage = favoriteService.findFavoritesByUserOrderByCreatedAtDesc(user, pageable);
	
	// お気に入りが見つからなかった場合
	if(favoritePage.isEmpty()) {
		redirectAttributes.addFlashAttribute("errorMessage", "お気に入りが見つかりません。");
		// お気に入り一覧ページにリダイレクトする
		return "redirect:/favorites";
	}
	
	// ビュー側で favorite.house.name が参照できるようにする(getName()を呼ぶことで、民宿（House）の情報がDBから読み込まれる)
	favoritePage.getContent().forEach(favorite -> {
		favorite.getHouse().getName();
	
	});
	
	// favoritePageという名前でお気に入り情報をビューに渡す	
	model.addAttribute("favoritePage", favoritePage);
	return"favorites/index";
	}
	
	// 新しいお気に入りを登録する
	@PostMapping("/houses/{houseId}/favorites/create")
	public String createFavorite(@PathVariable (name = "houseId")Integer houseId, 
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
								RedirectAttributes redirectAttributes,
								Model model) 
	{
		// ログインしているユーザーを取得する
		User user = userDetailsImpl.getUser();
		
		// 民間情報を取得する
		House house = houseRepository.findById(houseId).orElse(null);
		
		// 民宿がみつからないとき
		if(house == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定された民宿は存在しません。");
			return "redirect:/houses";
		}
		
		// すでにお気に入り登録されているかチェックする(同じユーザーが同じ民宿に何回もお気に入り登録できない仕様)
		if (favoriteService.isFavorite(house, user)) {
			redirectAttributes.addFlashAttribute("errorMessage", "この民宿はすでにお気に入りに登録されています。");
			return "redirect:/houses/{houseId}";
		}
		
		// favoriteServiceを使って保存(お気に入り登録)
		favoriteService.createFavorite(house,user);
		
		// 登録が成功したら民宿詳細へリダイレクト
		redirectAttributes.addFlashAttribute("successMessage", "お気に入りに追加しました。");
		return "redirect:/houses/{houseId}";
	}
	
	@PostMapping("/houses/{houseId}/favorites/{favoriteId}/delete")
	public String deleteFavorite(@PathVariable(name = "houseId") Integer houseId,
								@PathVariable(name = "favoriteId") Integer favoriteId,
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, RedirectAttributes redirectAttributes) {
		// ログインしているユーザーを取得する
		User user = userDetailsImpl.getUser();
		// お気に入り情報をOptionalで取得する
		Optional<Favorite> optionalFavorite = favoriteService.findFavoriteById(favoriteId);
		
		
		
		// お気に入りデータがそもそも存在しない、またはお気に入り登録した人がログイン中のユーザーではない場合、削除させないようにする
		if(optionalFavorite.isEmpty() || !optionalFavorite.get().getUser().equals(user)) {
			redirectAttributes.addFlashAttribute("errorMessage","お気に入り登録はできません。");
			return "redirect:/houses/{houseId}";
		}
		
		Favorite favorite = optionalFavorite.get();
		// お気に入りを削除する
		favoriteService.deleteFavorite(favorite);
		redirectAttributes.addFlashAttribute("successMessage", "お気に入りを解除しました。");
		return "redirect:/houses/{houseId}";
	}
}
