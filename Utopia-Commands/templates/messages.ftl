<@ircmessage type="reply_notice">
    <#if messages?has_content>
        <#list messages as message>
        (id:${message.id}) From: ${message.sender} - To: ${message.recipient.mainNick} - Sent: ${user.getDateInUsersLocalTime(message.sent)}
        Message: ${message.message}
        </#list>
    <#else>
    You have no messages at the moment
    </#if>
</@ircmessage>