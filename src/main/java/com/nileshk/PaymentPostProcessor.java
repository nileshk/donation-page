package com.nileshk;

import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface PaymentPostProcessor {

	@Async
	void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation);

	@Async
	void afterPaypalPayment(Payment payment, Donation donation);
}
