<@ircmessage type="reply_notice">
    <#if provinceWithResource??>
        <#if provinceWithResource.province.provinceOwner??>
        ${BLUE+provinceWithResource.province.provinceOwner.mainNick+OLIVE+provinceWithResource.resource+NORMAL}
        <#else>
        ${BLUE+provinceWithResource.province.name+OLIVE+provinceWithResource.resource+NORMAL}
        </#if>
    <#else>
        <@compact>
            <#list provinces as info>
                <#if info.province.provinceOwner??>
                ${BLUE+info.province.provinceOwner.mainNick+OLIVE+info.resource+NORMAL}
                <#else>
                ${BLUE+info.province.name+OLIVE+info.resource+NORMAL}
                </#if>
            </#list>
        </@compact>
    </#if>
</@ircmessage>