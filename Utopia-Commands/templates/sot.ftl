<@ircmessage type="reply_notice">
    <#if intel.province.race??><#assign race=intel.province.race.name/></#if>
    <#if intel.province.personality??><#assign pers=intel.province.personality.name/></#if>
    <#assign name=intel.province.name loc=intel.province.kingdom.location/>
>> ${OLIVE+name+" "+loc+NORMAL} << ${OLIVE+race!} ${pers!}${NORMAL} Added: ${timeUtil.compareDateToCurrent(intel.lastUpdated)} ago by o${RED+intel.savedBy+NORMAL}o
Land: ${PURPLE+intel.province.land+NORMAL} NW: ${PURPLE+intel.province.networth+NORMAL} NWPA: ${PURPLE+intel.province.nwpa+NORMAL} BE: ${PURPLE+intel.buildingEfficiency+NORMAL} Peasants: ${PURPLE+intel.peasants+NORMAL} TB: ${PURPLE+intel.tradeBalance+NORMAL}
    <@compress  single_line=true>
    Gold: ${BLUE+intel.money+NORMAL} Food: ${BLUE+intel.food+NORMAL} Runes: ${BLUE+intel.runes+NORMAL}
    TPA: <#if intel.province.thievesLastUpdated??>${stringUtil.colorWithAge(intel.province.modThievesPerAcre?string("0.#"), intel.province.thievesLastUpdated)}<#else>${intel.province.modThievesPerAcre?string("0.#")}</#if>
    WPA: <#if intel.province.wizardsLastUpdated??>${stringUtil.colorWithAge(intel.province.modWizardsPerAcre?string("0.#"), intel.province.wizardsLastUpdated)}<#else>${intel.province.modWizardsPerAcre?string("0.#")}</#if>
    </@compress>

    <#nt>Solds: ${MAGENTA+intel.soldiers+NORMAL} Ospecs: ${MAGENTA+intel.offSpecs+NORMAL} Dspecs: ${MAGENTA+intel.defSpecs+NORMAL} Elites: ${MAGENTA+intel.elites+NORMAL} Horses: ${MAGENTA+intel.warHorses+NORMAL} Prisoners: ${MAGENTA+intel.prisoners+NORMAL}
OME: ${PURPLE+intel.offensiveME+NORMAL} Offense: ${RED+intel.modOffense} (${intel.modOffensePerAcre} opa) ${NORMAL} DME: ${PURPLE+intel.defensiveME+NORMAL} Defense: ${RED+intel.modDefense} (${intel.modDefensePerAcre} dpa)${NORMAL}
Practical Off: ${BLUE+intel.province.pmo} (${intel.province.practicalModOffPerAcre} opa)${NORMAL} Practical Def: ${BLUE+intel.province.pmd} (${intel.province.practicalModDefPerAcre} dpa)${NORMAL}
    <#if intel.overpopulated>Is overpopulated! </#if><#if intel.plagued>Has the plague! </#if><#if intel.province.kingdom.dragon??>Has a ${intel.province.kingdom.dragon.name} dragon!</#if>
${BLUE}Export: ${DARK_GREEN+intel.exportLine!""+NORMAL}
</@ircmessage>