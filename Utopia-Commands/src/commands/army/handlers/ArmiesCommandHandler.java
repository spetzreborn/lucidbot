/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package commands.army.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import api.tools.collections.Params;
import api.tools.files.FilterUtil;
import api.tools.time.DateUtil;
import database.daos.ArmyDAO;
import database.models.Army;
import database.models.Province;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

public class ArmiesCommandHandler implements CommandHandler {
    private final PropertiesCollection properties;
    private final ArmyDAO armyDAO;
    private final String commandPrefix;

    @Inject
    public ArmiesCommandHandler(final PropertiesCollection properties, final ArmyDAO armyDAO,
                                @Named(PropertiesConfig.COMMANDS_PREFIX) final String commandPrefix) {
        this.properties = properties;
        this.armyDAO = armyDAO;
        this.commandPrefix = commandPrefix;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, final Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            //TODO future possibility: allow predefined filters, for example not including faery's in !armies home
            List<Army> armyList = null;
            List<Province> provinceList = null;

            String selfKD = properties.get(UtopiaPropertiesConfig.INTRA_KD_LOC);
            if (params.isEmpty()) {
                armyList = armyDAO.getAllArmiesOutForKD(selfKD);
            } else if (params.size() == 1) {
                if (params.getIntParameter("limit") > 0) {
                    armyList = armyDAO.getArmiesOutForKD(selfKD, params.getIntParameter("limit"));
                } else if (params.getDoubleParameter("hours") > 0) {
                    long lastAllowedTime = System.currentTimeMillis() + DateUtil.hoursToMillis(params.getDoubleParameter("hours"));
                    armyList = armyDAO.getArmiesOutForKD(selfKD, new Date(lastAllowedTime));
                } else if (params.containsKey("home")) {
                    provinceList = armyDAO.getProvincesWithFullArmyHome(selfKD);
                } else if (params.getParameter("kd") != null) {
                    armyList = armyDAO.getAllArmiesOutForKD(params.getParameter("kd"));
                }
            } else if (params.size() == 2) {
                if (params.containsKey("home") && params.containsKey("full")) {
                    provinceList = new ArrayList<>();
                    provinceList.addAll(armyDAO.getProvincesWithSomeArmyHome(selfKD));
                } else if (params.containsKey("kd") && params.containsKey("limit") && params.getIntParameter("limit") > 0) {
                    armyList = armyDAO.getArmiesOutForKD(params.getParameter("kd"), params.getIntParameter("limit"));
                } else if (params.containsKey("kd") && params.containsKey("hours") && params.getDoubleParameter("hours") > 0) {
                    long lastAllowedTime = System.currentTimeMillis() + DateUtil.hoursToMillis(params.getDoubleParameter("hours"));
                    armyList = armyDAO.getArmiesOutForKD(params.getParameter("kd"), new Date(lastAllowedTime));
                } else if (params.containsKey("kd") && params.containsKey("home")) {
                    provinceList = armyDAO.getProvincesWithFullArmyHome(params.getParameter("kd"));
                }
            } else if (params.size() == 3) {
                if (params.containsKey("home") && params.containsKey("full") && params.containsKey("kd")) {
                    provinceList = new ArrayList<>();
                    provinceList.addAll(armyDAO.getProvincesWithSomeArmyHome(params.getParameter("kd")));
                }
            }

            if (armyList == null && provinceList == null)
                return CommandResponse.errorResponse("Syntax error. Check " + commandPrefix + "syntax " + context.getCommand().getName());

            List<Object> out = new ArrayList<>(10);
            out.add("home");
            out.add(params.containsKey("home"));
            out.add("full");
            out.add(params.containsKey("full"));
            out.add("kd");
            out.add(params.containsKey("kd") ? params.getParameter("kd") : selfKD);
            if (armyList != null) {
                FilterUtil.applyFilters(armyList, filters, Province.class);
                out.add("armies");
                Collections.sort(armyList);
                out.add(armyList);
            }
            if (provinceList != null) {
                FilterUtil.applyFilters(provinceList, filters);
                out.add("provinces");
                out.add(provinceList);
            }
            return CommandResponse.resultResponse(out.toArray());
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
