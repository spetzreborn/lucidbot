<@ircmessage type="reply_notice">
    <@compact>
        <#list provinces as province>
        ${BLUE+province.name+NORMAL} (<@compact><@colorIntel province=province/></@compact>)
        </#list>
    </@compact>
</@ircmessage>

<#macro colorIntel province>
    <#if province.sot??><@color string="SoT" date=province.sot.lastUpdated/><#else>-, </#if><#if province.som??><@color string="SoM" date=province.som.lastUpdated/><#else>-, </#if><#if province.survey??><@color string="Survey" date=province.survey.lastUpdated/><#else>-, </#if><#if province.sos??><@color string="SoS" date=province.sos.lastUpdated/><#else>-, </#if><#if province.thievesLastUpdated??><@color string="Infil" date=province.thievesLastUpdated/><#else>-</#if>
</#macro>

<#macro color string date>
${stringUtil.colorWithAge(string, date)}
</#macro>