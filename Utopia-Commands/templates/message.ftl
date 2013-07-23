<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Message(s) sent to: "+NORMAL>
        <#list messages as message>
        ${message.recipient.mainNick} (id:${message.id})
        </#list>
    </@compact>
</@ircmessage>