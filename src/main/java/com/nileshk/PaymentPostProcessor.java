package com.nileshk;

import com.stripe.model.Charge;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface PaymentPostProcessor {

	@Async
	void postProcessPayment(Map<String, Object> map, Charge charge);

}
