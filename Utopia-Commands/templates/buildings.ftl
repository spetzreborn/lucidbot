<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Building short names: "+NORMAL>
        <#list buildings as building>
        ${building.shortName}
        </#list>
    </@compact>
</@ircmessage>