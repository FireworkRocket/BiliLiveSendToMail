# BiliLiveSendToMail
哔哩哔哩直播消息发送到邮箱

使用方法：

用Java22在首次运行后会创建一个配置文件：

```
# Configuration file example
# EmailList: List of recipient email addresses, separated by commas
# Example: example1@example.com,example2@example.com
# LiveIDs: List of recipient LiveIDs, separated by commas too
# Example: XXXXXX,XXXXXX,XXXXX
LiveIDs=XXXXXX,XXXXXX,XXXXXX
EmailList=example1@example.com,example2@example.com
smtpHost=<smtpHost>
smtpPort=<smtpPort>
smtpUsername=<smtpUsername>
smtpPassword=<smtpPassword>
imapHost=<imapHost>
imapPort=<imapPort>
imapUsername=<imapUsername>
imapPassword=<imapPassword>
checkTimeHour=0
checkTimeMinute=0
checkTimeSed=0
retryIntervalSeconds=10
apiUrl=https://api.live.bilibili.com/room/v1/Room/get_info?room_id=
```

按照提示填写即可
翻译对照：

```
LiveIDs=填写你要监听的主播，如有多个，用英文的逗号分隔
EmailList=填写你要就收推送的邮箱，如有多个，用英文的逗号分隔
smtpHost=SMTP服务器
smtpPort=SMPT端口
smtpUsername=SMPT用户名
smtpPassword=SMTP密码
imapHost=IMAP服务器
imapPort=IMAP端口
imapUsername=IMAP用户名
imapPassword=IMAP密码
checkTimeHour=定时任务，执行完这次推送后多久再检测（小时）
checkTimeMinute=定时任务，执行完这次推送后多久再检测（分钟）
checkTimeSed=定时任务，执行完这次推送后多久再检测（秒）
retryIntervalSeconds=检测到未开播时多久再重试（秒）
apiUrl=最好不要改
```

注意SMTP和IMAP的区分，两种协议并不一样，填写时需要注意， 填写完成后重新运行即可

Linux后台运行命令：
`
nohup <命令> &
`

Windows随便挂在那里就行

记得一定要用Java22+才可以运行，低版本Java会直接报错