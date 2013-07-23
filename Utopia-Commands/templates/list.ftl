<@ircmessage type="reply_notice">
    <#if spellUnspecified??>
        <@compact intro=DARK_GREEN+"Active "+type.name+": "+NORMAL>
            <#list spellUnspecified as info>
            ${BLUE+info.province.name+NORMAL}<#if !kdSpecified> ${info.province.kingdom.location}</#if> (${OLIVE+info.timeLeftInHours+NORMAL})
            </#list>
        </@compact>

        <#if kdSpecified>
            <@compact intro=RED+"Missing "+type.name+": "+NORMAL>
                <#list missing as prov>
                ${BLUE+prov.name+NORMAL}
                </#list>
            </@compact>

        </#if>
    <#elseif opUnspecified??>
        <@compact intro=DARK_GREEN+"Active "+type.name+": "+NORMAL>
            <#list opUnspecified as info>
            ${BLUE+info.province.name+NORMAL}<#if !kdSpecified> ${info.province.kingdom.location}</#if> (${OLIVE+info.timeLeftInHours+NORMAL})
            </#list>
        </@compact>

        <#if kdSpecified>
            <@compact intro=RED+"Missing "+type.name+": "+NORMAL>
                <#list missing as prov>
                ${BLUE+prov.name+NORMAL}
                </#list>
            </@compact>

        </#if>
    <#elseif allUnspecified??>
        <@columns underlined=true colLengths=columnLengths>
        Province\Type
            <#list columnDefinitions as def>
            ${def}
            </#list>
        </@columns>
        <#list allUnspecified as info>
            <@columns underlined=true colLengths=columnLengths>
            ${info.province.name}
                <#list info.durations as duration>
                ${duration}
                </#list>
            </@columns>
        </#list>
    <#elseif spellSpecified??>
    ${BLUE+spellSpecified.province.name+NORMAL} has ${spellSpecified.type.name} which expires on the tick in (${OLIVE+spellSpecified.timeLeftInHours+NORMAL}) hours
    <#elseif opSpecified??>
    ${BLUE+opSpecified.province.name+NORMAL} has ${opSpecified.type.name} which expires on the tick in (${OLIVE+opSpecified.timeLeftInHours+NORMAL}) hours
    <#else>
        <#if allSpellsSpecified?has_content>
        ${BLUE+province.name+NORMAL} has the following spells up:
            <#list allSpellsSpecified as spell>
            ${DARK_GREEN+spell.type.name} expires in (${OLIVE+spell.timeLeftInHours+NORMAL}) hours
            </#list>
        </#if>
        <#if allOpsSpecified?has_content>
        ${BLUE+province.name+NORMAL} has the following ops up:
            <#list allOpsSpecified as op>
            ${DARK_GREEN+op.type.name} expires in (${OLIVE+op.timeLeftInHours+NORMAL}) hours
            </#list>
        </#if>
    </#if>
</@ircmessage>