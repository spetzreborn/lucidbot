<@ircmessage type="reply_notice">
    <#list usersByCountry?keys as key>
        <@compact intro=DARK_GREEN+key+": "+NORMAL>
            <#list usersByCountry[key] as user>
            ${user.mainNick}
            </#list>
        </@compact>

    </#list>
</@ircmessage>