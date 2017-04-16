package com.nileshk;

import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.MailBuilder;
import net.sargue.mailgun.Response;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Service
public class MailGunService implements PaymentPostProcessor {

	private static final Logger logger = Logger.getLogger(MailGunService.class);

	private Configuration configuration;
	private String siteTitle;
	private String displayName;
	private List<String> recipients;

	public MailGunService(
			@Value("${mailgun.domain:}") String domain,
			@Value("${mailgun.apiKey:}") String apiKey,
			@Value("${mailgun.fromName:}") String fromName,
			@Value("${mailgun.fromEmail:}") String fromEmail,
			@Value("${app.emailNotificationRecipients:}") String emailNotificationRecipients,
			@Value("${org.siteTitle:}") String siteTitle,
			@Value("${org.displayName:}") String displayName) {
		if (isNotBlank(domain) && isNotBlank(apiKey) && isNotBlank(fromName) && isNotBlank(fromEmail) && isNotBlank(emailNotificationRecipients)) {
			configuration = new Configuration()
					.domain(domain)
					.apiKey(apiKey)
					.from(fromName, fromEmail);
			recipients = Arrays.asList(emailNotificationRecipients.split(","));
			this.siteTitle = siteTitle;
			this.displayName = displayName;
		}
	}

	@Override
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		if (configuration != null) {
			sendMail(donation, "Stripe", charge.getId());
		}
	}

	@Override
	public void afterPaypalPayment(Payment payment, Donation donation) {
		if (configuration != null) {
			sendMail(donation, "PayPal", payment.getId());
		}
	}

	private void sendMail(Donation donation, String paymentType, String id) {
		if (configuration != null) {
			logger.info("Sending notification e-mails");
			String subject = String.format("Donation: %s from %s to %s", donation.getAmountString(), donation.getName(), siteTitle());
			MailBuilder builder = Mail.using(configuration)
					.body()
					.h2("Donation Received")
					.p(subject)
					.table()
					.row("Name", trimToEmpty(donation.getName()))
					.row("Amount", trimToEmpty(donation.getAmountString()))
					.row("Email", trimToEmpty(donation.getEmail()))
					.row("Address1", trimToEmpty(donation.getAddress1()))
					.row("Address2", trimToEmpty(donation.getAddress2()))
					.row("City", trimToEmpty(donation.getCity()))
					.row("State", trimToEmpty(donation.getState()))
					.row("Zip", trimToEmpty(donation.getZip()))
					.row("Country", trimToEmpty(donation.getCountry()))
					.row("Occupation", trimToEmpty(donation.getOccupation()))
					.row("Purpose", trimToEmpty(donation.getPurpose()))
					.row(paymentType + " ID", trimToEmpty(id))
					.row("Date", trimToEmpty(donation.getPaymentDate().toString()))
					.end()
					.mail();
			for (String recipient : recipients) {
				builder.to(recipient);
			}
			Response response = builder
					.subject(subject)
					.build()
					.send();
			if (response.isOk()) {
				logger.info("Notifications sent successfully: " + response.responseMessage());
			} else {
				logger.error("Notifications failed: " + response.responseMessage());
			}
		}
	}

	private String siteTitle() {
		return isNotBlank(siteTitle) ? siteTitle : displayName;
	}

}
