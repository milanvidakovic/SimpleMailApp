package com.example.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Profile;

@CrossOrigin(origins = "http://localhost:5500")
@RestController
@RequestMapping(value = "")
public class Controller {

	private static final String APPLICATION_NAME = "Simple mail app";
	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static Gmail client;


	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;
	TokenResponse response;
	Credential credential;

	@Value("${gmail.client.clientId}")
	private String clientId;

	@Value("${gmail.client.clientSecret}")
	private String clientSecret;

	@Value("${gmail.client.redirectUri}")
	private String redirectUri;

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public RedirectView googleConnectionStatus() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		if (flow == null) {
			Details web = new Details();
			web.setClientId(clientId);
			web.setClientSecret(clientSecret);
			clientSecrets = new GoogleClientSecrets().setWeb(web);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();

			List<String> scopes = new ArrayList<>();
			scopes.add(GmailScopes.GMAIL_COMPOSE);
			scopes.add(GmailScopes.GMAIL_INSERT);
			scopes.add(GmailScopes.GMAIL_LABELS);
			//scopes.add(GmailScopes.GMAIL_METADATA);
			scopes.add(GmailScopes.GMAIL_MODIFY);
			scopes.add(GmailScopes.GMAIL_READONLY);
			scopes.add(GmailScopes.GMAIL_SEND);
			scopes.add(GmailScopes.GMAIL_SETTINGS_BASIC);
			//scopes.addAll(GmailScopes.all());
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
					Collections.unmodifiableList(scopes)).build();
		}
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

		String build = authorizationUrl.build();

		return new RedirectView(build);
	}

	@RequestMapping(value = "/login/gmailCallback", method = RequestMethod.GET, params = "code",	
			produces = MediaType.APPLICATION_JSON_VALUE)
	public RedirectView callback(@RequestParam(value = "code") String code) throws URISyntaxException {

		RedirectView redirect = new RedirectView("http://localhost:5500/SimpleMailApp/client/index.html");
		redirect.addStaticAttribute("code", code);
		return redirect;

	}

	@RequestMapping(value = "/me", method = RequestMethod.GET, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getMe(@RequestParam(value = "code") String code) {

		JSONObject me = new JSONObject();

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");	

			response.getAccessToken();


			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();


			String userId = "me";
			Profile profile = client.users().getProfile(userId).execute();

			me.put("email", profile.getEmailAddress());



			/*GoogleCredential credential = new GoogleCredential().setAccessToken(response.getAccessToken());   
			Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();
			Userinfoplus userinfo = oauth2.userinfo().get().execute();
			System.out.println(userinfo.getName());
			System.out.println(userinfo.getFamilyName());
			System.out.println(userinfo.getPicture());
			userinfo.toPrettyString();*/

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(me.toString(), HttpStatus.OK);

	}



	@RequestMapping(value = "/labels", method = RequestMethod.GET, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getLabels(@RequestParam(value = "code") String code) {

		JSONArray labelArray = new JSONArray();

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";
			ListLabelsResponse labelsResponse = client.users().labels().list(userId).execute();

			for (Label l : labelsResponse.getLabels()) {
				Label label = client.users().labels().get(userId, l.getId()).execute();
				JSONObject labelJSON = new JSONObject();
				labelJSON.put("name", label.getName());
				if(label.getLabelListVisibility() != null) {
					labelJSON.put("labelListVisibility", label.getLabelListVisibility());
				} else {
					labelJSON.put("labelListVisibility", "labelShow");
				}
				if(label.getMessageListVisibility() != null) {
					labelJSON.put("messageListVisibility", label.getMessageListVisibility());
				} else {
					labelJSON.put("messageListVisibility", "show");
				}

				labelJSON.put("messagesTotal", label.getMessagesTotal());
				labelJSON.put("messagesUnread", label.getMessagesUnread());

				labelArray.put(labelJSON);
			}
			//System.out.println(labelArray);


		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(labelArray.toString(), HttpStatus.OK);

	}

	@RequestMapping(value = "/allMessages", method = RequestMethod.GET, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getAllMessages(@RequestParam(value = "code") String code) {

		JSONArray messageArray = new JSONArray();

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";
			ListMessagesResponse msgResponse = client.users().messages().list(userId).execute();

			for (Message msg : msgResponse.getMessages()) {

				Message message = client.users().messages().get(userId, msg.getId()).execute();
				
				JSONObject messageJSON = fetchMessages(message);
				messageArray.put(messageJSON);
			}


		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(messageArray.toString(), HttpStatus.OK);

	}

	@RequestMapping(value = "/messages", method = RequestMethod.GET, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getMessages(@RequestParam(value = "code") String code, @RequestParam(value = "label") String l) {

		JSONArray messageArray = new JSONArray();

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";
			
			List<String> labelIds = new ArrayList<>();
			labelIds.add(l);
			ListMessagesResponse msgResponse = client.users().messages().list(userId).setLabelIds(labelIds).execute();

			for (Message msg : msgResponse.getMessages()) {

				Message message = client.users().messages().get(userId, msg.getId()).execute();
				
				JSONObject messageJSON = fetchMessages(message);
				messageArray.put(messageJSON);
			}	


		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(messageArray.toString(), HttpStatus.OK);

	}

	private JSONObject fetchMessages(Message message) throws IOException, JSONException, ParseException {

		JSONObject messageJSON = new JSONObject();

		messageJSON.put("id", message.getId());
		messageJSON.put("snippet", message.getSnippet());
		JSONArray labels = new JSONArray();
		for (String label : message.getLabelIds()) {
			labels.put(label);
		}
		messageJSON.put("labels", labels);
		JSONArray headersArray = new JSONArray();
		for (MessagePartHeader header : message.getPayload().getHeaders()) {
			if(header.getName().equals("Date")) {
				SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ROOT);
				f.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date date =  f.parse(header.getValue());  
				DateFormat df = new SimpleDateFormat("dd.MM.yyyy kk:mm", Locale.ENGLISH);
				String s = df.format(date);
				header.setValue(s);
				headersArray.put(header);
			}
			if(header.getName().equals("Subject")) {
				headersArray.put(header);
			}
			if(header.getName().equals("From")) {
				headersArray.put(header);
			}

		}
		return messageJSON.put("headers", headersArray);


	}

	@RequestMapping(value = "/message", method = RequestMethod.GET, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getMessage(@RequestParam(value = "code") String code, @RequestParam(value = "id") String id) {

		JSONObject messageJSON = new JSONObject();
		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";


			Message message = client.users().messages().get(userId, id).setFormat("full").execute();

			messageJSON.put("id", message.getId());
			String content = StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getParts().get(0).getBody().getData()));

			messageJSON.put("content", content);
			messageJSON.put("snippet", message.getSnippet());
			JSONArray labels = new JSONArray();
			for (String label : message.getLabelIds()) {
				labels.put(label);
			}
			messageJSON.put("labelIds", labels);
			JSONArray headersArray = new JSONArray();
			for (MessagePartHeader header : message.getPayload().getHeaders()) {
				if(header.getName().equals("Date")) {
					SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ROOT);
					f.setTimeZone(TimeZone.getTimeZone("UTC"));
					Date date =  f.parse(header.getValue());  
					DateFormat df = new SimpleDateFormat("dd.MM.yyyy kk:mm", Locale.ENGLISH);
					String s = df.format(date);
					header.setValue(s);
					headersArray.put(header);
				}
				if(header.getName().equals("Subject")) {
					headersArray.put(header);
				}
				if(header.getName().equals("From")) {
					headersArray.put(header);
				}

			}
			messageJSON.put("headers", headersArray);
			//System.out.println(messageJSON);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(messageJSON.toString(), HttpStatus.OK);

	}

	/**
	 * Create a MimeMessage using the parameters provided.
	 *
	 * @param to email address of the receiver
	 * @param from email address of the sender, the mailbox account
	 * @param subject subject of the email
	 * @param bodyText body text of the email
	 * @return the MimeMessage to be used to send email
	 * @throws MessagingException
	 */
	public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {

		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		MimeMessage email = new MimeMessage(session);

		email.setFrom(new InternetAddress(from));
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
		email.setSubject(subject);
		email.setText(bodyText);

		return email;
	}

	/**
	 * Create a message from an email.
	 *
	 * @param emailContent Email to be set to raw of message
	 * @return a message containing a base64url encoded email
	 * @throws IOException
	 * @throws MessagingException
	 */
	public static Message createMessageWithEmail(MimeMessage emailContent)
			throws MessagingException, IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		emailContent.writeTo(buffer);
		byte[] bytes = buffer.toByteArray();
		String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
		Message message = new Message();
		message.setRaw(encodedEmail);

		return message;
	}

	@RequestMapping(value = "/send", method = RequestMethod.POST, params = "code",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> sendMessage(@RequestParam(value = "code") String code, @RequestBody String body) {		

		JSONObject messageJSON = new JSONObject();

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";

			JSONObject json = new JSONObject(body);
			String to = json.getString("to");
			String subject = json.getString("subject");
			String bodyText = json.getString("bodyText");

			MimeMessage emailContent = createEmail(to, userId, subject, bodyText);

			Message message = createMessageWithEmail(emailContent);
			Message mm = client.users().messages().send(userId, message).execute();

			messageJSON.put("message", mm);
			System.out.println(mm.toPrettyString());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(messageJSON.toString(), HttpStatus.OK);

	}

}
