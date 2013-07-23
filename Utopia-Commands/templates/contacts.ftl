<@ircmessage type="reply_notice">
    <#if user??>
    ${BLUE+user.mainNick+NORMAL} is ${user.realName!"?"} from ${user.country!"?"}
        <#if user.email??>Email: ${user.email}</#if>
        <#if user.sms??>Sms address: ${user.sms} (Confirmed? ${user.smsConfirmed?string})</#if>
        <#list user.contactInformation as contact>
        ${contact.informationType}: ${contact.information}
        </#list>
    <#elseif value??>
    ${type} was set to ${value}
    <#else>
    ${type} was removed from the contact information
    </#if>
</@ircmessage>