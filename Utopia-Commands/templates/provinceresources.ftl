<@ircmessage type="reply_notice">
    <@compact>
        <#list provincesWithResources as provAndRes>
        ${BLUE+(provAndRes_index+1)+"."+provAndRes.name+NORMAL}${stringUtil.colorWithAge(provAndRes.resource?string, provAndRes.lastUpdated)}(${DARK_GRAY}<#if provAndRes.province.race??>${provAndRes.province.race.name}<#else>?</#if>${NORMAL})
        </#list>
    </@compact>

    <#if sum?? && sum!=0>Sum: ${OLIVE+sum+NORMAL}</#if>
</@ircmessage>