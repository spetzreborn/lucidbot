<@ircmessage type="reply_notice">
Ambushing ${province.name}:
    <#list armies as aaPair>
    Raw offense needed to ambush army #${aaPair.army.armyNumber} (${aaPair.army.landGained} acres): ${aaPair.ambush}
    </#list>
NOTE: Town watch is not included in the calculations
</@ircmessage>