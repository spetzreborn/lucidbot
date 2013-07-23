package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ProvinceDAO;
import database.daos.TargetDAO;
import database.models.Bindings;
import database.models.Province;
import database.models.Target;
import tools.BindingsManager;
import web.models.RS_Target;
import web.models.RS_TargetHitter;
import web.tools.BindingsParser;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static api.tools.collections.CollectionUtil.isNotEmpty;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("targets")
public class TargetResource {
    private final TargetDAO targetDAO;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<BindingsParser> bindingsParserProvider;
    private final Provider<BindingsManager> bindingsManagerProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public TargetResource(final TargetDAO targetDAO,
                          final Provider<ProvinceDAO> provinceDAOProvider,
                          final Provider<BotUserDAO> botUserDAOProvider,
                          final Provider<BindingsParser> bindingsParserProvider,
                          final Provider<BindingsManager> bindingsManagerProvider,
                          final Provider<Validator> validatorProvider) {
        this.targetDAO = targetDAO;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.bindingsParserProvider = bindingsParserProvider;
        this.bindingsManagerProvider = bindingsManagerProvider;
        this.validatorProvider = validatorProvider;
    }

    /**
     * Adds a target. Any hitters specified are added in the order they're received (i.e. the position element
     * on the objects are ignored and later updated to fit the order of the incoming list).
     *
     * @param newTarget the target to add
     * @return the added target
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Target addTarget(@Valid final RS_Target newTarget,
                               @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Province targetProvince = provinceDAOProvider.get().getProvince(newTarget.getProvince().getId());
        Target.TargetType targetType = Target.TargetType.fromName(newTarget.getType());

        Target existing = targetDAO.getTarget(targetProvince, targetType);
        if (existing != null) throw new IllegalArgumentException("That province is already a target");

        Bindings bindings = bindingsParserProvider.get().parse(newTarget.getBindings());
        Target target = new Target(targetProvince, targetType, newTarget.getDetails(), bindings);

        if (isNotEmpty(newTarget.getHitters())) {
            BotUserDAO botUserDAO = botUserDAOProvider.get();
            int position = 1;
            for (RS_TargetHitter hitter : newTarget.getHitters()) {
                BotUser user = botUserDAO.getUser(hitter.getUser().getId());
                target.insertHitter(user, position);
                ++position;
            }
        }

        target = targetDAO.save(target);
        return RS_Target.fromTarget(target, true);
    }

    /**
     * @param id the id of the target
     * @return the target with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Target getTarget(@PathParam("id") final long id) {
        Target target = targetDAO.getTarget(id);

        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Target.fromTarget(target, true);
    }

    /**
     * Returns targets for the specified user, or all available targets
     *
     * @param userId the id of the user you want to get the targets for
     * @return a list of targets
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Target>> getTargets(@QueryParam("userId") final Long userId) {
        List<RS_Target> targets = new ArrayList<>();
        if (userId == null) {
            for (Target target : targetDAO.getAllTargets()) {
                targets.add(RS_Target.fromTarget(target, true));
            }
        } else {
            BotUserDAO botUserDAO = botUserDAOProvider.get();
            BotUser user = botUserDAO.getUser(userId);
            checkNotNull(user, "There's no such user");

            List<Target> targetsForUser = targetDAO.getTargetsForUser(user, bindingsManagerProvider.get());
            for (Target target : targetsForUser) {
                targets.add(RS_Target.fromTarget(target, true));
            }
        }
        return JResponse.ok(targets).build();
    }

    /**
     * Updates a target (currently on the details)
     *
     * @param id            the id of the target to update
     * @param updatedTarget the updates
     * @return the updated target
     */
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Target updateTarget(@PathParam("id") final long id,
                                  final RS_Target updatedTarget,
                                  @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Target target = targetDAO.getTarget(id);
        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedTarget).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        target.setType(Target.TargetType.fromName(updatedTarget.getType()));
        target.setDetails(updatedTarget.getDetails());
        return RS_Target.fromTarget(target, true);
    }

    /**
     * Deletes a target
     *
     * @param id the id of the target
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteTarget(@PathParam("id") final long id,
                             @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Target target = targetDAO.getTarget(id);
        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        targetDAO.delete(target);
    }

    /**
     * Adds a hitter to a target
     *
     * @param id     the id of the target to update
     * @param hitter the hitter to add
     * @return the updated target
     */
    @Path("{id : \\d+}/hitters")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Target addHitter(@PathParam("id") final long id,
                               @Valid final RS_TargetHitter hitter,
                               @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Target target = targetDAO.getTarget(id);
        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser botUser = botUserDAOProvider.get().getUser(hitter.getUser().getId());

        if (target.containsUserAsHitter(botUser)) throw new IllegalArgumentException("That user is already a hitter");

        int currentHitters = target.getAmountOfHitters();
        int position = hitter.getPosition();
        if (position > currentHitters) target.insertHitter(botUser, currentHitters + 1);
        else target.insertHitter(botUser, position);

        return RS_Target.fromTarget(target, true);
    }

    /**
     * Moves a hitter to another position in the hit list
     *
     * @param id     the id of the target to update
     * @param hitter the hitter
     * @return the updated target
     */
    @Path("{id : \\d+}/hitters")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Target moveHitter(@PathParam("id") final long id,
                                @Valid final RS_TargetHitter hitter,
                                @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Target target = targetDAO.getTarget(id);
        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser botUser = botUserDAOProvider.get().getUser(hitter.getUser().getId());

        checkArgument(target.containsUserAsHitter(botUser), "There's no such hitter");

        int currentHitters = target.getAmountOfHitters();
        int toPosition = hitter.getPosition();
        if (toPosition > currentHitters + 1) toPosition = currentHitters;

        target.moveHitter(botUser, toPosition);

        return RS_Target.fromTarget(target, true);
    }

    /**
     * Deletes a hitter
     *
     * @param id     the id of the target
     * @param hitter the hitter to remove
     * @return the updated target
     */
    @Path("{id : \\d+}/hitters")
    @DELETE
    @Transactional
    public RS_Target deleteHitter(@PathParam("id") final long id,
                                  final RS_TargetHitter hitter,
                                  @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        checkNotNull(hitter.getUser(), "The hitter must have a user");
        checkNotNull(hitter.getUser().getId(), "The user must have an id");

        Target target = targetDAO.getTarget(id);
        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser botUser = botUserDAOProvider.get().getUser(hitter.getUser().getId());
        checkNotNull(botUser, "No such user");

        target.removeHitter(botUser);
        target = targetDAO.save(target);
        return RS_Target.fromTarget(target, true);
    }
}
