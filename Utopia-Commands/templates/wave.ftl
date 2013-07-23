<@ircmessage type="reply_notice">
    <#compress>
        <#if attendance??>
        Attendance registered
        <#else>
            <#include "news_bar.ftl">

        The wave is scheduled for ${utodate} (in ${timeLeft})
        </#if>
    </#compress>
</@ircmessage>