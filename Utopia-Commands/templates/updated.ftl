<@ircmessage type="reply_notice">
    <@compact>
        <#list updated as item>
        ${BLUE}${item_index+1}.<#if item.province.provinceOwner??>${item.province.provinceOwner.mainNick}<#else>${item.province.name}</#if>${NORMAL}(${OLIVE+timeUtil.compareDateToCurrent(item.lastUpdated)+NORMAL})
        </#list>
    </@compact>
</@ircmessage>