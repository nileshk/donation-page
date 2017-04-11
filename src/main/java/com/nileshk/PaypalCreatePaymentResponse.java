package com.nileshk;

import java.io.Serializable;

public class PaypalCreatePaymentResponse implements Serializable {

	private static final long serialVersionUID = 326476015159175334L;
	private String payToken;

	public PaypalCreatePaymentResponse(String payToken) {
		this.payToken = payToken;
	}

	public String getPayToken() {
		return payToken;
	}

	public void setPayToken(String payToken) {
		this.payToken = payToken;
	}
}
