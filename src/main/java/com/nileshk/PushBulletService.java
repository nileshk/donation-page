package com.nileshk;

import com.github.sheigutn.pushbullet.Pushbullet;
import com.github.sheigutn.pushbullet.items.push.sendable.SendablePush;
import com.github.sheigutn.pushbullet.items.push.sendable.defaults.SendableNotePush;
import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Service
public class PushBulletService implements PaymentPostProcessor {

	private List<String> apiTokens;
	private String siteTitle;
	private String displayName;

	public PushBulletService(@Value("${pushbullet.apiTokens:}") String apiTokens,
			@Value("${org.siteTitle:}") String siteTitle,
			@Value("${org.displayName:}") String displayName) {
		this.apiTokens = Arrays.asList(apiTokens.split(","));
		this.siteTitle = siteTitle;
		this.displayName = displayName;
	}

	@Override
	@Async
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		push(donation, "Stripe", charge.getId());
	}

	@Async
	@Override
	public void afterPaypalPayment(Payment payment, Donation donation) {
		push(donation, "PayPal", payment.getId());
	}

	private void push(Donation donation, String paymentType, String id) {
		String title = String.format("Donation: %s from %s to %s", donation.getAmountString(), donation.getName(), displayName);
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Donation: %s from %s to %s", donation.getAmountString(), donation.getName(), siteTitle())).append("\n")
				.append("Name: ").append(trimToEmpty(donation.getName())).append("\n")
				.append("Amount: ").append(trimToEmpty(donation.getAmountString())).append("\n")
				.append("Email: ").append(trimToEmpty(donation.getEmail())).append("\n")
				.append("Address1: ").append(trimToEmpty(donation.getAddress1())).append("\n")
				.append("Address2: ").append(trimToEmpty(donation.getAddress2())).append("\n")
				.append("City: ").append(trimToEmpty(donation.getCity())).append("\n")
				.append("State: ").append(trimToEmpty(donation.getState())).append("\n")
				.append("Zip: ").append(trimToEmpty(donation.getZip())).append("\n")
				.append("Country: ").append(trimToEmpty(donation.getCountry())).append("\n")
				.append("Occupation: ").append(trimToEmpty(donation.getOccupation())).append("\n")
				.append("Purpose: ").append(trimToEmpty(donation.getPurpose())).append("\n")
				.append(paymentType).append(" ID: ").append(trimToEmpty(id)).append("\n")
				.append("Date: ").append(trimToEmpty(donation.getPaymentDate().toString())).append("\n");
		String body = sb.toString();
		if (apiTokens != null && apiTokens.size() > 0) {
			for (String apiToken : apiTokens) {
				Pushbullet pushbullet = new Pushbullet(apiToken);
				SendablePush push = new SendableNotePush(title, body);
				pushbullet.push(push);
			}
		}
	}

	private String siteTitle() {
		return isNotBlank(siteTitle) ? siteTitle : displayName;
	}
}
