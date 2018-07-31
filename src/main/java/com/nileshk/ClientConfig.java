package com.nileshk;

import java.io.Serializable;

/**
 * Config that gets passed back to the client
 */
public class ClientConfig implements Serializable {

	private static final long serialVersionUID = 7666767769819506242L;

	private String mainPageUrl;
	private String siteTitle;
	private String appPreviewImageUrl = "";
	private String publishableKey;
	private String organizationDisplayName;
	private Boolean applyPayEnabled = true;
	private Boolean clientLoggingEnabled;
	private Boolean collectOccupationEnabled;
	private Integer collectOccupationThreshold;
	private Integer donationLimit;
	private Boolean paypalEnabled;
	private Boolean paypalSandbox;
	private String emailSignupUrl;
	private String vcsBuildId;

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

	public Boolean getApplyPayEnabled() {
		return applyPayEnabled;
	}

	public void setApplyPayEnabled(Boolean applyPayEnabled) {
		this.applyPayEnabled = applyPayEnabled;
	}

	public Boolean getClientLoggingEnabled() {
		return clientLoggingEnabled;
	}

	public void setClientLoggingEnabled(Boolean clientLoggingEnabled) {
		this.clientLoggingEnabled = clientLoggingEnabled;
	}

	public Boolean getCollectOccupationEnabled() {
		return collectOccupationEnabled;
	}

	public void setCollectOccupationEnabled(Boolean collectOccupationEnabled) {
		this.collectOccupationEnabled = collectOccupationEnabled;
	}

	public Integer getCollectOccupationThreshold() {
		return collectOccupationThreshold;
	}

	public void setCollectOccupationThreshold(Integer collectOccupationThreshold) {
		this.collectOccupationThreshold = collectOccupationThreshold;
	}

	public Integer getDonationLimit() {
		return donationLimit;
	}

	public void setDonationLimit(Integer donationLimit) {
		this.donationLimit = donationLimit;
	}

	public Boolean getPaypalEnabled() {
		return paypalEnabled;
	}

	public void setPaypalEnabled(Boolean paypalEnabled) {
		this.paypalEnabled = paypalEnabled;
	}

	public Boolean getPaypalSandbox() {
		return paypalSandbox;
	}

	public void setPaypalSandbox(Boolean paypalSandbox) {
		this.paypalSandbox = paypalSandbox;
	}

	public String getMainPageUrl() {
		return mainPageUrl;
	}

	public void setMainPageUrl(String mainPageUrl) {
		this.mainPageUrl = mainPageUrl;
	}

	public String getSiteTitle() {
		return siteTitle;
	}

	public void setSiteTitle(String siteTitle) {
		this.siteTitle = siteTitle;
	}

	public String getAppPreviewImageUrl() {
		return appPreviewImageUrl;
	}

	public void setAppPreviewImageUrl(String appPreviewImageUrl) {
		this.appPreviewImageUrl = appPreviewImageUrl;
	}

	public String getEmailSignupUrl() {
		return emailSignupUrl;
	}

	public void setEmailSignupUrl(String emailSignupUrl) {
		this.emailSignupUrl = emailSignupUrl;
	}

	public String getVcsBuildId() {
		return vcsBuildId;
	}

	public void setVcsBuildId(String vcsBuildId) {
		this.vcsBuildId = vcsBuildId;
	}
}
