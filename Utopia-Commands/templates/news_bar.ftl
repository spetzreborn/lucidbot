<@compact separator=" | ">
    <#list unseenInfoOfInterest as info>
        <#if thisActivityType?? && info.type = thisActivityType.typeName>
        ${info.type}
        <#elseif info.unseen != 0>
        ${RED+info.type} (${info.unseen})${NORMAL}
        <#else>
        ${DARK_GREEN+info.type+NORMAL}
        </#if>
    </#list>
</@compact>
