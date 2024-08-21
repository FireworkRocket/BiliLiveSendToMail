package org.FireworkRocket;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    public static Set<String> DeletedMailList = new HashSet<>();

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

    public static void checkEmailReplies(Boolean isClose) {
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
            if (!store.isConnected()){
                store.connect(imapHost, imapUsername, imapPassword);
            }

            Folder inbox = store.getFolder("INBOX");
            if (!inbox.isOpen()) {
                inbox.open(Folder.READ_WRITE);
            }

            if (isClose){
                inbox.close(false);
                store.close();
                return;
            }

            // 搜索未读邮件
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                if (message.getSubject().contains("TD") || message.getContent().toString().contains("TD")) {
                    Address[] fromAddresses = message.getFrom();
                    for (Address address : fromAddresses) {
                        String email = ((InternetAddress) address).getAddress();
                        DeletedMailList.add(email);
                        removeEmailFromConfig(email);
                        Main.emailList.remove(email);
                        System.out.println("Removed " + email + " from email list.");
                        sendReceipt(email); // 发送回执邮件
                    }
                }
                // 标记邮件为已读
                message.setFlag(Flags.Flag.SEEN, true);
            }
        } catch (Exception e) {
            System.err.println("Error checking email replies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void removeEmailFromConfig(String email) {
        try {
            Properties properties = new Properties();
            String configFilePath = "config.properties";
            File configFile = new File(configFilePath);

            // 加载配置文件
            try (FileInputStream input = new FileInputStream(configFile)) {
                properties.load(input);
            }

            // 获取 EmailList 属性并删除指定的邮箱地址
            String emailListStr = properties.getProperty("EmailList");
            if (emailListStr != null && !emailListStr.isEmpty()) {
                List<String> emailList = new ArrayList<>(Arrays.asList(emailListStr.split(",")));
                emailList.remove(email);

                // 更新 EmailList 属性
                properties.setProperty("EmailList", String.join(",", emailList));
            }

            // 将更新后的属性写回配置文件，同时保留原始排版和注释
            List<String> lines = Files.readAllLines(Paths.get(configFilePath));
            List<String> updatedLines = lines.stream()
                    .map(line -> line.startsWith("EmailList=") ? "EmailList=" + properties.getProperty("EmailList") : line)
                    .collect(Collectors.toList());

            Files.write(Paths.get(configFilePath), updatedLines);
        } catch (IOException e) {
            System.err.println("更新配置文件时出错: " + e.getMessage());
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