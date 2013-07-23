<@compact intro=DARK_GREEN+"Returning armies: "+NORMAL>
    <#list armies as army>
        <#if army.province.provinceOwner??>
        ${BLUE+army.province.provinceOwner.mainNick+NORMAL} (${DARK_GRAY+timeUtil.compareDateToCurrent(army.returningDate)+NORMAL})
        <#else>
        ${BLUE+army.province.name} ${army.province.kingdom.location+NORMAL} (${DARK_GRAY+timeUtil.compareDateToCurrent(army.returningDate)+NORMAL})
        </#if>
    </#list>
</@compact>