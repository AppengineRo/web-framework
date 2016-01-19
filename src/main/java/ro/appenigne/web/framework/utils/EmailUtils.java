package ro.appenigne.web.framework.utils;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

public class EmailUtils {
	/**
	 * Trimite un email folosind APIul de la Google App Engine.
	 *
	 * @param subject Subiectul mesajului.
	 * @param body    Corpul mesajului.
	 * @throws javax.mail.MessagingException
	 * @throws javax.mail.internet.AddressException
	 */
	public static void sendEmail(String from, String to, String subject, String body) throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

		msg.setSubject(subject);
		msg.setText(body);
		Transport.send(msg);
	}

	public static void sendHTMLEmail(InternetAddress from, List<String> emails, String subject, String body, String htmlBody, InternetAddress replyTo) throws UnsupportedEncodingException, MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(from);
		for (String email : emails) {
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
		}
		InternetAddress[] replyToAdress = new InternetAddress[1];
		replyToAdress[0] = replyTo;
		msg.setReplyTo(replyToAdress);
		msg.setSubject(subject);
		msg.setText(body);

		Multipart mp = new MimeMultipart();

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlBody, "text/html");
		mp.addBodyPart(htmlPart);

		msg.setContent(mp);
		Transport.send(msg);
	}

	public static void sendHTMLEmail(InternetAddress from, InternetAddress to, String subject, String body, String htmlBody, InternetAddress replyTo) throws UnsupportedEncodingException, MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(from);
		msg.addRecipient(Message.RecipientType.TO, to);
		InternetAddress[] replyToAdress = new InternetAddress[1];
		replyToAdress[0] = replyTo;
		msg.setReplyTo(replyToAdress);
		msg.setSubject(subject);
		msg.setText(body);

		Multipart mp = new MimeMultipart();

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlBody, "text/html");
		mp.addBodyPart(htmlPart);

		msg.setContent(mp);
		Transport.send(msg);
	}

	public static void sendHTMLEmail(InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, String htmlBody, InternetAddress replyTo) throws UnsupportedEncodingException, MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(from);
		if (to != null) {
			for (InternetAddress email : to) {
				msg.addRecipient(Message.RecipientType.TO, email);
			}
		}
		if (cc != null) {
			for (InternetAddress email : cc) {
				msg.addRecipient(Message.RecipientType.CC, email);
			}
		}
		if (bcc != null) {
			for (InternetAddress email : bcc) {
				msg.addRecipient(Message.RecipientType.BCC, email);
			}
		}
		InternetAddress[] replyToAdress = new InternetAddress[1];
		replyToAdress[0] = replyTo;
		msg.setReplyTo(replyToAdress);
		msg.setSubject(subject);
		msg.setText(body);

		Multipart mp = new MimeMultipart();

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlBody, "text/html");
		mp.addBodyPart(htmlPart);

		msg.setContent(mp);
		Transport.send(msg);
	}

	public static void sendFileEmail(String email, String subject, String fileName, String fileType, String fileContent, String htmlBody) throws Exception {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("admin@cbn-expert.appspotmail.com", "Callcenter Admin"));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
		msg.setSubject(subject);
		msg.setText("");

		Multipart mp = new MimeMultipart();
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.setFileName(fileName);
		attachment.setContent(fileContent, fileType);
		mp.addBodyPart(attachment);
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlBody, "text/html");
		mp.addBodyPart(htmlPart);
		msg.setContent(mp);

		Transport.send(msg);
	}
	

}
