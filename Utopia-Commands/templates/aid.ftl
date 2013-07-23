<@ircmessage type="reply_notice">
    <#include "news_bar.ftl">

Aid requests:
    <#list requests as request>
    [id:${request.id}] ${BLUE+request.province.name+NORMAL} requests ${OLIVE+request.amount+NORMAL} ${request.type.typeName} (${request.importanceType.typeName} added ${timeUtil.compareDateToCurrent(request.added)} ago)
    </#list>
Aid offers:
    <#list offered as offer>
    [id:${offer.id}] ${BLUE+offer.province.name+NORMAL} offers ${OLIVE+offer.amount+NORMAL} ${offer.type.typeName} (added ${timeUtil.compareDateToCurrent(offer.added)} ago)
    </#list>
</@ircmessage>