<@ircmessage type="reply_notice">
${user.mainNick}'s stats:
    <#list stats?keys as stat>
    ${stat}: ${stats[stat]}
    </#list>
</@ircmessage>