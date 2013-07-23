<@ircmessage type="reply_notice">
    <#include "news_bar.ftl">

    <#list notes as note>
    [id:${note.id}] ${DARK_GREEN}Added ${note.addedTimeAgo} ago by ${BLUE+note.addedBy+NORMAL}
    ${NORMAL+note.message}
    </#list>
</@ircmessage>