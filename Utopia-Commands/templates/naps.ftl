<@ircmessage type="reply_notice">
    <#include "news_bar.ftl">

    <#list naps as nap>
    NAP with ${nap.kdLocation} <#if nap.expiry??>ends on ${nap.expiryUtoDate} (in ${nap.expiry})</#if> - Details: ${nap.details}
    </#list>
</@ircmessage>