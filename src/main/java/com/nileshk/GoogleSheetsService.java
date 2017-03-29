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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

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

	@Async
	public void postProcessPayment(Map<String, Object> map, Charge charge) {
		logger.info("GoogleSheetsService.postProcessPayment");
		if (isNotBlank(googleSheetId)) {
			try {
				Sheets service = getSheetsService();
				String spreadsheetId = googleSheetId;
				addLog(map, charge, service, spreadsheetId);
				addTransaction(map, service, spreadsheetId);

			} catch (IOException e) { // TODO More info on exception
				logger.error("IOException adding to Google spreadsheet", e);
			} catch (Exception e) {
				logger.error("Error occurred adding to Google spreadsheet", e);
			}
		} else {
			logger.debug("Google Sheet not configured");
		}
	}

	private void addTransaction(Map<String, Object> map, Sheets service, String spreadsheetId) throws IOException {
		ValueRange contentToAppend = new ValueRange();
		contentToAppend.setMajorDimension("ROWS");
		List<List<Object>> list = new ArrayList<>();
		List<Object> e = new ArrayList<>();
		Map<String, Object> token = (Map<String, Object>) map.get("token");
		String name = "";
		String address1 = "";
		String address2 = "";
		String city = "";
		String state = "";
		String zip = "";
		String country = "";
		String email = "";
		if (token != null) {
			if (token.containsKey("shippingContact")) {
				Map<String, Object> shippingMap = (Map<String, Object>) token.get("shippingContact");
				name = (shippingMap.getOrDefault("givenName", "") + " " + shippingMap.getOrDefault("familyName", "")).trim();
				if (shippingMap.containsKey("addressLines")) {
					Object addressLinesObj = shippingMap.get("addressLines");
					if (addressLinesObj instanceof String[]) {
						logger.info("Address is an array");
						String[] addressLines = (String[]) addressLinesObj;
						if (addressLines.length > 0) {
							address1 = addressLines[0];
						}
						if (addressLines.length > 1) {
							address2 = addressLines[1];
						}
					} else if (addressLinesObj instanceof List) {
						logger.info("Address is a list");
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
					country = (String) shippingMap.getOrDefault("countryCode", "");
					email = (String) shippingMap.getOrDefault("emailAddress", "");
				}
			}
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

		e.add(dateFormat.format(new Date()));
		e.add(((Integer) map.getOrDefault("amount", 0)) / 100);
		e.add(trimToEmpty(name));
		e.add(trimToEmpty(address1));
		e.add(trimToEmpty(address2));
		e.add(trimToEmpty(city));
		e.add(trimToEmpty(state));
		e.add(trimToEmpty(zip));
		e.add(trimToEmpty(country));
		e.add(trimToEmpty((String) map.getOrDefault("occupation", "")));
		e.add(trimToEmpty(email));
		list.add(e);
		contentToAppend.setValues(list);
		AppendValuesResponse appendValuesResponse = service.spreadsheets().values()
				.append(spreadsheetId, "Transactions!A:K", contentToAppend)
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
