<@ircmessage type="action" target=channel>
    <@compact intro="slaps " separator=" ">
        <#list users as user>
        ${user.currentNick}
        </#list>
    </@compact>
</@ircmessage>