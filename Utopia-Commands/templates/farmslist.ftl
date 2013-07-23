<@ircmessage type="reply_notice">
    <#list farms as farm>
    (id:${farm.id}) ${farm.province.name} ${farm.province.kingdom.location} - Details: ${farm.details}
    </#list>
</@ircmessage>