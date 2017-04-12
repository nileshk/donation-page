package com.nileshk;

import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProcessorsImpl implements Processors {

	private static final Logger logger = Logger.getLogger(ProcessorsImpl.class);

	final List<PaymentPostProcessor> paymentPostProcessors;

	public ProcessorsImpl(List<PaymentPostProcessor> paymentPostProcessors) {
		this.paymentPostProcessors = paymentPostProcessors;
	}

	@Override
	@Async
	public void postProcess(Map<String, Object> param, Charge charge) {
		try {
			if (paymentPostProcessors != null) {
				for (PaymentPostProcessor paymentPostProcessor : paymentPostProcessors) {
					if (paymentPostProcessor != null) {
						paymentPostProcessor.postProcessPayment(param, charge, new Donation(param));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failure iterating over payment post processors", e);
		}
	}

	@Override
	@Async
	public void afterPaypal(Payment payment, String purpose, String occupation) {
		try {
			Donation donation = new Donation(payment, purpose, occupation);
			if (paymentPostProcessors != null) {
				for (PaymentPostProcessor paymentPostProcessor : paymentPostProcessors) {
					if (paymentPostProcessor != null) {
						paymentPostProcessor.afterPaypalPayment(payment, donation);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failure iterating over payment post processors", e);
		}
	}
}
