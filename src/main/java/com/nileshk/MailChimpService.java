package com.nileshk;

import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.MailchimpException;
import com.ecwid.maleorang.MailchimpObject;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod;
import com.ecwid.maleorang.method.v3_0.lists.members.MemberInfo;
import com.stripe.model.Charge;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static com.nileshk.PaymentContants.DONATION_PURPOSE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class MailChimpService implements PaymentPostProcessor {

	private static final Logger logger = Logger.getLogger(MailChimpService.class);

	@Value("${mailchimp.apiKey}")
	String apiKey;

	@Value("${mailchimp.listId}")
	String listId;

	@Value("${mailchimp.donationsOnly:true}")
	Boolean donationsOnly;

	@Value("${mailchimp.allFields:false}")
	Boolean allFields;

	@Override
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		if (isNotBlank(apiKey) && isNotBlank(listId)
				&& (donationsOnly == null || !donationsOnly || DONATION_PURPOSE.equals(donation.getPurpose()))) {

			try (MailchimpClient client = new MailchimpClient(apiKey)) {
				logger.debug("Saving email to MailChimp list: " + listId);
				EditMemberMethod.CreateOrUpdate method = new EditMemberMethod.CreateOrUpdate(listId, donation.getEmail());
				method.status = "subscribed";
				method.merge_fields = new MailchimpObject();
				method.merge_fields.mapping.put("FNAME", donation.firstName());
				method.merge_fields.mapping.put("LNAME", donation.lastName());
				if (allFields != null && allFields) {
					method.merge_fields.mapping.put("DATEPAID", donation.getPaymentDate());
					method.merge_fields.mapping.put("AMOUNTSTR", donation.getAmountString());
					method.merge_fields.mapping.put("AMOUNTCENT", donation.getAmountCents());
					method.merge_fields.mapping.put("FULLNAME", donation.getName());
					method.merge_fields.mapping.put("ADDRESS1", donation.getAddress1());
					method.merge_fields.mapping.put("ADDRESS2", donation.getAddress2());
					method.merge_fields.mapping.put("CITY", donation.getCity());
					method.merge_fields.mapping.put("STATE", donation.getState());
					method.merge_fields.mapping.put("ZIP", donation.getZip());
					method.merge_fields.mapping.put("COUNTRY", donation.getCountry());
					method.merge_fields.mapping.put("OCCUPATION", donation.getOccupation());
					method.merge_fields.mapping.put("PURPOSE", donation.getPurpose());
				}

				MemberInfo member = client.execute(method);
				logger.info("The user has been successfully subscribed: " + member);
			} catch (IOException e) {
				logger.error("IOException from MailchimpClient", e);
			} catch (MailchimpException e) {
				logger.error("MailchimpException on MailchimpClient.execute", e);
				// XXX Try with just default fields if allFields fails?
			}
		}
	}
}
