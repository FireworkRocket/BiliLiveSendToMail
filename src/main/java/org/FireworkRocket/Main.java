package org.FireworkRocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static List<String> LiveIDs;
    private static boolean emailSentToday = false;
    static int num = 0;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static List<String> emailList;
    private static int checkTimeHour;
    private static int checkTimeMinute;
    private static int retryIntervalSeconds;
    private static String apiUrl;

    public static void main(String[] args) {
        loadConfig();
        scheduler.scheduleAtFixedRate(() -> {
            num++;
            if (!emailSentToday) {
                checkLiveStatusAndSendEmails();
            } else {
                System.out.println(num + ":今天的邮件已经发送或跳过。等待第二天。");
            }
        }, 0, retryIntervalSeconds, TimeUnit.SECONDS); // 使用配置文件中的重试间隔

        // 在指定时间重置 emailSentToday 标志
        scheduler.scheduleAtFixedRate(() -> {
            emailSentToday = false;
            System.out.println("重置 emailSentToday 标志，监听准备就绪。");
        }, getInitialDelay(), 24, TimeUnit.HOURS); // 每24小时重置一次
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        File configFile = new File("config.properties");

        if (!configFile.exists()) {
            try (FileOutputStream output = new FileOutputStream(configFile)) {
                // 设置默认属性
                properties.setProperty("LiveIDs", "XXXXXX,XXXXXX,XXXXXX");
                properties.setProperty("EmailList", "example1@example.com,example2@example.com");
                properties.setProperty("smtpHost", "<smtpHost>");
                properties.setProperty("smtpPort", "<smtpPort>");
                properties.setProperty("smtpUsername", "<smtpUsername>");
                properties.setProperty("smtpPassword", "<smtpPassword>");
                properties.setProperty("checkTimeHour", "21");
                properties.setProperty("checkTimeMinute", "0");
                properties.setProperty("retryIntervalSeconds", "10");
                properties.setProperty("apiUrl", "https://api.live.bilibili.com/room/v1/Room/get_info?room_id=");
                // 写入文件并添加中文注释和示例邮箱
                properties.store(output,
                        "Configuration file example \n " +
                                "EmailList: List of recipient email " +
                                "addresses, separated by commas \n  " +
                                "Example: example1@example.com,example2@example.com \n"
                                +" LiveIDs: List of recipient LiveIDs, separated by commas too \n" +
                                "  Example: XXXXXX,XXXXXX,XXXXX");
                System.out.println("配置文件已创建: " + configFile.getAbsolutePath() + " 请在配置完成后再次启动程序 :)");
                System.exit(0);
            } catch (IOException e) {
                System.err.println("创建配置文件时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }

        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            String emails = properties.getProperty("emailList");
            if (emails == null || emails.isEmpty()) {
                throw new IllegalArgumentException("配置文件无效");
            }
            emailList = Arrays.asList(emails.split(","));
            LiveIDs = Arrays.asList(properties.getProperty("LiveIDs").split(","));
            EmailSender.setSmtpConfig(
                    properties.getProperty("smtpHost"),
                    properties.getProperty("smtpPort"),
                    properties.getProperty("smtpUsername"),
                    properties.getProperty("smtpPassword")
            );
            checkTimeHour = Integer.parseInt(properties.getProperty("checkTimeHour"));
            checkTimeMinute = Integer.parseInt(properties.getProperty("checkTimeMinute"));
            retryIntervalSeconds = Integer.parseInt(properties.getProperty("retryIntervalSeconds"));
            apiUrl = properties.getProperty("apiUrl");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("加载配置时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkLiveStatusAndSendEmails() {
        for (String liveID : LiveIDs) {
            try {
                System.out.println("检查直播状态...");

                // 构建请求 URL
                URL url = new URL(apiUrl + liveID);
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.setRequestMethod("GET");
                request.setConnectTimeout(5000);
                request.setReadTimeout(5000);
                request.connect();

                // 将输入流转换为 JSON 对象
                JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(request.getInputStream())).getAsJsonObject();
                Gson gson = new Gson();
                Data data = gson.fromJson(jsonObject.getAsJsonObject("data"), Data.class);

                // 检查直播状态是否为 1
                if (data.getLiveStatus() == 1) {
                    System.out.println("直播状态为 1，准备发送邮件... 再3S内按下回车键跳过今天的发送");

                    // 等待用户输入
                    if (waitForUserInput(3)) {
                        System.out.println("用户跳过了发送邮件。");
                        emailSentToday = true;
                    } else {
                        // 构建邮件内容
                        String emailBody = "<!DOCTYPE html>" +
                                "<html>" +
                                "<head>" +
                                "<style>" +
                                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f3f3f3; padding: 20px; }" +
                                ".container { background-color: #ffffff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); }" +
                                "h1 { color: #0078d7; background-color: #f0f0f0; padding: 10px; border-radius: 4px; }" +
                                ".info-block { background-color: #e6e6fa; padding: 10px; border-radius: 4px; margin: 0; }" +
                                "p { color: #333333; margin: 0; }" +
                                "a { color: #0078d7; text-decoration: none; }" +
                                "a:hover { text-decoration: underline; }" +
                                ".image-container { text-align: center; margin: 20px 0; }" +
                                ".image-container img { max-width: 100%; height: auto; border-radius: 8px; }" +
                                "</style>" +
                                "</head>" +
                                "<body>" +
                                "<div class='container'>" +
                                "<div class='image-container'><img src='" + data.getUserCover() + "' alt='User Cover'></div>" +
                                "<h1>直播信息通知</h1>" +
                                "<div class='info-block'>" +
                                "<p><strong>UID:</strong> " + data.getUid() + "</p>" +
                                "<p><strong>标题:</strong> " + data.getTitle() + "</p>" +
                                "<p><strong>房间ID:</strong> " + data.getRoomId() + "</p>" +
                                "<p><strong>直播状态:</strong> " + data.getLiveStatus() + "</p>" +
                                "<p><strong>用户空间:</strong> <a href='https://space.bilibili.com/" + data.getUid() + "'>点击这里</a></p>" +
                                "<p><strong>直播链接:</strong> <a href='https://live.bilibili.com/" + liveID + "'>点击这里</a></p>" +
                                "</div>" +
                                "</div>" +
                                "</body>" +
                                "</html>";

                        String emailSubject = "直播信息通知";

                        // 发送邮件给所有收件人
                        EmailSender.sendEmails(emailList, emailSubject, emailBody);
                        emailSentToday = true; // 发送完邮件后设置标志为 true
                        System.out.println("邮件发送成功。");
                    }
                } else {
                    System.out.println("直播状态不是 1，将在" + retryIntervalSeconds + "秒后再次检查。");
                }

            } catch (Exception e) {
                System.err.println("发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean waitForUserInput(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000;
        try {
            while (System.currentTimeMillis() < endTime) {
                if (System.in.available() > 0) {
                    new Scanner(System.in).nextLine();
                    return true;
                }
                Thread.sleep(100); // Short sleep to avoid busy-waiting
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return false;
    }

    private static long getInitialDelay() {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        Calendar nextCheckTime = Calendar.getInstance(TimeZone.getDefault());
        nextCheckTime.set(Calendar.HOUR_OF_DAY, checkTimeHour);
        nextCheckTime.set(Calendar.MINUTE, checkTimeMinute);
        nextCheckTime.set(Calendar.SECOND, 0);
        nextCheckTime.set(Calendar.MILLISECOND, 0);
        if (now.after(nextCheckTime)) {
            nextCheckTime.add(Calendar.DAY_OF_MONTH, 1);
        }
        long initialDelay = nextCheckTime.getTimeInMillis() - now.getTimeInMillis();
        System.out.println("Initial delay: " + initialDelay + " milliseconds");
        return initialDelay;
    }
}