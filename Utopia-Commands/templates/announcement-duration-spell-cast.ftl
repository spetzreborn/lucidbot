<#if spell.type.spellCharacter.characterName = "Self Spellop">
<#else>
${DARK_GREEN+spell.type.name+RED} added for ${BLUE+spell.province.name} ${spell.province.kingdom.location+RED}, expires in ${OLIVE+spell.timeLeft}
</#if>