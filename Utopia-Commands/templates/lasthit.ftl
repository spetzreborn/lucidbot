<@ircmessage type="reply_notice">
${attack.attacker.name} => ${attack.type.typeName} (${attack.gain!"0 gain"}) => ${attack.target.name}
    <@compress single_line=true>
    Attack was made ${timeUtil.compareDateToCurrent(attack.timeOfAttack)} ago with ${attack.offenseSent} mo, killing ${attack.kills}
        <#if attack.spreadPlague>, giving the target plague</#if>
        <#if attack.gotPlagued>, giving the hitter plague</#if>
    </@compress>
</@ircmessage>