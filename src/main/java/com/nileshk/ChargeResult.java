package com.nileshk;

import com.stripe.model.Charge;

import java.io.Serializable;

public class ChargeResult implements Serializable {

	private static final long serialVersionUID = 1397303362248480911L;

	private Long amount;
	private String email;
	private boolean error = false;
	private String errorMessage;

	public ChargeResult() {
	}

	public ChargeResult(Charge charge) {
		if (charge != null) {
			amount = charge.getAmount();
			email = charge.getDescription();
		}
	}

	public static ChargeResult error(String errorMessage) {
		ChargeResult result = new ChargeResult();
		result.setError(true);
		result.setErrorMessage(errorMessage);
		return result;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
