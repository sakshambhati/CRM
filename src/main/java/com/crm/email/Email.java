//package com.crm.email;
//
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.Properties;
//import jakarta.mail.Authenticator;
//import jakarta.mail.Message;
//import jakarta.mail.MessagingException;
//import jakarta.mail.Multipart;
//import jakarta.mail.PasswordAuthentication;
//import jakarta.mail.Session;
//import jakarta.mail.Transport;
//import jakarta.mail.internet.AddressException;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeBodyPart;
//import jakarta.mail.internet.MimeMessage;
//import jakarta.mail.internet.MimeMultipart;
//import org.springframework.beans.factory.annotation.Value;
//
//
//public class Email {
//
//    private static String externalEmail,  internalEmail;
//
//    @Value("${spring.mail.host}")
//    private static String host;
//    @Value("${spring.mail.port}")
//    private static String port;
//    @Value("${spring.mail.username}")
//    private static String userName;
//    @Value("${spring.mail.password}")
//    private static String password;
//
//
//    int flag=0;
//
//
//    private Email(String externalEmail,String password,String internalEmail,
//                     String host, String port){
//        this.externalEmail=externalEmail;
//        this.password=password;
//        this.internalEmail=internalEmail;
//        this.host=host;
//        this.port=port;
//    }
//
//
//
//    public static void sendEmailWithAttachments(String toAddress, String message, String[] attachFiles)
//            throws AddressException, MessagingException {
//
//        // sets SMTP server properties
//        Properties properties = new Properties();
//        properties.put("mail.smtp.host", host);
//        properties.put("mail.smtp.port", port);
//        properties.put("mail.smtp.auth", "true");
//        properties.put("mail.smtp.starttls.enable", "true");
//        properties.put("mail.user", userName);
//        properties.put("mail.password", password);
//
//        // creates a new session with an authenticator
//        Authenticator auth = new Authenticator() {
//            public PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(userName, password);
//            }
//        };
//        Session session = Session.getInstance(properties, auth);
//
//        // creates a new e-mail message
//        Message msg = new MimeMessage(session);
//
//        msg.setFrom(new InternetAddress(userName));
//        InternetAddress[] toAddresses = { new InternetAddress(toAddress) };
//        msg.setRecipients(Message.RecipientType.TO, toAddresses);
//        msg.setSubject("Leads");
//        msg.setSentDate(new Date());
//
//        // creates message part
//        MimeBodyPart messageBodyPart = new MimeBodyPart();
//        messageBodyPart.setContent(message, "text/html");
//
//        // creates multi-part
//        Multipart multipart = new MimeMultipart();
//        multipart.addBodyPart(messageBodyPart);
//
//        // adds attachments
//        if (attachFiles != null && attachFiles.length > 0) {
//            for (String filePath : attachFiles) {
//                if(filePath.length()<2) continue;
//                MimeBodyPart attachPart = new MimeBodyPart();
//
//                try {
//                    attachPart.attachFile(filePath);
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//
//                multipart.addBodyPart(attachPart);
//            }
//        }
//
//        // sets the multi-part as e-mail's content
//        msg.setContent(multipart);
//
//        // sends the e-mail
//        Transport.send(msg);
//
//    }
//
//
//
//
//}
//
//
