<#if aid.importanceType.typeName="Offering Aid">
${RED}An aid offer for ${OLIVE+aid.amount} ${aid.type.typeName+RED} was just added!
<#else>
${RED}An aid request for ${OLIVE+aid.amount} ${aid.type.typeName+RED} was just added for ${BLUE+aid.province.name+RED}!
</#if>