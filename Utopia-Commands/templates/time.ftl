<#if isUtoTime>
    <@ircmessage type="reply_message">
    ${OLIVE}Current time in utopia:${NORMAL} ${BLUE+time}
    </@ircmessage>
<#else>
    <@ircmessage type="reply_notice">
    ${OLIVE}Time in ${BLUE+user}'s ${OLIVE}local time: ${BLUE+time+NORMAL} <#if difference??>(${difference})</#if>
    </@ircmessage>
</#if>