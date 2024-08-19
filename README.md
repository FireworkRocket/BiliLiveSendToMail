# BiliLiveSendToMail
哔哩哔哩直播消息发送到邮箱

使用方法：

用Java22在首次运行后会创建一个配置文件：

```
#Configuration file example 
# EmailList: List of recipient email addresses, separated by commas 
#  Example: example1@example.com,example2@example.com 
# LiveIDs: List of recipient LiveIDs, separated by commas too 
#  Example: XXXXXX,XXXXXX,XXXXX
#Mon Aug 19 23:30:07 CST 2024
EmailList=example1@example.com,example2@example.com
LiveIDs=XXXXXX,XXXXXX,XXXXXX
apiUrl=https\://api.live.bilibili.com/room/v1/Room/get_info?room_id\=
checkTimeHour=21
checkTimeMinute=0
retryIntervalSeconds=10
smtpHost=<smtpHost>
smtpPassword=<smtpPassword>
smtpPort=<smtpPort>
smtpUsername=<smtpUsername>
```

按照提示填写即可
翻译对照：

```
EmailList=填写你要就收推送的邮箱，如有多个，用英文的逗号分隔
LiveIDs=填写你要监听的主播，如有多个，用英文的逗号分隔
apiUrl=最好不要改
checkTimeHour=定时任务，执行完这次推送后多久再检测（小时）
checkTimeMinute=定时任务，执行完这次推送后多久再检测（分钟）
retryIntervalSeconds=检测到未开播时多久再重试（秒）
smtpHost=SMTP服务器
smtpPassword=SMTP密码
smtpPort=SMPT端口
smtpUsername=SMPT密码
```


