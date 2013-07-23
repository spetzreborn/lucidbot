<@ircmessage type="reply_notice">
    <#list checkedin as checkIn>
    ${BLUE+checkIn.user.mainNick+NORMAL} checked in ${DARK_GREEN+checkIn.checkedIn+NORMAL} (${timeUtil.compareDateToCurrent(checkIn.checkInTime)})
    </#list>
</@ircmessage>