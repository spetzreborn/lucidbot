<@ircmessage type="reply_notice">
    <#if build??>
    ${build.type} (id:${build.id})<#if build.personality??> for ${build.personality.name}</#if> was added by ${build.addedBy} ${timeUtil.compareDateToCurrent(build.added)} ago
    Target land for this build: ${build.land} acres
        <@columns underlined=true colLengths=[10, 5, 10, 5]>
        OSPA:
        ${build.ospa}
        DSPA:
        ${build.dspa}
        EPA:
        ${build.epa}
        BPA:
        ${build.bpa}
        TPA:
        ${build.tpa}
        WPA:
        ${build.wpa}
        </@columns>
        <@columns underlined=true colLengths=[10, 5, 10, 5]>
            <#list build.buildings as building>
            ${building.building.shortName}
            ${building.percentage}%
            </#list>
            <#if !evenNoOfBuildings>
            -
            -
            </#if>
        </@columns>
    <#elseif race??>
    Builds for ${race.name}/<#if personality??>${personality.name}<#else>-</#if>:
        <#list builds as build>
        ${build.type} (id:${build.id})<#if build.personality??> for ${build.personality.name}</#if> was added by ${build.addedBy} ${timeUtil.compareDateToCurrent(build.added)} ago
        </#list>
    <#else>
    Builds for your setup:
        <#list builds as build>
        ${build.type} (id:${build.id}) was added by ${build.addedBy} ${timeUtil.compareDateToCurrent(build.added)} ago
        </#list>
    </#if>
</@ircmessage>