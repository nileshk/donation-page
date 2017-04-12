if (typeof _DONATION_PAGE_APP_ === 'undefined') { var _DONATION_PAGE_APP_ = {}; }

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
	var app = _DONATION_PAGE_APP_;

	//noinspection JSUnresolvedVariable
	Stripe.setPublishableKey(publishableKey);
	var collectOccupationEnabled = (typeof app.collectOccupationEnabled === 'undefined') ? true : app.collectOccupationEnabled;
	var collectOccupationThreshold = (typeof app.collectOccupationThreshold === 'undefined') ? 100 : app.collectOccupationThreshold;
	var donationLimit = (typeof app.donationLimit === 'undefined') ? -1 : app.donationLimit;
	var payDuesPage = (typeof app.payDuesPage === 'undefined') ? false : app.payDuesPage;
	var pagePurpose = (typeof app.pagePurpose === 'undefined') ? "donation" : app.pagePurpose;
	var clientLoggingEnabled = (typeof app.clientLoggingEnabled === 'undefined') ? true : app.clientLoggingEnabled;

	var paypalEnabled = (typeof app.paypalEnabled === 'undefined') ? false : app.paypalEnabled;

	var submittedAmount = 0;
	var submittedAmountStr = "";
	var applePayEnabled = false;
	var occupation = "";

	function log(logObject) {
		setTimeout(function() {
			if (clientLoggingEnabled) {
				$.ajax({
					type: 'POST',
					url: _stripePaymentsBaseUrl + 'log',
					data: JSON.stringify(logObject),
					contentType: "application/json",
					dataType: 'json'
				}).fail(function(jqXHR, textStatus, errorThrown) {
					if (window.console) {
						console.log(textStatus + ": " + errorThrown);
						console.log("Failed to log: ");
						console.log(logObject);
					}
				});
			}
			if (window.console) {
				console.log(logObject);
			}
		}, 10);
	}

	log('Initializing...');

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
		hideProcessingPayment();
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
				collectOccupationEnabled: collectOccupationEnabled,
				pagePurpose: pagePurpose,
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
		log('Beginning Apple Pay...');
		hideMultiPay();
		var shippingContactFields = ['email', 'name', 'postalAddress'];
		/* Uncomment this if we don't want to require address for paying dues
		if (payDuesPage) {
			shippingContactFields = ['email', 'name'];
		}
		*/
		var paymentRequest = {
			requiredShippingContactFields: shippingContactFields,
			countryCode: 'US',
			currencyCode: 'USD',
			total: {
				label: organizationDisplayName,
				amount: submittedAmountStr
			}
		};
		log('Payment request:');
		log(paymentRequest);
		var session = Stripe.applePay.buildSession(paymentRequest,
			function(result, completion) {
				log('Apple Pay result:');
				log(result);
				$('#processingPaymentDialog').modal();
				var param = {
					id: result.token.id,
					amount: submittedAmount,
					currency: 'usd',
					description: result.shippingContact.emailAddress,
					occupation: occupation,
					collectOccupationEnabled: collectOccupationEnabled,
					pagePurpose: pagePurpose,
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
					// You can now redirect the user to a receipt page, etc.
					if (!data.error) {
						completion(ApplePaySession.STATUS_SUCCESS);
						doSuccess(data.amount, data.email);
					} else {
						completion(ApplePaySession.STATUS_FAILURE);
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
		log('Doing credit card donate');
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
		log('Selected amount: $' + submittedAmountStr);

		if (collectOccupationEnabled && submittedAmount > (collectOccupationThreshold * 100) && isEmpty(occupation)) {
			$('#alertOccupationText').text("Please provide your occupation");
			$('#alertOccupation').removeClass("hidden");
			shouldReturn = true;
			log('Occupation not provided');
		} else {
			$('#alertOccupation').addClass("hidden");
			if (!isEmpty(occupation)) {
				log("Occupation: " + occupation);
			}
		}
		if (donationLimit > 0 && submittedAmount > (donationLimit * 100)) {
			var donationLimitErrorText = "Donation exceeds limit of $" + donationLimit + ".";
			$('#alertCustomDonationText').text(donationLimitErrorText);
			$('#alertCustomDonation').removeClass("hidden");
			shouldReturn = true;
			log(donationLimitErrorText);
		} else {
			$('#alertCustomDonation').addClass("hidden");
		}
		if (shouldReturn) {
			return;
		}

		if (!applePayEnabled && !paypalEnabled) {
			doCreditCardDonate();
		} else {
			$('#donationAmountAlert').text("Amount: $" + submittedAmountStr);
			//$('.donation-selection').hide();
			$('#multi-pay-options').modal();
			log('Showing multiple pay options');
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
				log("Apple Pay enabled");
				//document.getElementById('apple-pay-button').style.display = 'block';
			} else {
				log("Apple Pay not available");
			}
		});
	}

	if (paypalEnabled) {
		paypal.Button.render({
			// Set your environment
			env: app.paypalSandbox ? 'sandbox' : 'production',

			style: {
				label: 'checkout', // checkout || credit
				size: 'medium',    // tiny | small | medium
				shape: 'pill',     // pill | rect
				color: 'blue'      // gold | blue | silver
			},

			payment: function() {
				return paypal.request.post(_stripePaymentsBaseUrl + 'paypalCreatePayment', {amount: submittedAmountStr})
					.then(function(res) {
						return res.payToken;
					});
			},

			onAuthorize: function(data, actions) {
				return paypal.request.post(_stripePaymentsBaseUrl + 'paypalExecutePayment', {
					payToken: data.paymentID,
					payerId: data.payerID,
					occupation: occupation,
					pagePurpose: pagePurpose
				}).then(function(data) {
					log("Paypal execute successful, redirecting to success page.");
					doSuccess(submittedAmount, data.payer.payer_info.email);
					//document.querySelector('#paypal-button-container').innerText = 'Payment Complete!';
				});
				/*.error(function(jqXHR, textStatus, errorThrown) {
				 if (console) {
				 console.log(jqXHR);
				 console.log(textStatus);
				 console.log(errorThrown);
				 }
				 }); */
			}
		}, '#paypal-button-container');
	}

	log('Initialization complete');
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
