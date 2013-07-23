<@ircmessage type="reply_notice">
    <#list provinces as province>
    ${province.name} - ${province.race.name+"/"+province.personality.name} played by ${province.provinceOwner.mainNick}
    </#list>
</@ircmessage>