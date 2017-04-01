package com.nileshk;

import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PaymentController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String COLLECT_OCCUPATION_ENABLED_KEY = "collectOccupationEnabled";

	private String publishableKey;

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

	final List<PaymentPostProcessor> paymentPostProcessors;
	//PaymentPostProcessor paymentPostProcessor;

	@Autowired
	public PaymentController(
			@Value("${stripe.secretKey}") String secretKey,
			@Value("${stripe.publishableKey}") String publishableKey,
			/* PaymentPostProcessor paymentPostProcessor*/
			List<PaymentPostProcessor> paymentPostProcessors) {
		Stripe.apiKey = secretKey;
		this.publishableKey = publishableKey;
		//this.paymePostProcessor = paymentPostProcessor;
		this.paymentPostProcessors = paymentPostProcessors;
	}

	@RequestMapping(value = "/", method = GET)
	public String index(Model model) {
		defaultModel(model);
		return "index";
	}

	private void defaultModel(Model model) {
		// Use current time as vcs build id if in developement
		model.addAttribute("vcsBuildId", (isBlank(vcsBuildId) || "@buildNumber@".equals(vcsBuildId)) ? String.valueOf(new Date().getTime()) : vcsBuildId);

		model.addAttribute("organizationDisplayName", organizationDisplayName);
		model.addAttribute("mainPageUrl", mainPageUrl);
		String displaySiteTitle = isNotBlank(siteTitle) ? siteTitle : organizationDisplayName;
		model.addAttribute("siteTitle", displaySiteTitle);
		model.addAttribute(COLLECT_OCCUPATION_ENABLED_KEY, collectOccupationEnabled);
		model.addAttribute("collectOccupationThreshold", collectOccupationThreshold);
		model.addAttribute("donationLimit", donationLimit);
		model.addAttribute("pagePurpose", "donation");
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
		model.addAttribute("pagePurpose", "dues");
		return "index";
	}

	@RequestMapping(value = "/getConfig", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ClientConfig getClientConfig() {
		ClientConfig config = new ClientConfig();
		config.setPublishableKey(publishableKey);
		config.setOrganizationDisplayName(organizationDisplayName);
		config.setApplyPayEnabled(applePayEnabled);
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
			postProcess(param, chargeResult);
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

	private void postProcess(Map<String, Object> param, Charge charge) {
		try {
			if (paymentPostProcessors != null) {
				for (PaymentPostProcessor paymentPostProcessor : paymentPostProcessors) {
					if (paymentPostProcessor != null) {
						paymentPostProcessor.postProcessPayment(param, charge);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failure iterating over payment post processors", e);
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
