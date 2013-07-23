<@ircmessage type="reply_message">
    <@compact intro=DARK_GREEN+"Attending: "+NORMAL>
        <#list attendance.ATTENDING as status>
        ${BLUE+status.user.mainNick+NORMAL}
        </#list>
    </@compact>

    <@compact intro=BROWN+"Late: "+NORMAL>
        <#list attendance.LATE as status>
        ${BLUE+status.user.mainNick+NORMAL} ${DARK_GRAY}(${status.details})${NORMAL}
        </#list>
    </@compact>

    <@compact intro=RED+"Not attending: "+NORMAL>
        <#list attendance.NOT_ATTENDING as status>
        ${BLUE+status.user.mainNick+NORMAL}
        </#list>
    </@compact>
</@ircmessage>