#Works with Jython2.7b1
import re
from api.events.bot import NonCommandEvent


def handles():
    return NonCommandEvent #we want to get all the posts users make that aren't commands


def handleEvent(event):
    eventContext = event.getContext()
    postFromIrc = eventContext.getInput()

    if "wtf" in postFromIrc.lower():
        nickOfPostingUser = eventContext.getUser().getMainNick() #note that the difference between getUser and getBotUser is that the former represents the user on IRC, so it only has basic data like current nick and such, but that's all we need here
        add_wtf_for_user(nickOfPostingUser)


#As this example is mostly for show, the below isn't really perfect. We don't lock access to the file, so theoretically someone else could
#modify the file in between the read and write in this method, and then we'd lose one or more wtfs
#The code is also naive and could be simplified a lot by using something like 'pickle' probably
def add_wtf_for_user(userNick):
    pattern = re.compile(userNick + "\t(\\d+)") #regular expression for matching the line for the user
    file = open("./scripts/data/wtfs.txt", "r")
    contents = [] #save a list of all the entries from the file, so we can write it back later
    userFound = False
    for line in file:
        m = pattern.match(line) #check if this line matches for the user
        if m is not None: #yes, it's a match!
            updatedWtfs = int(m.group(1)) + 1 #add 1 to the existing count
            contents.append(userNick + "\t" + str(updatedWtfs)) #add line to the list
            userFound = True
        else:
            contents.append(line) #it wasn't the user we were looking for, so just add the line unmodified
    if not userFound: #the user isn't in the file yet, so we add a new line for him/her
        contents.append(userNick + "\t1")
    file.close()

    #write all the lines back to the file
    file = open("./scripts/data/wtfs.txt", "w")
    for line in contents:
        file.write("%s\n" % line)
    file.close()