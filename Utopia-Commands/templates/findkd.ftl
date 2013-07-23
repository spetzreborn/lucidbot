<@ircmessage type="reply_notice">
    <@compact intro="Matching kingdoms: ">
        <#list kingdoms as kingdom>
        ${kingdom.location}
        </#list>
    </@compact>
</@ircmessage>