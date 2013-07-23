<#if army.province.provinceOwner??>
${BLUE+army.province.provinceOwner.mainNick}'s ${RED}army #${army.armyNumber} has returned
<#else>
${RED}Army #${OLIVE+army.armyNumber+RED} of ${BLUE+army.province.name} ${army.province.kingdom.location+RED} has returned
</#if>