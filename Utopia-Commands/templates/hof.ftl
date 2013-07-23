<@ircmessage type="reply_notice">
-- Kingdom Hall of Fame --
    <#list hof?keys as entry>
    ${DARK_GREEN+entry+NORMAL}:<#rt>
        <@compact intro=" ">
            <#list hof[entry] as stats>
            ${BLUE+stats.nick+NORMAL} (${OLIVE+stats.number+NORMAL})
                <#if stats_index = 5><#break></#if>
            </#list>
        </@compact>

    </#list>
</@ircmessage>