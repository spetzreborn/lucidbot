<@ircmessage type="reply_notice">
    <#if commandTypes??>
        <@compact intro=DARK_GREEN+"Command types: "+NORMAL>
            <#list commandTypes as type>
            ${type}
            </#list>
        </@compact>
        <#if topics?size!=0>
            <@compact intro=DARK_GREEN+"Help topics: "+NORMAL>
                <#list topics as topic>
                ${topic.name}
                </#list>
            </@compact>
        </#if>
        <#if collections?size!=0>
            <@compact intro=DARK_GREEN+"Help topic collections: "+NORMAL>
                <#list collections as col>
                ${col.name}
                </#list>
            </@compact>
        </#if>
    <#elseif topic??>
        <#list topic.splitText as line>
        ${line}
        </#list>
    <#elseif collection??>
        <#if helpTopics?size!=0>
            <@compact intro=DARK_GREEN+"Help topics: "+NORMAL>
                <#list helpTopics as topic>
                ${topic.name}
                </#list>
            </@compact>
        </#if>
        <#if children?size!=0>
            <@compact intro=DARK_GREEN+"Help topic collections: "+NORMAL>
                <#list children as col>
                ${col.name}
                </#list>
            </@compact>
        </#if>
    <#elseif commands??>
        <@compact intro=DARK_GREEN+"Commands: "+NORMAL>
            <#list commands as command>
            ${command.name}
            </#list>
        </@compact>
    <#elseif command??>
    ${command.name} - ${command.helpText}
    </#if>
</@ircmessage>