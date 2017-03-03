package com.nileshk;

import com.stripe.model.Charge;

import java.io.Serializable;

public class ChargeResult implements Serializable {

	private static final long serialVersionUID = 1666695160039191687L;

	private Long amount;
	private String email;

	public ChargeResult() {
	}

	public ChargeResult(Charge charge) {
		if (charge != null) {
			amount = charge.getAmount();
			email = charge.getDescription();
		}
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
}
