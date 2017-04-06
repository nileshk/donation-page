package com.nileshk;

import com.stripe.model.Charge;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class DatabaseService implements PaymentPostProcessor {

	private static final Logger logger = Logger.getLogger(DatabaseService.class);

	@Autowired
	JdbcTemplate jdbc;

	@Override
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		logger.debug("DatabaseService.postProcessPayment");
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
}
