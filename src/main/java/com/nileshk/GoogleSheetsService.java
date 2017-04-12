package com.nileshk;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.paypal.api.payments.Payment;
import com.stripe.model.Charge;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Writes transaction info to a Google Sheet
 */
@Service
public class GoogleSheetsService implements PaymentPostProcessor {

	private static final Logger logger = Logger.getLogger(GoogleSheetsService.class);

	/**
	 * Application name.
	 */
	private static final String APPLICATION_NAME = "DonationsPage"; // TODO Get this dynamically from app

	@Value("${app.googleSheetId:}")
	String googleSheetId;

	/**
	 * Directory to store user credentials for this application.
	 */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(// TODO make this come from Spring boot config
			System.getProperty("user.home"), ".credentials/sheets.googleapis.com-donations");

	private static FileDataStoreFactory DATA_STORE_FACTORY;

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static HttpTransport HTTP_TRANSPORT;

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	/**
	 * If modifying these scopes, delete your previously saved credentials
	 * at ~/.credentials/sheets.googleapis.com-donations
	 */
	private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			logger.fatal("Error retrieving  Google OAuth2 data store");
			//System.exit(1);
		}
	}

	@Override
	@Async
	public void postProcessPayment(Map<String, Object> map, Charge charge, Donation donation) {
		logger.info("GoogleSheetsService.postProcessPayment");
		afterPayment(map, charge, null, donation);
	}

	@Override
	@Async
	public void afterPaypalPayment(Payment payment, Donation donation) {
		afterPayment(null, null, payment, donation);
	}

	public void afterPayment(Map<String, Object> map, Charge charge, Payment payment, Donation donation) {
		logger.info("GoogleSheetsService.afterPayment");
		if (isNotBlank(googleSheetId)) {
			try {
				Sheets service = getSheetsService();
				String spreadsheetId = googleSheetId;
				if (map != null && charge != null) {
					addLog(map, charge, service, spreadsheetId);
				} else if (payment != null) {
					addPayPalLog(payment, donation, service, spreadsheetId);
				}
				addTransaction(donation, service, spreadsheetId);

			} catch (IOException e) { // TODO More info on exception
				logger.error("IOException adding to Google spreadsheet", e);
			} catch (Exception e) {
				logger.error("Error occurred adding to Google spreadsheet", e);
			}
		} else {
			logger.debug("Google Sheet not configured");
		}
	}

	private void addTransaction(Donation donation, Sheets service, String spreadsheetId) throws IOException {
		ValueRange contentToAppend = new ValueRange();
		contentToAppend.setMajorDimension("ROWS");
		List<List<Object>> list = new ArrayList<>();
		List<Object> e = new ArrayList<>();

		e.add(dateFormat.format(donation.getPaymentDate()));
		e.add(donation.getAmountCents() / 100);
		e.add(trimToEmpty(donation.getName()));
		e.add(trimToEmpty(donation.getAddress1()));
		e.add(trimToEmpty(donation.getAddress2()));
		e.add(trimToEmpty(donation.getCity()));
		e.add(trimToEmpty(donation.getState()));
		e.add(trimToEmpty(donation.getZip()));
		e.add(trimToEmpty(donation.getCountry()));
		e.add(trimToEmpty(donation.getOccupation()));
		e.add(trimToEmpty(donation.getEmail()));
		e.add(trimToEmpty(donation.getPurpose()));
		list.add(e);
		contentToAppend.setValues(list);
		AppendValuesResponse appendValuesResponse = service.spreadsheets().values()
				.append(spreadsheetId, "Transactions!A:L", contentToAppend)
				.setValueInputOption("RAW")
				.execute();
		logger.info(appendValuesResponse.toPrettyString());
	}

	private void addLog(Map<String, Object> map, Charge charge, Sheets service, String spreadsheetId) throws IOException {
		ValueRange contentToAppend = new ValueRange();
		contentToAppend.setMajorDimension("ROWS");
		List<List<Object>> list = new ArrayList<>();
		List<Object> e = new ArrayList<>();
		e.add(dateFormat.format(new Date()));
		e.add(String.valueOf(map.getOrDefault("amount", "0")));
		e.add(map.getOrDefault("currency", ""));
		e.add(map.getOrDefault("description", ""));
		e.add(map.getOrDefault("occupation", ""));
		e.add(map.getOrDefault("source", ""));
		e.add(map.getOrDefault("token", "").toString());
		e.add(map.getOrDefault("logData", ""));
		e.add(map.toString());
		e.add(charge.toJson());
		list.add(e);
		contentToAppend.setValues(list);
		AppendValuesResponse appendValuesResponse = service.spreadsheets().values()
				.append(spreadsheetId, "Log!A:J", contentToAppend)
				.setValueInputOption("RAW")
				.execute();
		logger.info(appendValuesResponse.toPrettyString());
	}


	private void addPayPalLog(Payment payment, Donation donation, Sheets service, String spreadsheetId) throws IOException {
		ValueRange contentToAppend = new ValueRange();
		contentToAppend.setMajorDimension("ROWS");
		List<List<Object>> list = new ArrayList<>();
		List<Object> e = new ArrayList<>();
		e.add(trimToEmpty(dateFormat.format(donation.getPaymentDate())));
		e.add(trimToEmpty(payment.toJSON()));
		e.add(trimToEmpty(donation.getAmountString()));
		e.add(trimToEmpty(donation.getEmail()));
		e.add(trimToEmpty(donation.getOccupation()));
		e.add(trimToEmpty(donation.getPurpose()));
		list.add(e);
		contentToAppend.setValues(list);
		AppendValuesResponse appendValuesResponse = service.spreadsheets().values()
				.append(spreadsheetId, "PayPal!A:F", contentToAppend)
				.setValueInputOption("RAW")
				.execute();
		logger.info(appendValuesResponse.toPrettyString());
	}

	/**
	 * Creates an authorized Credential object.
	 *
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	private static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in = GoogleSheetsService.class.getResourceAsStream("/client_secret.json");
		GoogleClientSecrets clientSecrets =
				GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		DataStore<Serializable> dataStore = DATA_STORE_FACTORY.getDataStore("StoredCredential");
		if (dataStore != null && dataStore.containsKey("user") && dataStore.get("user") instanceof StoredCredential) {
			StoredCredential storedCredential = (StoredCredential) dataStore.get("user");
			GoogleCredential googleCredential = new GoogleCredential.Builder()
					.setTransport(HTTP_TRANSPORT)
					.setJsonFactory(JSON_FACTORY)
					.setClientSecrets(clientSecrets)
					.build();
			googleCredential.setAccessToken(storedCredential.getAccessToken());
			googleCredential.setRefreshToken(storedCredential.getRefreshToken());
			googleCredential.setExpirationTimeMilliseconds(storedCredential.getExpirationTimeMilliseconds());
			logger.debug("Credentials found in data store and returned");
			return googleCredential;
		} /* TODO Generation of credentials: else {

			// Build flow and trigger user authorization request.
			GoogleAuthorizationCodeFlow flow =
					new GoogleAuthorizationCodeFlow.Builder(
							HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
							.setDataStoreFactory(DATA_STORE_FACTORY)
							.setAccessType("offline")
							.build();
			Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
			logger.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
			return credential;
		}*/
		return null;
	}

	/**
	 * Build and return an authorized Sheets API client service.
	 *
	 * @return an authorized Sheets API client service
	 * @throws IOException
	 */
	private static Sheets getSheetsService() throws IOException {
		Credential credential = authorize();
		return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}



}
