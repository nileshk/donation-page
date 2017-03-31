var _stripePaymentsBaseUrl = (typeof _stripePaymentsBaseUrl === 'undefined') ? '' : _stripePaymentsBaseUrl;

var _DonationPage_handleDonate;

// Polyfill startsWith
if (!String.prototype.startsWith) {
  String.prototype.startsWith = function(searchString, position) {
    position = position || 0;
    return this.indexOf(searchString, position) === position;
  };
}

function init(publishableKey, organizationDisplayName, applyPayEnabledConfigured) {
	"use strict";
	//noinspection JSUnresolvedVariable
	Stripe.setPublishableKey(publishableKey);
	var collectOccupationEnabled = (typeof _DonationPage_collectOccupationEnabled === 'undefined') ? true : _DonationPage_collectOccupationEnabled;
	var collectOccupationThreshold = (typeof _DonationPage_collectOccupationThreshold === 'undefined') ? 100 : _DonationPage_collectOccupationThreshold;
	var donationLimit = (typeof _DonationPage_donationLimit === 'undefined') ? -1 : _DonationPage_donationLimit;

	var submittedAmount = 0;
	var submittedAmountStr = "";
	var applePayEnabled = false;
	var occupation = "";

	function isEmpty(str) {
	    return (!str || 0 === str.length);
	}

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
		hideMultiPay();
		$('.donation-selection').show();
	}

	var handler = StripeCheckout.configure({
		key: publishableKey,
		image: 'https://stripe.com/img/documentation/checkout/marketplace.png',
		locale: 'auto',
		currency: 'usd',
		billingAddress: true,
		allowRememberMe: true,
		token: function(token) {
			// You can access the token ID with `token.id`.
			// Get the token ID to your server-side code for use.
			var param = {
				id: token.id,
				currency: 'usd',
				amount: submittedAmount,
				description: token.email,
				occupation: occupation,
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
				hideProcessingPayment();
				// console.log(data);
				if (!data.error) {
					doSuccess(data.amount, data.email);
				} else {
					errorDialog(data.errorMessage);
				}
			}).fail(function(jqXHR, textStatus, errorThrown) {
				hideProcessingPayment();
				errorDialog(textStatus + ": " + errorThrown);
			}).always(function() {
				hideProcessingPayment();
			});
		}
	});

	function beginApplePay() {
		hideMultiPay();
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
					currency: 'usd',
					description: result.shippingContact.emailAddress,
					occupation: occupation,
					applePayResult: result,
					logData: JSON.stringify(result)
				};

				$.ajax({
						type: 'POST',
						url: _stripePaymentsBaseUrl + 'submitPayment',
						data: JSON.stringify(param),
						contentType: "application/json",
						dataType: 'json'
				}).done(function(data) {
					completion(ApplePaySession.STATUS_SUCCESS);
					// You can now redirect the user to a receipt page, etc.
					if (!data.error) {
						doSuccess(data.amount, data.email);
					} else {
						errorDialog(data.errorMessage);
					}
				}).fail(function(x, textStatus, errorThrown) {
					completion(ApplePaySession.STATUS_FAILURE);
					errorDialog(textStatus + ": " + errorThrown);
				}).always(function() {
					hideProcessingPayment();
				});


			}, function(error) {
				hideProcessingPayment();
				// console.log(error.message);
				errorDialog(error.message);
			});

		session.begin();
	}

	function doCreditCardDonate() {
		hideMultiPay();
		handler.open({
			name: organizationDisplayName,
			description: 'Donate $' + submittedAmountStr,
			amount: submittedAmount
		});
	}

	function hideMultiPay() {
		$('#multi-pay-options').modal('hide');
	}

	function hideProcessingPayment() {
		$('#processingPaymentDialog').modal('hide');
	}

	function handleDonate(amount, amountStr) {
		// Open Checkout with further options:
		submittedAmount = amount;
		submittedAmountStr = amountStr;

		occupation = $('#occupationInput').val();
		var shouldReturn = false;

		if (collectOccupationEnabled && submittedAmount > (collectOccupationThreshold * 100) && isEmpty(occupation)) {
			$('#alertOccupationText').text("Please provide your occupation");
			$('#alertOccupation').removeClass("hidden");
			shouldReturn = true;
		} else {
			$('#alertOccupation').addClass("hidden");
		}
		if (donationLimit > 0 && submittedAmount > (donationLimit * 100)) {
			$('#alertCustomDonationText').text("Donation exceeds limit of $" + donationLimit + ".");
			$('#alertCustomDonation').removeClass("hidden");
			shouldReturn = true;
		} else {
			$('#alertCustomDonation').addClass("hidden");
		}
		if (shouldReturn) {
			return;
		}

		if (!applePayEnabled) {
			doCreditCardDonate();
		} else {
			$('#donationAmountAlert').text("Amount: $" + submittedAmountStr);
			//$('.donation-selection').hide();
			$('#multi-pay-options').modal();
		}
	}

	_DonationPage_handleDonate = handleDonate;

	$('#cancel-button').click(function() {
		$('.donation-selection').show();
		hideMultiPay();
	});

	$('#credit-pay-button').click(doCreditCardDonate);

	$('#apple-pay-button').click(beginApplePay);

	$('#donateButton_custom').click(function(e) {
		var amount = document.getElementById("donationAmount").value;
		if (amount) {
			handleDonate(parseInt(amount) * 100, amount);
		}
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
				// console.log("Apple Pay enabled");
				//document.getElementById('apple-pay-button').style.display = 'block';
			} else {
				// console.log("Apple Pay not available");
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
			alert("Initial page load failed");
			console.log("textStatus: " + textStatus);
			console.log("errorThrown: " + errorThrown);
		}
	});
});
