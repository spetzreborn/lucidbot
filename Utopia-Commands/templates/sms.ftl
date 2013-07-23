<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Message sent to: "+NORMAL>
        <#list users as user>
        ${user.mainNick}
        </#list>
    </@compact>
</@ircmessage>