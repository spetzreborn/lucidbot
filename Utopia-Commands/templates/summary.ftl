<@ircmessage type="reply_notice">
    <#if summary??>
    -- Summary --
    Total gains: ${summary.selfGains.landTaken} (${summary.selfGains.hitsMade} hits) vs. ${summary.enemyGains.landTaken} (${summary.enemyGains.hitsMade} hits)
        <#list summary.kdStats as stat>
            <#if (stat.selfActivities.hitsMade > 0) || (stat.enemyActivities.hitsMade > 0)>
            ${stat.attackType.typeName}: ${stat.selfActivities.totalResultsOut} (${stat.selfActivities.hitsMade} hits) vs. ${stat.enemyActivities.totalResultsOut} (${stat.enemyActivities.hitsMade} hits)
            </#if>
        </#list>
    <#else>
    -- Summary for ${province} --
    Total land exchange: ${landExchange} (${hitsMade} hits made / ${hitsTaken} hits taken)
        <#list stats as stat>
            <#if (stat.activities.hitsMade > 0) || (stat.activities.hitsReceived > 0)>
            ${stat.attackType.typeName}: ${stat.activities.totalResultsOut} (${stat.activities.hitsMade} hits) vs. ${stat.activities.totalResultsIn} (${stat.activities.hitsReceived} hits)
            </#if>
        </#list>
    </#if>
Last added news item: ${lastAddedNewsItem.originalMessage}
</@ircmessage>