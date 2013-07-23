<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Order categories: "+NORMAL>
        <#list categories as cat>
        ${cat.name}
        </#list>
    </@compact>
</@ircmessage>