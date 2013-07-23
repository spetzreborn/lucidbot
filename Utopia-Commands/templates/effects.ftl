<@ircmessage type="reply_notice">
    <#if buildingEffects??>
        <#list buildingEffects as effect>
        ${effect}
        </#list>
    <#elseif bpa??>
    ${bpa} bpa has that effect
    <#elseif scienceEffects??>
    The resulting effect is ${scienceEffects}
    <#elseif spellEffects??>
    ${spellEffects}
    <#elseif opEffects??>
    ${opEffects}
    </#if>
</@ircmessage>