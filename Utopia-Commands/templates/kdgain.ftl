<@ircmessage type="reply_notice">
    <#if hasResource>
    The gain would be ${result}
    <#else>
    The gain would be the targeted resource*${percentage?string("0.##")}
    </#if>
</@ircmessage>