package com.nileshk;

import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class DatabaseService implements PaymentPostProcessor {

	private static final Logger logger = LogManager.getLogger(DatabaseService.class);

	@Autowired
	JdbcTemplate jdbc;

	@Override
	@Async
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		addDonation(donation);
		addPaymentLog(donation, charge.toJson(), map.toString(), "Stripe");
	}

	@Override
	@Async
	public void afterPaypalPayment(Payment payment, Donation donation) {
		addDonation(donation);
		addPaymentLog(donation, payment.toJSON(), "", "PayPal");
	}

	private void addDonation(Donation donation) {
		logger.debug("DatabaseService.addDonation");
		jdbc.update(
				"INSERT INTO donations (payment_date, amount_cents, amount_string, name, address1, address2, city, state, zip, country, email, occupation, purpose) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				donation.getPaymentDate(),
				donation.getAmountCents(),
				donation.getAmountString(),
				donation.getName(),
				donation.getAddress1(),
				donation.getAddress2(),
				donation.getCity(),
				donation.getState(),
				donation.getZip(),
				donation.getCountry(),
				donation.getEmail(),
				donation.getOccupation(),
				donation.getPurpose());
	}

	private void addPaymentLog(Donation donation, String jsonResponse, String parameters, String paymentProcessor) {
		logger.debug("DatabaseService.addPaymentLog");
		jdbc.update(
				"INSERT INTO payment_log (payment_date, json_response, parameters, amount_string, email, occupation, purpose, payment_processor) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				donation.getPaymentDate(),
				jsonResponse,
				parameters,
				donation.getAmountString(),
				donation.getEmail(),
				donation.getOccupation(),
				donation.getPurpose(),
				paymentProcessor
		);
	}
}
