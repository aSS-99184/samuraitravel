package com.example.samuraitravel.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.Favorite;
import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReservationInputForm;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.repository.ReviewRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.FavoriteService;
import com.example.samuraitravel.service.HouseService;
import com.example.samuraitravel.service.ReviewService;

@Controller
@RequestMapping("/houses")
public class HouseController {
	private final HouseService houseService;
	private final HouseRepository houseRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewService reviewService;
	private final FavoriteService favoriteService;
	
	public HouseController(HouseService houseService, HouseRepository houseRepository, ReviewRepository reviewRepository, ReviewService reviewService,  FavoriteService favoriteService) {
		this.houseService = houseService;
		this.houseRepository = houseRepository;
		this.reviewRepository = reviewRepository;
		this.reviewService = reviewService;
		this.favoriteService = favoriteService;
	}
	
	@GetMapping
	public String index(@RequestParam(name = "keyword", required = false)String keyword,
						@RequestParam(name = "area", required = false)String area,
						@RequestParam(name = "price", required = false)Integer price,
						@RequestParam(name = "order", required = false)String order,
						@PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable,
						Model model) 
	{
		Page<House> housePage;
		
		if (keyword != null && !keyword.isEmpty()) {
			if(order != null && order.equals("priceAsc")) {
				housePage = houseRepository.findByNameLikeOrAddressLikeOrderByPriceAsc("%" + keyword + "%", "%" + keyword + "%", pageable);			
			} else {
				housePage = houseRepository.findByNameLikeOrAddressLikeOrderByCreatedAtDesc("%" + keyword + "%", "%" + keyword + "%", pageable);
			}
		} else if (area != null && !area.isEmpty()) {
			if (order != null && order.equals("priceAsc")) {
				housePage = houseRepository.findByAddressLikeOrderByPriceAsc("%" + area + "%", pageable);
			} else {
				housePage = houseRepository.findByAddressLikeOrderByCreatedAtDesc("%" + area + "%", pageable);
			}
		} else if(price != null) {
			if (order != null && order.equals("priceAsc")) {
				housePage = houseRepository.findByPriceLessThanEqualOrderByPriceAsc(price, pageable);
			} else {
				housePage = houseRepository.findByPriceLessThanEqualOrderByCreatedAtDesc(price, pageable);
			}
		} else {
			if (order != null && order.equals("priceAsc")) {
				housePage = houseRepository.findAllByOrderByPriceAsc(pageable);
			} else {
				housePage = houseRepository.findAllByOrderByCreatedAtDesc(pageable);
			}
		}
		
		model.addAttribute("housePage",housePage);
		model.addAttribute("keyword", keyword);
		model.addAttribute("area",area);
		model.addAttribute("price",price);
		model.addAttribute("order", order);
		
		return "houses/index";
	}
	
	// 民宿(House)の詳細ページにレビューの情報を渡す処理をする。
	@GetMapping("/{houseId}")
	public String show(@PathVariable(name = "houseId") Integer houseId, 
						@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, 
						RedirectAttributes redirectAttributes,  
						Model model) {
		// IDで民宿を検索、必要な情報を取り出す(Houseを定義する) 
		Optional<House> optionalHouse  = houseService.findHouseById(houseId);
		
		// 民宿情報がもしないときの処理
		if (optionalHouse.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "指定された民宿が見つかりません。");
			return "redirect:/houses";
		}
		
		// houseを使えるようにする
		House house = optionalHouse.get();
		
		// 取得した民宿情報(house)をビューに渡すためにModelに追加する
		model.addAttribute("house", house);
		
		boolean hasUserAlreadyReviewed = false; 
		Favorite favorite = null;
		boolean isFavorite = false;
		
		// ユーザーが存在する場合
		if (userDetailsImpl != null) {
			User user = userDetailsImpl.getUser();
			// そのユーザーのレビューがあるかどうかチェックする
			hasUserAlreadyReviewed = reviewService.hasUserAlreadyReviewed(house, user);
			// ユーザーがお気に入り追加しているかどうか
			favorite = favoriteService.findFavoriteByHouseAndUser(house, user);
			if (favorite != null) {
				isFavorite = true;				
			} else {
				isFavorite = false;
			}
		} else {
			// ユーザーではない場合エラーを表示してログインページにリダイレクト
			redirectAttributes.addFlashAttribute("error", "ログインが必要です。");
			return "redirect:/login"; 
		}

		model.addAttribute("hasUserAlreadyReviewed", hasUserAlreadyReviewed);
		model.addAttribute("favorite", favorite);
		model.addAttribute("isFavorite", isFavorite);
		
		// 特定の民宿に対する最新のレビュー6件だけを取得し、リスト形式で出力する。
		// ページングが不要で6件固定のため戻り値にList<Review>を使う
		List<Review> newReviews = reviewRepository.findTop6ByHouseOrderByCreatedAtDesc(house); 
		model.addAttribute("newReviews", newReviews); 
		
		
		// 指定したレビュー件数の取得する
		// // 大量の件数に対応するためlong型にする(Spring Data JPAの仕様)
		long totalReviewCount = reviewRepository.countByHouse(house);
		model.addAttribute("totalReviewCount", totalReviewCount);  
		
		// ユーザーが予約するときに情報をいれるフォーム
		model.addAttribute("reservationInputForm", new ReservationInputForm());
		
		return "houses/show";
	}
}
