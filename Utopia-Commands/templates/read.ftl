<@ircmessage type="reply_notice">
${DARK_GREEN}Posts in that thread: ${NORMAL}
    <#list posts as post>
    Posted by ${BLUE+post.user.mainNick+NORMAL} (id: ${post.id}) @ ${OLIVE+user.getDateInUsersLocalTime(post.posted)+NORMAL} <#if post.lastEdited??>(edited: ${user.getDateInUsersLocalTime(post.lastEdited)})</#if>:
    ${post.post}
    ---------------------
    </#list>
</@ircmessage>