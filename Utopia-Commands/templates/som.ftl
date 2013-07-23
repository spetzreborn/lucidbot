<@ircmessage type="reply_notice">
    <#if intel.province.race??><#assign race=intel.province.race.name/></#if>
    <#if intel.province.personality??><#assign pers=intel.province.personality.name/></#if>
    <#assign name=intel.province.name loc=intel.province.kingdom.location/>
>> ${OLIVE+name+" "+loc+NORMAL} << ${OLIVE+race!} ${pers!}${NORMAL} Added: ${timeUtil.compareDateToCurrent(intel.lastUpdated)} ago by o${RED+intel.savedBy+NORMAL}o
${BLUE}Net Defense at Home: ${OLIVE}${intel.netDefense!"?"}${BLUE} Net Offense at Home: ${OLIVE}${intel.netOffense!"?"}${BLUE}
    <#list intel.sortedArmies as army>
        <#if army.type.typeName = "Army Home">
        ${BLUE}Army home: Solds: ${OLIVE+army.soldiers+BLUE} DS: ${OLIVE+army.defSpecs+BLUE} OS: ${OLIVE+army.offSpecs+BLUE} Elites: ${OLIVE+army.elites}
        <#elseif army.type.typeName = "Army Training">
        ${BLUE}In training: DS: ${OLIVE+army.defSpecs+BLUE} OS: ${OLIVE+army.offSpecs+BLUE} Elites: ${OLIVE+army.elites+BLUE} Thieves: ${OLIVE+army.thieves}
        <#else>
        ${BLUE}Army #: ${OLIVE+army.armyNumber+BLUE} with ${RED+army.generals+BLUE} gens returns in: ${RED+timeUtil.compareDateToCurrent(army.returningDate)+BLUE} Solds: ${OLIVE+army.soldiers+BLUE} OS: ${OLIVE+army.offSpecs+BLUE} Elites: ${OLIVE+army.elites+BLUE} Land: ${OLIVE+army.landGained}
        </#if>
    </#list>
${BLUE}Export: ${DARK_GREEN+intel.exportLine!""+NORMAL}
</@ircmessage>