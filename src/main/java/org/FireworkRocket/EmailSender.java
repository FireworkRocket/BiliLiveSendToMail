package org.FireworkRocket;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;

import java.util.*;
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
    private static String imapHost;
    private static String imapPort;
    private static String imapUsername;
    private static String imapPassword;
    private static Set<String> emailList = new HashSet<>();

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

    public static void setImapConfig(String host, String port, String username, String password) {
        imapHost = host;
        imapPort = port;
        imapUsername = username;
        imapPassword = password;
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

    public static void checkEmailReplies() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.put("mail.imap.ssl.enable", "true");

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(imapUsername, imapPassword);
                }
            });

            Store store = session.getStore("imaps");
            store.connect(imapHost, imapUsername, imapPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // 搜索未读邮件
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                if (message.getSubject().contains("TD") || message.getContent().toString().contains("TD")) {
                    Address[] fromAddresses = message.getFrom();
                    for (Address address : fromAddresses) {
                        String email = ((InternetAddress) address).getAddress();
                        emailList.remove(email);
                        System.out.println("Removed " + email + " from email list.");
                        sendReceipt(email); // 发送回执邮件
                    }
                }
                // 标记邮件为已读
                message.setFlag(Flags.Flag.SEEN, true);
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("Error checking email replies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendReceipt(String recipient) {
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

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUsername));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("退订确认");
            message.setText("您已成功退订我们的邮件通知服务。");

            Transport.send(message);
            System.out.println("Receipt email sent successfully to " + recipient);
        } catch (MessagingException e) {
            System.err.println("Failed to send receipt email to " + recipient + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}