<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+user.mainNick+"'s linked nicks: "+NORMAL>
        <#list nicks as nick>
        ${nick.nickname}
        </#list>
    </@compact>
</@ircmessage>