<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"To build: "+NORMAL separator=" | ">
        <#list build as info>
        ${info.building.name}: ${info.amount}
        </#list>
    </@compact>
</@ircmessage>