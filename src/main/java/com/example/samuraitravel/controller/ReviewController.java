package com.example.samuraitravel.controller;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReviewEditForm;
import com.example.samuraitravel.form.ReviewRegisterForm;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.HouseService;
import com.example.samuraitravel.service.ReviewService;

@Controller
// 特定の民宿に関するレビューを表示したり操作するためのもの
@RequestMapping("/houses/{houseId}/reviews")
public class ReviewController {
	// 民宿(House)の情報を取得するために使う
	private final HouseService houseService;
	// レビュー(Review)に関する操作をまとめて行うために使う
	private final ReviewService reviewService;
	
	// 民宿の情報と、それに関連するレビューを扱うために、2つのServiceを使う
	public ReviewController(HouseService houseService, ReviewService reviewService) {
		this.houseService = houseService;
		this.reviewService = reviewService;
	}

	// レビュー一覧を取得してビューに渡す処理を行う
	@GetMapping
	// どの民宿に対するレビュー一覧かはっきりしたいので@PathVariableを使う
	public String index(@PathVariable(name = "houseId")Integer houseId,
						// 作成日時で新しいレビューが先に10件ずつ表示される
						@PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable,RedirectAttributes redirectAttributes,
						Model model) {
		
		// HouseServiceから民宿情報を取得する。
		Optional<House> optionalHouse = houseService.findHouseById(houseId);
		
		// 民宿情報が存在しないならエラーを表示して民宿一覧に戻る
		if (optionalHouse.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage","指定された民宿は存在しません。");
			return "redirect:/houses";
		}
		
		// もし、optionalHouseに値が入っている場合はget()でその値を取り出す
		House house = optionalHouse.get();
		
		// 特定の民宿のレビューを作成日の新しい順に取得する
		Page<Review> reviewPage = reviewService.findReviewsByHouseOrderByCreatedAtDesc(house, pageable);
		
		// 民宿とレビューの情報をビュー(HTMLテンプレート)に渡す
		model.addAttribute("house", house);
		model.addAttribute("reviewPage", reviewPage);
			
		
		return "reviews/index";
			
	}
	
	// 新規レビュー投稿ページを表示する
	@GetMapping("/register")
	public String register(@PathVariable(name = "houseId")Integer houseId, RedirectAttributes redirectAttributes, Model model){
		
		// HouseServiceから民宿情報を取得する。
		Optional<House> optionalHouse = houseService.findHouseById(houseId);
		
		// 民宿情報が存在しないならエラーを表示して民宿一覧に戻る
		if (optionalHouse.isEmpty()) {		
			redirectAttributes.addFlashAttribute("errorMessage","指定された民宿は存在しません。");

			return "redirect:/houses";
		} 

		House house = optionalHouse.get();
		
		// レビュー登録フォームをビューに渡す
		model.addAttribute("reviewRegisterForm", new ReviewRegisterForm());
		// 民間情報をビューに渡す
		model.addAttribute("house", house);
		
		return "reviews/register";
	}
	
	// 新しいレビューを登録する
	@PostMapping("/create")
	public String createReview(@PathVariable(name = "houseId")Integer houseId, 
								@ModelAttribute @Validated ReviewRegisterForm reviewRegisterForm, BindingResult bindingResult, RedirectAttributes redirectAttributes,								
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,Model model){
		
		Optional<House> optionalHouse = houseService.findHouseById(houseId);
		if (optionalHouse.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage","指定された民宿が存在しません。");
			return "redirect:/houses"; 
		}
		
		House house = optionalHouse.get();
		
		// 入力エラーのチェック
		if(bindingResult.hasErrors()) {
				model.addAttribute("house",house );
				model.addAttribute("reviewRegisterForm", reviewRegisterForm);
				
				return "reviews/register";
		}
		
		// レビューを書いたユーザーが誰か(ログイン中のユーザー)の情報を取得
		User user = userDetailsImpl.getUser();	
		
		// ユーザーがすでにレビューしたかどうかチェックする
		if(reviewService.hasUserAlreadyReviewed(house,user)) {
			// 民宿と
			model.addAttribute("house", house);
			// 投稿フォーム情報と
			model.addAttribute("reviewRegisterForm", reviewRegisterForm);
			// エラーメッセージ
			model.addAttribute("errorMessage", "すでにこの民宿にレビューを投稿済みです");
			return "redirect:/houses/{houseId}";
		}
		
		
		// 新しいレビューを保存する
		// どの民宿に対するレビューなのか、誰が投稿したのか関連付ける
		reviewService.createReview(reviewRegisterForm, house, user);
		redirectAttributes.addFlashAttribute("successMessage", "レビューを投稿しました。");
		
			return "redirect:/houses/{houseId}";
		}
		
	
	// レビュー編集ページを表示する
	@GetMapping("/{reviewId}/edit")
	public String editReview(@PathVariable(name = "houseId") Integer houseId,
						@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
						@PathVariable(name = "reviewId")Integer reviewId, RedirectAttributes redirectAttributes, Model model){
		
		// ReviewServiceを使って指定されたレビューIDを取得する。(見つからない可能性もあるので、Optional で受け取る)
		Optional<Review>optionalReview = reviewService.findReviewById(reviewId);
		// HouseServiceを使ってしてされた民宿IDを取得する。
		Optional<House> optionalHouse  = houseService.findHouseById(houseId);
		
		// レビューも民宿見つからない場合はエラーメッセージを表示する
		if(!optionalReview.isPresent() || !optionalHouse.isPresent() ) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");
			return "redirect:/houses";
		}	
		// OptionalからReviewを取り出す
		Review review = optionalReview.get();
		
