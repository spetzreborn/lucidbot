<@ircmessage type="reply_notice">
    <@compact>
        <#list claimed as claim>
        ${claim.province.name} (${BLUE+claim.hitters[0].mainNick+NORMAL})
        </#list>
    </@compact>
</@ircmessage>