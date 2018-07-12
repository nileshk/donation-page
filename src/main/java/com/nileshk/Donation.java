package com.nileshk;

import com.paypal.api.payments.PayerInfo;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.ShippingAddress;
import com.paypal.api.payments.Transaction;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Donation implements Serializable {

	private static final long serialVersionUID = -6941832796419479108L;

	private Date paymentDate;
	private Integer amountCents;
	private String amountString;
	private String name;
	private String firstName;
	private String lastName;
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String country;
	private String email;
	private String occupation;
	private String purpose;

	private Map<String, Object> map;

	public Donation() {
	}

	public Donation(Map<String, Object> map) {
		this.map = map;
		paymentDate = new Date();

		if (map.containsKey("applePayResult")) {
			Map<String, Object> applePayResult = (Map<String, Object>) map.get("applePayResult");
			if (applePayResult.containsKey("shippingContact")) {
				Map<String, Object> shippingMap = (Map<String, Object>) applePayResult.get("shippingContact");
				name = (shippingMap.getOrDefault("givenName", "") + " " + shippingMap.getOrDefault("familyName", "")).trim();
				if (shippingMap.containsKey("addressLines")) {
					Object addressLinesObj = shippingMap.get("addressLines");
					if (addressLinesObj instanceof String[]) {
						String[] addressLines = (String[]) addressLinesObj;
						if (addressLines.length > 0) {
							address1 = addressLines[0];
						}
						if (addressLines.length > 1) {
							address2 = addressLines[1];
						}
					} else if (addressLinesObj instanceof List) {
						List<String> addressLines = (List<String>) addressLinesObj;
						if (addressLines.size() > 0) {
							address1 = addressLines.get(0);
						}
						if (addressLines.size() > 1) {
							address2 = addressLines.get(1);
						}
					}
					city = (String) shippingMap.getOrDefault("locality", "");
					state = (String) shippingMap.getOrDefault("administrativeArea", "");
					zip = (String) shippingMap.getOrDefault("postalCode", "");
					country = (String) shippingMap.getOrDefault("country", shippingMap.getOrDefault("countryCode", ""));
					email = (String) shippingMap.getOrDefault("emailAddress", "");
				}
			}
		}
		Map<String, Object> token = (Map<String, Object>) map.get("token");
		if (token != null) {
			Map<String, Object> card = (Map<String, Object>) token.get("card");
			if (card != null) {
				if (isBlank(name)) {
					name = (String) card.getOrDefault("name", "");
				}
				if (isBlank(address1)) {
					address1 = (String) card.getOrDefault("address_line1", "");
				}
				if (isBlank(address2)) {
					address2 = (String) card.getOrDefault("address_line2", "");
				}
				if (isBlank(city)) {
					city = (String) card.getOrDefault("address_city", "");
				}
				if (isBlank(state)) {
					state = (String) card.getOrDefault("address_state", "");
				}
				if (isBlank(zip)) {
					zip = (String) card.getOrDefault("address_zip", "");
				}
				if (isBlank(country)) {
					country = (String) card.getOrDefault("address_country", "");
				}
				if (isBlank(country)) {
					country = (String) card.getOrDefault("country", "");
				}
				if (isBlank(email)) {
					email = (String) card.getOrDefault("email", "");
				}
			}
		}

		if (isBlank(email)) {
			email = (String) map.getOrDefault("description", "");
		}

		amountCents = (Integer) map.getOrDefault("amount", 0);
		amountString = "$" + String.valueOf(amountCents / 100);

		occupation = (String) map.getOrDefault("occupation", "");
		purpose = (String) map.getOrDefault("pagePurpose", "unknown");
	}

	public Donation(Payment payment, String purpose, String occupation) {
		paymentDate = new Date();
		Transaction transaction = payment.getTransactions().get(0);
		amountString = "$" + transaction.getAmount().getTotal();
		amountCents = Double.valueOf(Double.valueOf(transaction.getAmount().getTotal()) * 100).intValue();
		PayerInfo payerInfo = payment.getPayer().getPayerInfo();
		name = payerInfo.getFirstName() + " " + payerInfo.getLastName();
		firstName = payerInfo.getFirstName();
		lastName = payerInfo.getLastName();
		ShippingAddress shippingAddress = transaction.getItemList().getShippingAddress();
		address1 = shippingAddress.getLine1();
		address2 = shippingAddress.getLine2();
		city = shippingAddress.getCity();
		state = shippingAddress.getState();
		zip = shippingAddress.getPostalCode();
		country = shippingAddress.getCountryCode();
		email = payerInfo.getEmail();
		this.occupation = occupation;
		this.purpose = purpose;
	}

	public String firstName() {
		if (isNotBlank(firstName)) {
			return firstName;
		}
		if (isNotBlank(name)) {
			String[] split = name.split("\\s+");
			if (split != null && split.length > 0) {
				return split[0];
			}
		}
		return "";
	}

	public String lastName() {
		if (isNotBlank(lastName)) {
			return lastName;
		}
		if (isNotBlank(name)) {
			String[] split = name.split("\\s+");
			if (split != null && split.length > 0) {
				return split[split.length - 1];
			}
		}
		return "";
	}

	public String middleName() {
		if (isNotBlank(name)) {
			String[] split = name.split("\\s+");
			if (split != null && split.length == 3) {
				return split[1];
			}
		}
		return "";
	}

	public Date getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}

	public Integer getAmountCents() {
		return amountCents;
	}

	public void setAmountCents(Integer amountCents) {
		this.amountCents = amountCents;
	}

	public String getAmountString() {
		return amountString;
	}

	public void setAmountString(String amountString) {
		this.amountString = amountString;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstName() {
		return firstName();
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName();
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
}
