<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Notification added for: "+NORMAL>
        <#list notifications as notification>
        ${notification.user.mainNick}
        </#list>
    </@compact>

    <#if hasIgnored>
    Some user(s) either had the notification set already or lacked the necessary info for the method used (for example no email address)
    </#if>
</@ircmessage>