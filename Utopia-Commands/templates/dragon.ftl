<@ircmessage type="reply_notice">
Original dragon status: ${project.originalStatus} (created: ${timeUtil.compareDateToCurrent(project.created)} ago)
Current dragon status: ${project.status} (updated: ${timeUtil.compareDateToCurrent(project.updated)} ago)
    <#if actions?has_content>
        <@compact intro=DARK_GREEN+"Actions: "+NORMAL>
            <#list actions as action>
            ${BLUE+action.user.mainNick+NORMAL} (${OLIVE+action.contribution+NORMAL})
            </#list>
        </@compact>
    </#if>
</@ircmessage>