<@ircmessage type="reply_notice">
    <#if province??>
    ${type} results for ${province.name}:
    ${totalDamage} in ${totalAttempts} tries
        <#list spellsAndOps as info>
        Totals for ${BLUE+info.committer.mainNick+NORMAL} = ${info.damage} in ${info.amount} tries
        </#list>
    <#else>
        <@columns underlined=true colLengths=columnLengths>
        Province\Number
        Total damage
        Total attempts
        </@columns>
        <#list infoList as info>
            <@columns underlined=true colLengths=columnLengths>
            ${info.province.name}
            ${info.totalDamage}
            ${info.totalAttempts}
            </@columns>
        </#list>
    </#if>
</@ircmessage>