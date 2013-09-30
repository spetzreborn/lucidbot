package web.resources;

import api.database.models.BotUser;
import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import database.daos.ProvinceDAO;
import database.models.Province;
import database.models.SpellOpCharacter;
import tools.parsing.SpellsOpsParser;
import tools.target_locator.CharacterDrivenTargetLocatorFactory;
import tools.target_locator.TargetLocator;
import tools.target_locator.TargetLocatorFactory;
import web.documentation.Documentation;
import web.tools.WebContext;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.regex.Matcher;

@ValidationEnabled
@Path("spells_ops")
public class SpellsOpsResource {
    private final SpellsOpsParser spellsOpsParser;
    private final ProvinceDAO provinceDAO;
    private final CharacterDrivenTargetLocatorFactory characterDrivenTargetLocatorFactory;

    @Inject
    public SpellsOpsResource(final SpellsOpsParser spellsOpsParser,
                             final ProvinceDAO provinceDAO,
                             final CharacterDrivenTargetLocatorFactory characterDrivenTargetLocatorFactory) {
        this.spellsOpsParser = spellsOpsParser;
        this.provinceDAO = provinceDAO;
        this.characterDrivenTargetLocatorFactory = characterDrivenTargetLocatorFactory;
    }

    @Documentation("Adds any number of spells and ops (unformatted right from the game) and returns how many were parsed successfully. The 'single' boolean " +
            "flag may be used to specify if only a single op/spell is in the supplied text (if so the bot can save time by not continuing looking for matches after it finds one).")
    @POST
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public String addSpellsAndOps(@Documentation(value = "The spells and ops to add", itemName = "spellsAndOps")
                                  final String spellsAndOps,
                                  @Context
                                  final WebContext context,
                                  @Documentation("Set this to true to signal that the posted text only contains a single spell/op")
                                  @QueryParam("single")
                                  final boolean singleSpellOp,
                                  @Documentation("The target province's name. For spells and ops that contain the target name in them, or for self spells, this is " +
                                          "redundant and should be left out. In other cases it should be specified unless you want whatever target the user " +
                                          "currently has set for spells and ops to be used instead")
                                  @QueryParam("targetName")
                                  final String targetName,
                                  @Documentation("The location of the kingdom, specified on the form xx:xx (requires encoding because of the colon). " +
                                          "If this parameter is included, the bot will create the target province specified by targetName if it is missing")
                                  @QueryParam("targetKingdom")
                                  final String targetKingdom) {
        return String.valueOf(singleSpellOp ?
                spellsOpsParser.parseSingle(context.getBotUser(), spellsAndOps, createTargetLocatorFactory(targetName, targetKingdom)) :
                spellsOpsParser.parseMultiple(context.getBotUser(), spellsAndOps, createTargetLocatorFactory(targetName, targetKingdom)));
    }

    private TargetLocatorFactory createTargetLocatorFactory(final String targetName, final String kingdomLoc) {
        return new TargetLocatorFactory() {
            @Override
            public TargetLocator createLocator(final SpellOpCharacter spellOpCharacter) {
                if (targetName != null) {
                    return new TargetLocator() {
                        @Nullable
                        @Override
                        public Province locateTarget(final BotUser user, final Matcher matchedRegex) {
                            return kingdomLoc == null ? provinceDAO.getProvince(targetName) : provinceDAO.getOrCreateProvince(targetName, '(' + kingdomLoc + ')');
                        }
                    };
                } else
                    return characterDrivenTargetLocatorFactory.createLocator(spellOpCharacter);
            }
        };
    }

}
