<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Idle times: "+NORMAL>
        <#list activities as act>
        ${BLUE+act.user.mainNick+NORMAL} (${OLIVE+timeUtil.compareDateToCurrent(act.lastActivity)+NORMAL})
        </#list>
    </@compact>
</@ircmessage>