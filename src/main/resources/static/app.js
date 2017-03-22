var _stripePaymentsBaseUrl = (typeof _stripePaymentsBaseUrl === 'undefined') ? '' : _stripePaymentsBaseUrl;

function init(publishableKey, organizationDisplayName, applyPayEnabledConfigured) {
	"use strict";
	//noinspection JSUnresolvedVariable
	Stripe.setPublishableKey(publishableKey);

	var submittedAmount = 0;
	var submittedAmountStr = "";
	var applePayEnabled = false;

	function doSuccess(amount, email) {
		var loc = "successfulPayment?amount=" + amount;
		if (email) {
			loc += "&email=" + email;
		}
		window.location = loc;
	}

	function errorDialog(errorMessage) {
		$("#errorMessage").text(errorMessage);
		$('#errorDialog').modal();
	}

	var handler = StripeCheckout.configure({
		key: publishableKey,
		image: 'https://stripe.com/img/documentation/checkout/marketplace.png',
		locale: 'auto',
		currency: 'usd',
		shippingAddress: true,
		allowRememberMe: true,
		token: function(token) {
			// You can access the token ID with `token.id`.
			// Get the token ID to your server-side code for use.
			var param = {
				id: token.id,
				currency: 'usd',
				amount: submittedAmount,
				description: token.email,
				token: token
			};
			// console.log(param);
			$('#processingPaymentDialog').modal();
			$.ajax({
				type: 'POST',
				url: _stripePaymentsBaseUrl + 'submitPayment',
				data: JSON.stringify(param),
				contentType: "application/json",
				dataType: 'json'
			}).done(function(data) {
				$('#processingPaymentDialog').modal('hide');
				console.log(data);
				if (!data.error) {
					doSuccess(data.amount, data.email);
				} else {
					errorDialog(data.errorMessage);
				}
			}).fail(function(jqXHR, textStatus, errorThrown) {
				$('#processingPaymentDialog').modal('hide');
				errorDialog(textStatus + ": " + errorThrown);
			}).always(function() {
				$('#processingPaymentDialog').modal('hide');
			});
		}
	});

	function beginApplePay() {
		var paymentRequest = {
			requiredShippingContactFields: ['email', 'name', 'postalAddress'],
			countryCode: 'US',
			currencyCode: 'USD',
			total: {
				label: organizationDisplayName,
				amount: submittedAmountStr
			}
		};
		var session = Stripe.applePay.buildSession(paymentRequest,
			function(result, completion) {
				$('#processingPaymentDialog').modal();
				var param = {
					id: result.token.id,
					amount: submittedAmount,
					description: result.shippingContact.emailAddress,
					logData: JSON.stringify(result)
				};

				$.ajax({
						type: 'POST',
						url: _stripePaymentsBaseUrl + 'submitPayment',
						data: JSON.stringify(param),
						contentType: "application/json",
						dataType: 'json'
				}).done(function() {
					completion(ApplePaySession.STATUS_SUCCESS);
					// You can now redirect the user to a receipt page, etc.
					doSuccess(submittedAmount, null);
				}).fail(function(x, textStatus, errorThrown) {
					completion(ApplePaySession.STATUS_FAILURE);
					errorDialog(textStatus + ": " + errorThrown);
				}).always(function() {
					$('#processingPaymentDialog').modal('hide');
				});


			}, function(error) {
				$('#processingPaymentDialog').modal('hide');
				console.log(error.message);
				errorDialog(error.message);
			});

		session.begin();
	}

	function doCreditCardDonate() {
		handler.open({
			name: organizationDisplayName,
			description: 'Donate $' + submittedAmountStr,
			amount: submittedAmount
		});
	}

	function handleDonate(amount, amountStr) {
		// Open Checkout with further options:
		submittedAmount = amount;
		submittedAmountStr = amountStr;
		if (!applePayEnabled) {
			doCreditCardDonate();
		} else {
			$('#donationAmountAlert').text("Amount: $" + submittedAmountStr);
			$('.donation-selection').hide();
			$('.multi-pay-options').show();
		}
	}

	$('#cancel-button').click(function() {
		$('.donation-selection').show();
		$('.multi-pay-options').hide();
	});

	$('#credit-pay-button').click(doCreditCardDonate);

	$('#apple-pay-button').click(beginApplePay);

	document.getElementById('donateButton_custom').addEventListener('click', function(e) {
		var amount = document.getElementById("donationAmount").value;
		if (amount) {
			handleDonate(parseInt(amount) * 100, amount);
		}
		e.preventDefault();
	});

	document.getElementById('donateButton_5').addEventListener('click', function(e) {
		handleDonate(500, "5");
		e.preventDefault();
	});

	document.getElementById('donateButton_10').addEventListener('click', function(e) {
		handleDonate(1000, "10");
		e.preventDefault();
	});

	document.getElementById('donateButton_25').addEventListener('click', function(e) {
		handleDonate(2500, "25");
		e.preventDefault();
	});

	document.getElementById('donateButton_50').addEventListener('click', function(e) {
		handleDonate(5000, "50");
		e.preventDefault();
	});

	document.getElementById('donateButton_100').addEventListener('click', function(e) {
		handleDonate(10000, "100");
		e.preventDefault();
	});

	// Close Checkout on page navigation:
	window.addEventListener('popstate', function() {
		handler.close();
	});

	if (applyPayEnabledConfigured) {
		// Apple Pay
		Stripe.applePay.checkAvailability(function(available) {
			if (available) {
				applePayEnabled = true;
				console.log("Apple Pay enabled");
				//document.getElementById('apple-pay-button').style.display = 'block';
			} else {
				console.log("Apple Pay not available");
			}
		});
	}
}
$(document).ready(function() {
	$.ajax({
		type: 'GET',
		url: _stripePaymentsBaseUrl + 'getConfig',
		contentType: "application/json",
		dataType: 'json',
		success: function(data) {
			//console.log(data);
			if (data.publishableKey.startsWith("pk_test")) {
				console.log("TEST MODE - No money will be transferred");
				$('#alertTop').addClass('alert').addClass('alert-danger').text('! TEST MODE - No money will be transferred !');
			}
			init(data.publishableKey, data.organizationDisplayName, data.applyPayEnabled);
		},
		error: function(x, textStatus, errorThrown) {
			console.log("textStatus: " + textStatus);
			console.log("errorThrown: " + errorThrown);
			alert("Initial page load failed");
		}
	});
});
