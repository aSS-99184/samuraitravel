const stripe = Stripe('pk_test_51R6mqrRSOota6fUsZ44ZqeSyzuIwi3uMb2jOeuXCBRbcYz5rolNNpStiFRvz3wsmsJAOhrFjnjMtB7Ji9yK8IkGx00nXaN4nsX');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
	stripe.redirectToCheckout({
		sessionId: sessionId
	})
});