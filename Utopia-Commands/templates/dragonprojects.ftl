<@ircmessage type="reply_notice">
    <#if projects??>
    ${DARK_GREEN+"Dragon Projects: "+NORMAL}
        <#list projects as project>
        [id:${project.id}] - ${project.type.typeName} - Status: ${project.status} - Last updated: ${project.createdDateGMT} GMT
        </#list>
    <#else>
    No dragon projects have been started
    </#if>
</@ircmessage>