<@ircmessage type="reply_notice">
    <#if online>
    ${user.mainNick} is already connected with the following nicknames: ${stringUtil.merge(nicks, ", ")}
    <#else>
    ${user.mainNick} was last seen ${seen} ago
    </#if>
</@ircmessage>