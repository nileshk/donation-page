package com.nileshk;

import java.io.Serializable;

/**
 * Config that gets passed back to the client
 */
public class ClientConfig implements Serializable {

	private static final long serialVersionUID = -8244352411393600692L;

	private String publishableKey;
	private String organizationDisplayName;

	public String getPublishableKey() {
		return publishableKey;
	}

	public void setPublishableKey(String publishableKey) {
		this.publishableKey = publishableKey;
	}

	public String getOrganizationDisplayName() {
		return organizationDisplayName;
	}

	public void setOrganizationDisplayName(String organizationDisplayName) {
		this.organizationDisplayName = organizationDisplayName;
	}
}
