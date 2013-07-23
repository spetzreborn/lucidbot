<@ircmessage type="reply_notice">
${OLIVE}The kingdom of ${BLUE+kingdom.name!""} ${kingdom.location+OLIVE} Updated: ${BLUE+timeUtil.compareDateToCurrent(kingdom.lastUpdated)} ago
${OLIVE}NW: ${BLUE+kingdom.totalNw+OLIVE} Avg NW: ${BLUE+kingdom.averageNw+OLIVE} ${OLIVE}Land: ${BLUE+kingdom.totalLand+OLIVE} Avg Land: ${BLUE+kingdom.averageLand+OLIVE}
    <#list kingdom.setupInfo as info>
        <@compact intro=OLIVE+info.title+": ">
            <#list info.map?keys as item>
            ${BLUE+info.map[item]+NORMAL} ${BLACK+item+NORMAL}
            </#list>
            <#if info.unknowns!=0>${BLUE+info.unknowns+NORMAL} ${BLACK+"Unknown"+NORMAL}</#if>
        </@compact>

    </#list>
    <#if kingdom.kdComment??>${OLIVE}Comment: ${BLACK+kingdom.kdComment+NORMAL}</#if>
</@ircmessage>
