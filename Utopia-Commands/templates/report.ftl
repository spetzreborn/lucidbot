<@ircmessage type="reply_notice">
${user.mainNick} has been idle for ${timeUtil.compareDateToCurrent(activities.lastActivity)}
    <#list report as info>
        <#if info.unseen != 0>Last checked ${info.type} ${timeUtil.compareDateToCurrent(info.lastCheck)}, leaving ${info.unseen} unseen</#if>
    </#list>
</@ircmessage>