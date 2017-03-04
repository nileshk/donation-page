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
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PaymentController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private String publishableKey;

	@Value("${org.displayName:}")
	private String organizationDisplayName = "";

	@Value("${stripe.applyPayEnabled:true}")
	private Boolean applePayEnabled = true;

	@Value("${email.signup.url:/}")
	private String emailSignupUrl = "/";

	public PaymentController(
			@Value("${stripe.secretKey}") String secretKey,
			@Value("${stripe.publishableKey}") String publishableKey) {
		Stripe.apiKey = secretKey;
		this.publishableKey = publishableKey;
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
		Map<String, Object> clientParam = new HashMap<>();
		if (param.containsKey("logData")) {
			logger.info("Request LOG:");
			logger.info(param.get("logData").toString());
		}
		if (param.containsKey("amount")) {
			logger.info("Amount: " + param.get("amount"));
			clientParam.put("amount", param.get("amount"));
		}
		if (param.containsKey("currency")) {
			logger.info("Currency: " + param.get("currency"));
			clientParam.put("currency", param.get("currency"));
		}
		if (param.containsKey("description")) {
			logger.info("Description: " + param.get("description"));
			clientParam.put("description", param.get("description"));
		}
		clientParam.put("source", param.get("id"));
		try {
			Charge chargeResult = Charge.create(clientParam);
			logger.info("Charge successful");
			if (param.containsKey("description")) {
				logger.info("EMAIL: " + param.get("description"));
			}
			logger.info("Charge Result:");
			logger.info(chargeResult.toJson());
			logger.info("-------------------------");
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

	@RequestMapping(value = "/successfulPayment", method = GET)
	public String successfulPayment(
			@RequestParam(value = "amount", required = true) Long amount,
			@RequestParam(value = "email", required = false, defaultValue = "") String email,
			Model model) {
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
