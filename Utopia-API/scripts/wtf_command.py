#Works with Jython2.7b1
import jarray
import re
from api.runtime import ServiceLocator
from api.database.daos import BotUserDAO
from api.commands import CommandBuilder, CommandResponse, CommandParser, ParamParsingSpecification


def handles():
    return CommandBuilder.forCommand("wtf").ofType("lol-commands").build()


def getParsers():
    emptyParser = CommandParser.getEmptyParser() #this parser will catch the case where no parameters are used, just !wtf

    userParamParsingSpec = jarray.array([ParamParsingSpecification("user", "[^ ]+")], ParamParsingSpecification) #will catch !wtf <user nick>
    specificUserParser = CommandParser(userParamParsingSpec)
    return jarray.array([emptyParser, specificUserParser], CommandParser) #convert to a java array CommandParser[]


def handleCommand(context, params, filters, delayedEventPoster):
    if params.containsKey("user"): #if the user was specified, look it up in the database
        userdao = ServiceLocator.lookup(BotUserDAO) #get one of the bot's DAOs (data access object) to help look for the user in the database
        user = userdao.getUser(params.getParameter("user")) #get the user, or None if it doesn't exist. This method looks for all nicks
        #if we want fuzzy matching of user nicks: user = userdao.getClosestMatch(params.getParameter("user"))
        if user is None: #if no such user exists, return an error message
            return CommandResponse.errorResponse("No such user exists")
    else:
        user = context.getBotUser() #if no user was specified we use the calling user, which we can fetch from the context

    wtfs = get_wtfs_for_user(user.getMainNick()) #check how many wtfs this user has posted

    return CommandResponse.resultResponse("wtfs", wtfs, "user",
                                          user) #return data for the template to work with. In this case we send the user object and the wtfs


def get_wtfs_for_user(userNick):
    pattern = re.compile(userNick + "\t(\\d+)")
    with open("./scripts/data/wtfs.txt", "r") as source_file:
        line = source_file.readline()
        m = pattern.match(line)
        if m is not None:
            return m.group(1)
    return 0