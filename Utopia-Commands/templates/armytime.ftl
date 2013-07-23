<@ircmessage type="reply_notice">
${user.mainNick}'s armies:
    <#list armies as armyTimePair>
    #${BLUE+armyTimePair.army.armyNumber+NORMAL} [${OLIVE+armyTimePair.time+NORMAL}] (${armyTimePair.army.landGained}a ${armyTimePair.army.generals}g ${armyTimePair.army.modOffense}mo) -${armyTimePair.army.type.source}-
    </#list>
</@ircmessage>