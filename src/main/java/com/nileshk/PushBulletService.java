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
		push(donation);
	}

	@Async
	@Override
	public void afterPaypalPayment(Payment payment, Donation donation) {
		push(donation);
	}

	private void push(Donation donation) {
		String title = String.format("Donation: %s from %s to %s", donation.getAmountString(), donation.getName(), displayName);
		String body = String.format("Donation: %s from %s to %s", donation.getAmountString(), donation.getName(), siteTitle());
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
