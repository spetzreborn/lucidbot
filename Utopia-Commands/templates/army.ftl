<@ircmessage type="reply_notice">
    <#if added>
    Army added for ${BLUE+user.mainNick+NORMAL}, returns in ${OLIVE+timeUtil.compareDateToCurrent(army.returningDate)}
    <#else>
    ${user.mainNick}'s armies:
        <#list armies as army>
        #${BLUE+army.armyNumber+NORMAL} [${OLIVE+timeUtil.compareDateToCurrent(army.returningDate)+NORMAL}] (${army.landGained}a ${army.generals}g ${army.modOffense}mo) -${army.type.source}-
        </#list>
    </#if>
</@ircmessage>