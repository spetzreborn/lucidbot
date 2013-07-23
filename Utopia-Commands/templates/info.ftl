<@ircmessage type="reply_notice">
    <#if mainnick??>
    ${mainnick}'s status: ${status}
    </#if>
    <#if armies??>
        <#list armies as info>
        Army #${info.item.armyNumber} returns in ${info.time}
        </#list>
    </#if>
    <#if ops??>
        <#list ops as info>
        ${info.item.type.name} expires in ${info.time}
        </#list>
    </#if>
    <#if spells??>
        <#list spells as info>
        ${info.item.type.name} expires in ${info.time}
        </#list>
    </#if>
    <#if aid??>
        <#list aid as info>
        Aid: ${info.item.amount} ${info.item.type.typeName} was added ${info.time} ago (${info.item.importanceType.typeName})
        </#list>
    </#if>
</@ircmessage>