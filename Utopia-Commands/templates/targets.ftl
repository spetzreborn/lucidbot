<@ircmessage type="reply_notice">
    <#list targets as target>
    (id:${target.id}) ${target.province.name} ${target.province.kingdom.location} - Details: ${target.details}
        <#if target.hitters?has_content>
            <@compact intro="Hitters: ">
                <#list target.hitters as hitter>
                ${BLUE+hitter.mainNick+NORMAL}
                </#list>
            </@compact>

        </#if>
    </#list>
</@ircmessage>