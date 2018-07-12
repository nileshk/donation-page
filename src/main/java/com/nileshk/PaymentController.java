package com.nileshk;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Item;
import com.paypal.api.payments.ItemList;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.paypal.base.Constants.LIVE;
import static com.paypal.base.Constants.SANDBOX;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PaymentController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String COLLECT_OCCUPATION_ENABLED_KEY = "collectOccupationEnabled";
	private static final String OG_URL_KEY = "ogUrl";

	private String publishableKey;

	@Value("${paypal.enabled:false}")
	private Boolean paypalEnabled;

	@Value("${paypal.sandbox:false}")
	private Boolean paypalSandbox;

	@Value("${paypal.sandboxClientId:}")
	private String paypalSandboxClientId;

	@Value("${paypal.sandboxSecret:}")
	private String paypalSandboxSecret;

	@Value("${paypal.clientId:}")
	private String paypalClientId;

	@Value("${paypal.secret:}")
	private String paypalSecret;

	@Value("${paypal.returnUrl:}")
	private String paypalReturnUrl;

	@Value("${paypal.cancelUrl:}")
	private String paypalCancelUrl;

	@Value("${app.clientLoggingEnabled:true}")
	private Boolean clientLoggingEnabled = true;

	@Value("${app.url:}")
	private String appUrl = "";

	@Value("${app.previewImageUrl:}")
	private String appPreviewImageUrl = "";

	@Value("${org.displayName:}")
	private String organizationDisplayName = "";

	@Value("${org.siteTitle:}")
	private String siteTitle = "";

	@Value("${messages.donateHeader:}")
	private String donateHeader = "";

	@Value("${url.mainPage:/}")
	private String mainPageUrl = "/";

	@Value("${stripe.applyPayEnabled:true}")
	private Boolean applePayEnabled = true;

	@Value("${email.signup.url:/}")
	private String emailSignupUrl = "/";

	@Value("${app.collectOccupationEnabled:true}")
	private Boolean collectOccupationEnabled = true;

	@Value("${app.collectOccupationThreshold:100}")
	private Integer collectOccupationThreshold;

	@Value("${app.donationLimit:-1}")
	private Integer donationLimit;

	@Value("${app.googleAnalyticsTrackingId:}")
	private String googleAnalyticsTrackingId;

	@Value("${vcs.build.id}")
	private String vcsBuildId;

	final private Processors processors;

	@Autowired
	public PaymentController(
			@Value("${stripe.secretKey}") String secretKey,
			@Value("${stripe.publishableKey}") String publishableKey,
			Processors processors
			) {
		Stripe.apiKey = secretKey;
		this.publishableKey = publishableKey;
		this.processors = processors;
	}

	@RequestMapping(value = "/", method = GET)
	public String index(Model model) {
		defaultModel(model);
		return "index";
	}

	private void defaultModel(Model model) {
		// Use current time as vcs build id if in developement
		model.addAttribute("vcsBuildId", (isBlank(vcsBuildId) || "@buildNumber@".equals(vcsBuildId)) ? String.valueOf(new Date().getTime()) : vcsBuildId);

		model.addAttribute("stripePublishableKey", publishableKey);
		model.addAttribute("paypalEnabled", paypalEnabled);
		model.addAttribute("paypalSandbox", paypalSandbox);

		model.addAttribute("clientLoggingEnabled", clientLoggingEnabled);
		model.addAttribute("organizationDisplayName", organizationDisplayName);
		if (isNotBlank(appUrl)) {
			model.addAttribute(OG_URL_KEY, appUrl);
		}
		if (isNotBlank(appPreviewImageUrl)) {
			model.addAttribute("ogImageUrl", appPreviewImageUrl);
		}
		model.addAttribute("mainPageUrl", mainPageUrl);
		String displaySiteTitle = isNotBlank(siteTitle) ? siteTitle : organizationDisplayName;
		model.addAttribute("siteTitle", displaySiteTitle);
		model.addAttribute(COLLECT_OCCUPATION_ENABLED_KEY, collectOccupationEnabled);
		model.addAttribute("collectOccupationThreshold", collectOccupationThreshold);
		model.addAttribute("donationLimit", donationLimit);
		model.addAttribute("pagePurpose", PaymentContants.DONATION_PURPOSE);
		model.addAttribute("pagePurposeText", "Contribute to");
		if (isNotBlank(googleAnalyticsTrackingId)) {
			model.addAttribute("googleAnalyticsTrackingId", googleAnalyticsTrackingId);
		}
		/*
		String displayedDonateHeader = isNotEmpty(this.donateHeader) ? this.donateHeader :
				(isNotEmpty(organizationDisplayName)
						? ("Donate to " + organizationDisplayName)
						: ("Donate:"));
		model.addAttribute("donateHeader", displayedDonateHeader);
		*/
	}

	/**
	 * @param model
	 * @return Page that is an HTML fragment for integrating into other apps
	 */
	@RequestMapping(value = "/fragment", method = GET)
	public String fragment(Model model) {
		defaultModel(model);
		return "fragment";
	}

	/**
	 * Pay Dues page
	 * @param model
	 * @return page for paying dues
	 */
	@RequestMapping(value = "/dues", method = GET)
	public String payDues(Model model) {
		defaultModel(model);
		model.addAttribute(COLLECT_OCCUPATION_ENABLED_KEY, false); // Don't collect occupation for paying dues (this should be on file for members)
		model.addAttribute("pagePurposeText", "Pay Dues for");
		model.addAttribute("allowSpecificAmount", false);
		model.addAttribute("donateButtonsEnabled", false);
		model.addAttribute("payDuesPage", true);
		model.addAttribute("pagePurpose", PaymentContants.DUES_PURPOSE);
		if (isNotBlank(appUrl)) {
			model.addAttribute(OG_URL_KEY, appUrl + "dues");
		}
		return "index";
	}

	/**
	 * Generic Pay page
	 * @param model
	 * @return page for paying specific amount
	 */
	@RequestMapping(value = "/pay", method = GET)
	public String pay(Model model) {
		defaultModel(model);
		model.addAttribute(COLLECT_OCCUPATION_ENABLED_KEY, false); // Don't collect occupation for generic pay
		model.addAttribute("pagePurposeText", "Pay");
		model.addAttribute("allowSpecificAmount", false);
		model.addAttribute("donateButtonsEnabled", false);
		model.addAttribute("genericPayPage", true);
		model.addAttribute("pagePurpose", PaymentContants.PAY_PURPOSE);
		if (isNotBlank(appUrl)) {
			model.addAttribute(OG_URL_KEY, appUrl + "pay");
		}
		return "index";
	}

	@RequestMapping(value = "/getConfig", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ClientConfig getClientConfig(HttpServletRequest request) {
		ClientConfig config = new ClientConfig();
		config.setPublishableKey(publishableKey);
		config.setOrganizationDisplayName(organizationDisplayName);
		config.setApplyPayEnabled(applePayEnabled);
		if (request != null && request.getSession(true) != null) {
			HttpSession session = request.getSession(true);
			if (session != null) {
				logger.info("Session ID: " + session.getId());
			}
		}
		return config;
	}


	@RequestMapping(value = "/submitPayment", method = POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseBody
	public ChargeResult submitPayment(
			@RequestBody Map<String, Object> param
	) {
		logger.info("--- Submitting Payment ---");

		Boolean collectOccupationEnabled = this.collectOccupationEnabled;

		if (param != null) {
			logger.info(param.toString());
		}
		Map<String, Object> clientParam = new HashMap<>();
		if (param.containsKey("logData")) {
			logger.info("Request LOG:");
			logger.info(param.get("logData").toString());
		}
		Integer amount = (Integer) param.getOrDefault("amount", null);
		if (amount != null) {
			logger.info("Amount: " + amount);
			clientParam.put("amount", amount);

			if (donationLimit != null && donationLimit > 0 && amount > donationLimit * 100) {
				return ChargeResult.error(String.format("$%d donation exceeds limit of $%d", amount / 100, donationLimit));
			}

		}
		if (param.containsKey("currency")) {
			logger.info("Currency: " + param.get("currency"));
			clientParam.put("currency", param.get("currency"));
		}
		if (param.containsKey("description")) {
			String description = (String)param.get("description");
			logger.info("Description: " + description);
			clientParam.put("description", description);
			clientParam.put("receipt_email", description);
		}

		String occupation = (String) param.getOrDefault("occupation", null);

		if (param.containsKey(COLLECT_OCCUPATION_ENABLED_KEY)) {
			collectOccupationEnabled = (Boolean) param.get(COLLECT_OCCUPATION_ENABLED_KEY);
		}
		if (collectOccupationEnabled == null) {
			collectOccupationEnabled = false;
		}

		if (isNotBlank(occupation)) {
			logger.info("Occupation: " + occupation);
		} else if (collectOccupationEnabled && (collectOccupationThreshold != null && (amount == null || (amount > collectOccupationThreshold * 100)))) {
			return ChargeResult.error("Occupation not provided");
		}

		clientParam.put("source", param.get("id"));
		try {
			logger.info(clientParam.toString());
			Charge chargeResult = Charge.create(clientParam);
			logger.info("Charge successful");
			if (param.containsKey("description")) {
				logger.info("EMAIL: " + param.get("description"));
			}
			logger.info("Charge Result:");
			logger.info(chargeResult.toJson());
			logger.info("-------------------------");
			// TODO Do failed charges get to this point?
			processors.postProcess(param, chargeResult);
			logger.info("Post-processing initiated");
			return new ChargeResult(chargeResult);
		} catch (AuthenticationException e) {
			logger.error("AuthenticationException", e);
			return ChargeResult.error(e.getMessage());
		} catch (InvalidRequestException e) {
			logger.error("InvalidRequestException", e);
			return ChargeResult.error(e.getMessage());
		} catch (APIConnectionException e) {
			logger.error("APIConnectionException", e);
			return ChargeResult.error(e.getMessage());
		} catch (CardException e) {
			logger.error("CardException", e);
			return ChargeResult.error(e.getMessage());
		} catch (APIException e) {
			logger.error("APIException", e);
			return ChargeResult.error(e.getMessage());
		} catch (RuntimeException e) {
			logger.error("RuntimeException", e);
			return ChargeResult.error("Application error occurred, please contact admin:" + e.getMessage());
		}
	}


	@NotNull
	private APIContext getPaypalContext() {
		return paypalSandbox
					? new APIContext(paypalSandboxClientId, paypalSandboxSecret, SANDBOX)
					: new APIContext(paypalClientId, paypalSecret, LIVE);
	}

	@RequestMapping(value = "/paypalCreatePayment", method = POST,
			// consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseBody
	public PaypalCreatePaymentResponse paypalCreatePayment(@RequestParam("amount") String amount) {
		logger.info("paypalCreatePayment (sandbox = " + paypalSandbox + ")");

		APIContext context = getPaypalContext();
		Payment payment = new Payment();
		try {
			payment.setIntent("sale");
			// payment.setExperienceProfileId("");
			// payment.setRedirectUrls();
			payment.setPayer(new Payer().setPaymentMethod("paypal"));

			List<Transaction> transactions = new ArrayList<>();
			Transaction transaction = new Transaction();
			transaction.setAmount(new Amount().setTotal(amount).setCurrency("USD"));
			transaction.setDescription("Donation");
			ItemList itemList = new ItemList();
			List<Item> items = new ArrayList<>();
			items.add(new Item()
					.setQuantity("1")
					.setName("Donation")
					.setPrice(amount)
					.setCurrency("USD")
					.setDescription("Donation"));
			itemList.setItems(items);
			transaction.setItemList(itemList);
			transactions.add(transaction);
			payment.setTransactions(transactions);
			payment.setRedirectUrls(new RedirectUrls()
					.setCancelUrl(paypalCancelUrl)
					.setReturnUrl(paypalReturnUrl));
			logger.info("Payment: " + payment.toJSON());
			Payment response = payment.create(context);
			logger.info("Paypal payment created:" + response.toJSON());

			return new PaypalCreatePaymentResponse(response.getId());
		} catch (PayPalRESTException e) {
			logger.error("Failed to create PayPal Payment object", e);
		}
		return null;
	}

	@RequestMapping(value = "/paypalExecutePayment", method = POST,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseBody
	public String paypalExecutePayment(
			@RequestParam("payToken") String payToken,
			@RequestParam("payerId") String payerId,
			@RequestParam("pagePurpose") String pagePurpose,
			@RequestParam("occupation") String occupation
			) {
		logger.info("payerToken: " + payToken);
		logger.info("payerId: " + payerId);
		logger.info("pagePurpose: " + pagePurpose);
		logger.info("occupation: " + occupation);

		APIContext context = getPaypalContext();
		Payment payment = new Payment().setId(payToken);
		PaymentExecution paymentExecution = new PaymentExecution();
		paymentExecution.setPayerId(payerId);
		try {
			Payment createdPayment = payment.execute(context, paymentExecution);
			logger.info(createdPayment.toJSON());
			processors.afterPaypal(createdPayment, pagePurpose, occupation);
			return createdPayment.toJSON();
		} catch (PayPalRESTException e) {
			logger.error("Error executing PayPal payment", e);
			return "";
		}
	}


	@RequestMapping(value = "/successfulPayment", method = GET)
	public String successfulPayment(
			@RequestParam(value = "amount", required = true) Long amount,
			@RequestParam(value = "email", required = false, defaultValue = "") String email,
			Model model) {
		defaultModel(model);
		model.addAttribute("amount", String.valueOf(amount / 100));
		model.addAttribute("email", email);
		try {
			model.addAttribute("email_urlEncoded", URLEncoder.encode(email, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("URL encoding error", e);
		}
		model.addAttribute("emailSignupUrl", emailSignupUrl);
		return "successful_payment";
	}

}
