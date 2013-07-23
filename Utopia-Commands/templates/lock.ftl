<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Locked: "+NORMAL>
        <#list channels as channel>
        ${channel.name}
        </#list>
    </@compact>
</@ircmessage>