<@ircmessage type="reply_notice">
    <#list sections as section>
    (id:${section.id} - access:${section.accessLevelName}) ${section.name}
    </#list>
</@ircmessage>