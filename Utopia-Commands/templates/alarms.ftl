<@ircmessage type="reply_notice">
${DARK_GREEN+"Alarms: "+NORMAL}
    <#list alarms as alarm>
    Id: ${alarm.id}, expiring in ${alarm.timeLeft} with message: ${alarm.message}
    </#list>
</@ircmessage>