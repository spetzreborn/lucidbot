<#compress>
    <#list ops as op>
        <#if op.province.provinceOwner??>
        ${BLUE+op.province.provinceOwner.mainNick+OLIVE}'s ${DARK_GREEN+op.type.name+OLIVE} expires this tick
        <#else>
        ${BLUE+op.province.name} ${op.province.kingdom.location+OLIVE}'s ${DARK_GREEN+op.type.name+OLIVE} expires this tick
        </#if>
    </#list>
</#compress>