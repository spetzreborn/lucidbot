<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"To train: "+NORMAL separator=" | ">
        <#list train as info>
        ${info.troop}: ${info.amount}
        </#list>
    </@compact>
</@ircmessage>