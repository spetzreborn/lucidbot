<@ircmessage type="reply_notice">
    <#if isDuration>
    ${type} lasting for ${result} ticks added for ${province.name}
    <#else>
    ${result} (${type}) added for ${province.name}
    </#if>
</@ircmessage>