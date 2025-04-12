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
		
		House house = optionalHouse.get();
		
		// ある民宿のレビューを新しい順に取得する
		Page<Review> reviewPage = reviewService.findReviewsByHouseOrderByCreatedAtDesc(house, pageable);
		
		// 民宿とレビューの情報をビュー(HTMLテンプレート)に渡す
		model.addAttribute("house", house);
		model.addAttribute("reviewPage", reviewPage);
			
		
		return "reviews/index";
			
	}
	
	// レビュー投稿ページを表示する
	@GetMapping("/register")
	public String register(@PathVariable(name = "houseId")Integer houseId, RedirectAttributes redirectAttributes, Model model)
	{
		Optional<House> optionalHouse = houseService.findHouseById(houseId);
		
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
								@ModelAttribute @Validated ReviewRegisterForm reviewResisterForm, BindingResult bindingResult, RedirectAttributes redirectAttributes,								
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,Model model){
		
		// 入力エラーのチェック
		if(bindingResult.hasErrors()) {
			Optional<House> optionalHouse = houseService.findHouseById(houseId);
			if(optionalHouse.isPresent()) {
				model.addAttribute("house", optionalHouse.get());
			}
			return "reviews/register";
		}
		
		Optional<House> optionalHouse = houseService.findHouseById(houseId);
		if (optionalHouse.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage","指定された民宿は存在しません。");
			return "redirect:/houses"; 
		}
		House house = optionalHouse.get();
		// 現在ログイン中のユーザーの情報を取得
		User user = userDetailsImpl.getUser();
		
		// すでに同じユーザーが同じ民宿を投稿しているかチェック
		Optional<Review> existingReview = reviewService.findReviewByHouseAndUser(house, user);
		if (existingReview.isPresent()) {
			redirectAttributes.addFlashAttribute("errorMessage", "すでにこの民宿にレビューを投稿しています。");
			return "redirect:/houses/{houseId}";
		}
		
		// どの民宿に対するレビューなのか、誰が投稿したのか関連付ける
		reviewService.createReview(reviewResisterForm, house, user);
		redirectAttributes.addFlashAttribute("successMessage", "レビューを投稿しました。");
		
		return "redirect:/houses/{houseId}";
	}
	
	// レビュー編集ページを表示する
	@GetMapping("/{reviewId}/edit")
	public String edit(@PathVariable(name = "houseId") Integer houseId,
						@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
						@PathVariable(name = "reviewId")Integer reviewId, RedirectAttributes redirectAttributes, Model model){
		
		// ReviewServiceを使って指定されたレビューIDを取得する。見つからない可能性もあるので、Optional で受け取る
		Optional<Review>optionalReview = reviewService.findReviewById(reviewId);
		
		// レビューが見つからない場合
		if(!optionalReview.isPresent()) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");
			return "redirect:/houses";
		}	
		// OptionalからReviewを取り出す
		Review review = optionalReview.get();
		
		// ログイン中のユーザーとレビューのユーザーが一致するかチェック(ユーザーが自分のレビューしか更新できないようにする)
		if(!review.getUser().getId().equals(userDetailsImpl.getUser().getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "編集できません。");
			return "redirect:/houses/{houseId}";
					
		}
		
		// 民宿ID, ユーザーID, 名前, 評価, コメント, 作成日
		ReviewEditForm reviewEditForm = new ReviewEditForm(review.getHouse().getId(), review.getUser().getId(), review.getName(),  review.getRating(), review.getComment(), review.getCreatedAt());
		
		model.addAttribute("reviewEditForm", reviewEditForm);
		
		return "reviews/edit";
	}
	
	// レビューを更新する
	@PostMapping("/{reviewId}/update")
	public String updateReview(@PathVariable(name = "houseId") Integer houseId,
								@PathVariable(name = "reviewId") Integer reviewId,
								@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, 
								@ModelAttribute @Validated ReviewEditForm reviewEditForm, BindingResult bindingResult, RedirectAttributes redirectAttributes,Model model){
		
		// レビューを取得
		Optional<Review> optionalReview = reviewService.findReviewById(reviewId);
		// レビューがない場合
		if (optionalReview.isEmpty()) {
			// レビュー入力フォームで入力された内容を画面に表示するためmodelに追加
			redirectAttributes.addFlashAttribute("errorMessage","指定されたページが見つかりません。" );
			return "redirect:/houses";
		}		
		// 編集フォームに入力ミスがあったら
		if (bindingResult.hasErrors()) {			
			// 再度民宿情報を取得
			Optional<House> optionalHouse = houseService.findHouseById(houseId);
			// 民宿が見つからなければエラーメッセージを表示
			if(optionalHouse.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "指定された民宿は存在しません。");
				// もう一度編集フォーム画面を出す
				return "redirect:/houses/{houseId}";

			}
			
			// 編集画面を表示するのに必要な民宿情報をビューに渡す
			model.addAttribute("house", optionalHouse.get());
			// レビュー編集フォーム(レビュー内容や評価など)の入力内容をビューに渡す
			model.addAttribute("reviewEditForm",reviewEditForm);
			
			return "reviews/edit";
		}
				
		Review review = optionalReview.get();

		// ログイン中のユーザーとレビューのユーザーが一致するかチェック(ユーザーが自分のレビューしか更新できないようにする)
		if(!review.getUser().getId().equals(userDetailsImpl.getUser().getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "更新できません");
			return "redirect:/houses/{houseId}";
					
		}		
		// レビュー情報の更新
		reviewService.updateReview(reviewEditForm, review);
		redirectAttributes.addFlashAttribute("successMessage", "レビュー情報を更新しました。");
		
		return "redirect:/houses/{houseId}";
	}
	
	// レビューを削除する
	@PostMapping("/{reviewId}/delete")
	public String deleteReview(@PathVariable(name = "houseId") Integer houseId,
								@PathVariable(name = "reviewId")Integer reviewId, RedirectAttributes redirectAttributes) {
		Optional<Review>optionalReview = reviewService.findReviewById(reviewId);
		
		if(!optionalReview.isPresent()) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたページが見つかりません。");
			return "redirect:/houses/{houseId}";
		}
		
		Review review = optionalReview.get();
		
		reviewService.deleteReview(review);
		
		redirectAttributes.addFlashAttribute("successMessage", "レビューを削除しました。");
		
		return "redirect:/houses/{houseId}";		
		
	}
}

