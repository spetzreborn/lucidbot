<@ircmessage type="reply_notice">
    <#include "news_bar.ftl">

Current orders:
    <#list orders as order>
    [${order.categoryName}, id:${order.id}]: ${RED+order.order+NORMAL} (added ${DARK_GRAY+timeUtil.compareDateToCurrent(order.added)+NORMAL} ago)
    </#list>
</@ircmessage>