		// ユーザーが投稿者本人かチェック
		if(!review.getUser().getId().equals(userDetailsImpl.getUser().getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "編集できません。");
			return "redirect:/houses/{houseId}";
					
		}
		
		// 編集フォーム、評価, コメント
		ReviewEditForm reviewEditForm = new ReviewEditForm(review.getRating(), review.getComment());
		
		model.addAttribute("reviewEditForm", reviewEditForm);
		model.addAttribute("review", review);
		model.addAttribute("house", review.getHouse());
		
		return "reviews/edit";
	}
	
	// レビューを更新する
	@PostMapping("/{reviewId}/update")
	public String updateReview(@PathVariable(name = "houseId") Integer houseId,
								@PathVariable(name = "reviewId") Integer reviewId,
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, 
								@ModelAttribute @Validated ReviewEditForm reviewEditForm, BindingResult bindingResult, RedirectAttributes redirectAttributes,Model model){
		
		// ReviewServiceを使って指定されたレビューIDを取得する。(見つからない可能性もあるので、Optional で受け取る)
		Optional<Review>optionalReview = reviewService.findReviewById(reviewId);
		// HouseServiceを使ってしてされた民宿IDを取得する。
		Optional<House> optionalHouse  = houseService.findHouseById(houseId);
		
		// レビューも民宿見つからない場合はエラーメッセージを表示する
		if(!optionalReview.isPresent() || !optionalHouse.isPresent() ) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");
			return "redirect:/houses";
		}	
			
		Review review = optionalReview.get();
		House house = optionalHouse.get();

		// ログイン中のユーザーがレビュー投稿者かどうか確認する
		if(!review.getUser().getId().equals(userDetailsImpl.getUser().getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "更新できません");
			return "redirect:/houses/{houseId}";
					
		}
		
		// 編集フォームに入力ミスがあったら
		if (bindingResult.hasErrors()) {	
			// この編集したい民宿の情報と
			model.addAttribute("house", house);
			// 編集したいレビュー情報と
			model.addAttribute("review", review);
			// 入力フォームのデータをビューに渡して
			model.addAttribute("reviewEditForm", reviewEditForm);
			// レビュー編集フォームに戻る
			return "reviews/edit";
		}
		
		// レビュー情報の編集を行う(フォームで編集した新しいレビュー内容と編集前の既存のレビューオブジェクト)
		reviewService.updateReview(reviewEditForm, review);
		// 編集成功メッセージを表示する
		redirectAttributes.addFlashAttribute("successMessage", "レビュー情報を更新しました。");
		// 民宿詳細ページにリダイレクト
		return "redirect:/houses/{houseId}";
	}
	
	// レビューを削除する
	@PostMapping("/{reviewId}/delete")
	public String deleteReview(@PathVariable(name = "houseId") Integer houseId,
								@PathVariable(name = "reviewId")Integer reviewId, 
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, RedirectAttributes redirectAttributes) {

		// ReviewServiceを使って指定されたレビューIDを取得する。(見つからない可能性もあるので、Optional で受け取る)
		Optional<Review>optionalReview = reviewService.findReviewById(reviewId);
		// HouseServiceを使ってしてされた民宿IDを取得する。
		Optional<House> optionalHouse  = houseService.findHouseById(houseId);
		
		// レビューも民宿見つからない場合はエラーメッセージを表示する
		if(!optionalReview.isPresent() || !optionalHouse.isPresent() ) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");
			return "redirect:/houses";
		}	
		
		Review review = optionalReview.get();

		// ログイン中のユーザーがレビュー投稿者かどうか確認する
		if(!review.getUser().getId().equals(userDetailsImpl.getUser().getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "アクセスできません");
			return "redirect:/houses/{houseId}";
					
		}
		
		// レビューを削除する
		reviewService.deleteReview(review);	
		redirectAttributes.addFlashAttribute("successMessage", "レビューを削除しました。");
		// 民宿詳細ページにリダイレクト
		return "redirect:/houses/{houseId}";		
		
	}
}

