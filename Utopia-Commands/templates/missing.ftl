<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Missing: "+NORMAL>
        <#list provinces as prov>
        ${BLUE+prov.name+NORMAL}
        </#list>
    </@compact>
</@ircmessage>