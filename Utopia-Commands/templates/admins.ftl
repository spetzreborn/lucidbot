<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Bot admins: "+NORMAL>
        <#list admins as nick>
        ${nick}
        </#list>
    </@compact>
</@ircmessage>