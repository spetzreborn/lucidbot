<@ircmessage type="reply_notice">
    <#if intel.province.race??><#assign race=intel.province.race.name/></#if>
    <#if intel.province.personality??><#assign pers=intel.province.personality.name/></#if>
    <#assign name=intel.province.name loc=intel.province.kingdom.location/>
>> ${OLIVE+name+" "+loc+NORMAL} << ${OLIVE+race!} ${pers!}${NORMAL} Added: ${timeUtil.compareDateToCurrent(intel.lastUpdated)} ago by o${RED+intel.savedBy+NORMAL}o
    <@columns underlined=true colLengths=[20, 6, 6, 7, 7]>
        <#list intel.buildingInfo?keys as key>
        ${BLUE+key}
        ${OLIVE+intel.buildingInfo[key].amount}
        ${OLIVE+"+"+intel.buildingInfo[key].progress}
        ${intel.buildingInfo[key].percent?string("0.#")}%
        +${intel.buildingInfo[key].progressPercent?string("0.#")}%
        </#list>
    </@columns>
${BLUE}Export: ${DARK_GREEN+intel.exportLine!""+NORMAL}
</@ircmessage>