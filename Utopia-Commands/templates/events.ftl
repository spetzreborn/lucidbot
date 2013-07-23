<@ircmessage type="reply_notice">
    <#if attendance??>
    Attendance registered
    <#else>
        <#include "news_bar.ftl">

        <#list eventDetails as det>
        Event ${det.event.id}: ${det.event.description}
        Scheduled for: ${det.utodate} (in ${det.timeLeft})
        </#list>
    </#if>
</@ircmessage>