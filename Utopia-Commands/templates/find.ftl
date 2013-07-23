<@ircmessage type="reply_notice">
    <#if hasSortingSpecified>
        <@compact intro="Matching provinces: ">
            <#list found as info>
            ${(info_index+1)}. ${BLUE+info.province.name} ${info.province.kingdom.location+NORMAL} [${OLIVE+stringUtil.colorWithAge(info.resource, info.province.lastUpdated)+NORMAL}]
            </#list>
        </@compact>

    <#else>
        <@compact intro="Matching provinces: ">
            <#list found as province>
            ${(province_index+1)}. ${BLUE+province.name} ${province.kingdom.location+NORMAL}
            </#list>
        </@compact>

    </#if>
</@ircmessage>