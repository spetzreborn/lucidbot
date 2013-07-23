<@ircmessage type="reply_notice">
    <@compact intro=DARK_GREEN+"Threads in that section: "+NORMAL>
        <#list threads as thread>
        ${BLUE+thread.name+NORMAL} (id:${thread.id})
        </#list>
    </@compact>
</@ircmessage>