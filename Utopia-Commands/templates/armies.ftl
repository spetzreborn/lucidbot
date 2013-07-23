<@ircmessage type="reply_notice">
    <#if home && full>
        <@compact intro=DARK_GREEN+"Provinces with generals home: "+NORMAL>
            <#if provinces?size=0>None
            <#else>
                <#list provinces as prov>
                    <#assign name><#if prov.provinceOwner??>${prov.provinceOwner.mainNick}<#else>${prov.name}</#if></#assign>
                ${BLUE+name+NORMAL} (${prov.generalsHome}g<#if prov.estimatedCurrentOffense??>${" "+prov.estimatedCurrentOffense+" mo"}</#if>)
                </#list>
            </#if>
        </@compact>
    <#elseif home && !full>
        <@compact intro=DARK_GREEN+"Provinces with armies home: "+NORMAL>
            <#if provinces?size=0>None
            <#else>
                <#list provinces as prov>
                    <#assign name><#if prov.provinceOwner??>${prov.provinceOwner.mainNick}<#else>${prov.name}</#if></#assign>
                ${BLUE+name+NORMAL}
                </#list>
            </#if>
        </@compact>
    <#else>
        <@compact intro=DARK_GREEN+"Armies returning: "+NORMAL>
            <#if armies?size=0>None
            <#else>
                <#list armies as army>
                    <#assign name><#if army.province.provinceOwner??>${army.province.provinceOwner.mainNick}<#else>${army.province.name}</#if></#assign>
                ${BLUE+name+NORMAL} [${OLIVE+timeUtil.compareDateToCurrent(army.returningDate)+NORMAL}] (${army.landGained}a ${army.generals}g ${army.modOffense}mo) -${army.type.source}-
                </#list>
            </#if>
        </@compact>
    </#if>
</@ircmessage>