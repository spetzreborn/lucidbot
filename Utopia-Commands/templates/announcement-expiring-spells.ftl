<#compress>
    <#list spells as spell>
        <#if spell.province.provinceOwner??>
        ${BLUE+spell.province.provinceOwner.mainNick+OLIVE}'s ${DARK_GREEN+spell.type.name+OLIVE} expires this tick
        <#else>
        ${BLUE+spell.province.name} ${spell.province.kingdom.location+OLIVE}'s ${DARK_GREEN+spell.type.name+OLIVE} expires this tick
        </#if>
    </#list>
</#compress>