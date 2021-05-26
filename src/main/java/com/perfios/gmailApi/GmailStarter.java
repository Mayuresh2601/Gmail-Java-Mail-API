package com.perfios.gmailApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import io.restassured.path.json.JsonPath;

public class GmailStarter {

	private static final String APPLICATION_NAME = "Welcome to Perfios Insights!";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String USER_ID = "me";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

	/**
	 * Variable: Contains Credentials of )auth 2.0, client Id, Client Secret Key
	 */
	private static final String CREDENTIALS_FILE_PATH = System.getProperty("user.dir") + 
			File.separator + "src" + 
			File.separator + "main" + 
			File.separator + "resources" + 
			File.separator + "credentials" + 
			File.separator + "credentials_new.json";

	private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.dir") + 
			File.separator + "src" + 
			File.separator + "main" + 
			File.separator + "resources" + 
			File.separator + "credentials";

	/**
	 * Method: Authenticating Oauth 2.0
	 * @param HTTP_TRANSPORT
	 * @return http URI
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

		// Load client secrets.
		InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(9998).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/**method: Accessing Gmail Service
	 * @return End Point
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public static Gmail getService() throws IOException, GeneralSecurityException {

		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		return service;
	}

	/**Method: Get Email Matching Query 
	 * @param service
	 * @param userId
	 * @param query
	 * @return Data
	 * @throws IOException
	 */
	public static List<Message> listMessagesMatchingQuery(Gmail service, String userId, String query)
			throws IOException {
		ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();
		List<Message> messages = new ArrayList<Message>();
		while (response.getMessages() != null) {
			messages.addAll(response.getMessages());
			if (response.getNextPageToken() != null) {
				String pageToken = response.getNextPageToken();
				response = service.users().messages().list(userId).setQ(query).setPageToken(pageToken).execute();
			} else {
				break;
			}
		}
		return messages;
	}

	public static Message getMessage(Gmail service, String userId, List<Message> messages, int index)
			throws IOException {
		Message message = service.users().messages().get(userId, messages.get(index).getId()).execute();
		return message;
	}

	/**Method: Filter Gmail with Subject and hyperlink present in email
	 * @param query
	 * @return Data
	 */
	public static HashMap<String, String> getGmailData(String query) {
		try {
			Gmail service = getService();
			List<Message> messages = listMessagesMatchingQuery(service, USER_ID, query);
			Message message = getMessage(service, USER_ID, messages, 0);
			JsonPath jp = new JsonPath(message.toString());
			String subject = jp.getString("payload.headers.find { it.name == 'Subject' }.value");
			String body = new String(Base64.getDecoder().decode(jp.getString("payload.parts[0].body.data")));
			String link = null;
			String arr[] = body.split("\n");
			for (String s : arr) {
				s = s.trim();
				if (s.startsWith("http") || s.startsWith("https")) {
					link = s.trim();
				}
			}
			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put("subject", subject);
			hm.put("body", body);
			hm.put("link", link);
			return hm;
		} catch (Exception e) {
			System.out.println("email not found....");
			throw new RuntimeException(e);
		}
	}

	/**Method: Get total count of emails present in your Mail Id
	 * @return Count (int)
	 */
	public static int getTotalCountOfMails() {
		int size;
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
			List<Thread> threads = service.users().threads().list("me").execute().getThreads();
			size = threads.size();
		} catch (Exception e) {
			System.out.println("Exception log " + e);
			size = -1;
		}
		return size;
	}

	/**Method: To check Email Present or Not
	 * @param messageTitle
	 * @return boolean
	 */
	public static boolean isMailExist(String messageTitle) {
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME)
					.build();
			
			ListMessagesResponse response = service.users().messages()
					.list("me")
					.setQ("subject:" + messageTitle)
					.execute();
			
			List<Message> messages = getMessages(response);
			return messages.size() != 0;
		} catch (Exception e) {
			System.out.println("Exception log" + e);
			return false;
		}
	}

	/**Method: Get Message
	 * @param response
	 * @return
	 */
	private static List<Message> getMessages(ListMessagesResponse response) {
		List<Message> messages = new ArrayList<Message>();
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME)
					.build();
			
			while (response.getMessages() != null) {
				
				messages.addAll(response.getMessages());
				if (response.getNextPageToken() != null) {
					
					String pageToken = response.getNextPageToken();
					response = service.users().messages().list(USER_ID).setPageToken(pageToken).execute();
				} else {
					
					break;
				}
			}
			return messages;
			
		} catch (Exception e) {
			System.out.println("Exception log " + e);
			return messages;
		}
	}

	@SuppressWarnings({ "unused", "unchecked" })
	public static void main(String[] args) throws GeneralSecurityException, IOException {

		NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		getCredentials(HTTP_TRANSPORT);
		getService();

		List<Message> res = listMessagesMatchingQuery(getService(), USER_ID, APPLICATION_NAME);
		System.out.println("Query Matches: " + res);
		                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
		//Converting List into String
		String delim = ",";
		StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < res.size() - 1)
        {
            sb.append(res.get(i));
            sb.append(delim);
            i++;
        }
        sb.append(res.get(i));
        String response = sb.toString();
        System.out.println("\nString: "+response);
		
        Map<String, Object> firstKey = new ObjectMapper().readValue(response, HashMap.class);
        System.out.println("\nFirst Pattern matching Value: "+firstKey);
        String str = firstKey.toString();

		System.out.println("=================");
		System.out.println("Total count of emails is :" + getTotalCountOfMails());

		System.out.println("=================");
		boolean exist = isMailExist(APPLICATION_NAME);
		System.out.println("title exist or not: " + exist);

	}
}