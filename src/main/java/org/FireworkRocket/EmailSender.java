package org.FireworkRocket;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmailSender {
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService emailExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final AtomicBoolean emailSentToday = new AtomicBoolean(false);
    private static String smtpHost;
    private static String smtpPort;
    private static String smtpUsername;
    private static String smtpPassword;

    static {
        // 添加关闭钩子以正确关闭执行器
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            emailExecutor.shutdown();
            try {
                if (!emailExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    emailExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                emailExecutor.shutdownNow();
            }
        }));
    }

    public static void setSmtpConfig(String host, String port, String username, String password) {
        smtpHost = host;
        smtpPort = port;
        smtpUsername = username;
        smtpPassword = password;
    }

    public static void sendEmails(List<String> recipients, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", smtpHost);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        for (String recipient : recipients) {
            emailExecutor.submit(() -> {
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(smtpUsername));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                    message.setSubject(subject);
                    message.setContent(body, "text/html; charset=utf-8");

                    Transport.send(message);
                    System.out.println("Email sent successfully to " + recipient);
                } catch (MessagingException e) {
                    System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        emailSentToday.set(true); // Set the flag after attempting to send emails to all recipients
    }
}