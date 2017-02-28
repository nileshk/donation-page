package com.nileshk;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PaymentController {

	private String publishableKey;

	public PaymentController(
			@Value("${stripe.secretKey}") String secretKey,
			@Value("${stripe.publishableKey}") String publishableKey) {
		Stripe.apiKey = secretKey;
		this.publishableKey = publishableKey;
	}


	@RequestMapping(value = "/getPublishableKey", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Json getPublishableKey() {
		return new Json("\"" + publishableKey + "\"");
	}


	@RequestMapping(value = "/submitPayment", method = POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ResponseBody
	public Json submitPayment(
			@RequestBody Map<String, Object> param
	) {
		Map<String, Object> clientParam = new HashMap<>();
		clientParam.put("amount", param.get("amount"));
		clientParam.put("currency", param.get("currency"));
		clientParam.put("description", param.get("description"));
		clientParam.put("card", param.get("id"));
		try {
			Charge chargeResult = Charge.create(clientParam);
			String chargeResultJson = chargeResult.toJson();
			System.out.println(chargeResultJson);
			return new Json(chargeResultJson);
		} catch (AuthenticationException e) {
			e.printStackTrace();
		} catch (InvalidRequestException e) {
			e.printStackTrace();
		} catch (APIConnectionException e) {
			e.printStackTrace();
		} catch (CardException e) {
			e.printStackTrace();
		} catch (APIException e) {
			e.printStackTrace();
		}
		return null; // TODO error response
	}

	class Json {
		private final String value;

		public Json(String value) {
			this.value = value;
		}

		@JsonValue
		@JsonRawValue
		public String value() {
			return value;
		}
	}

}
