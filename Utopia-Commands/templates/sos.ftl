<@ircmessage type="reply_notice">
    <#if intel.province.race??><#assign race=intel.province.race.name/></#if>
    <#if intel.province.personality??><#assign pers=intel.province.personality.name/></#if>
    <#assign name=intel.province.name loc=intel.province.kingdom.location/>
>> ${OLIVE+name+" "+loc+NORMAL} << ${OLIVE+race!} ${pers!}${NORMAL} Added: ${timeUtil.compareDateToCurrent(intel.lastUpdated)} ago by o${RED+intel.savedBy+NORMAL}o
    <@columns underlined=true colLengths=[14, 7, 12]>
        <#list intel.sciInfo?keys as key>
        ${BLUE+key}
        ${OLIVE+intel.sciInfo[key].effect}%
        ${DARK_GREEN}${intel.sciInfo[key].books}
        </#list>
    </@columns>
${BLUE}Total: ${OLIVE+intel.totalBooks+BLUE} BPA: ${OLIVE+intel.booksPerAcre}
${BLUE}Export: ${DARK_GREEN+intel.exportLine!""+NORMAL}
</@ircmessage>