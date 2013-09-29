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

package setup.tools;

import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import database.daos.BonusDAO;
import database.daos.BuildingDAO;
import database.models.*;
import spi.settings.EntitySetup;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static api.database.transactions.Transactions.inTransaction;

public class Buildings implements EntitySetup {
    public final Building homes = new Building("Homes", "homes", "amount percent be", Lists.newArrayList(
            new BuildingFormula("4*#percent#*#be#/100*(1-#percent#/100)", "Increases birth rates by ?%", 100.0),
            new BuildingFormula("8*#amount#", "Increases your maximum population by ?", null)));
    public final Building farms = new Building("Farms", "farms", "amount be",
            Lists.newArrayList(new BuildingFormula("70*#amount#*#be#/100", "Produces ? bushels", null)));
    public final Building mills = new Building("Mills", "mills", "percent be", Lists.newArrayList(
            new BuildingFormula("4*#percent#*#be#/100*(1-#percent#/100)", "Decreases build costs by ?%", 99.0),
            new BuildingFormula("3*#percent#*#be#/100*(1-#percent#/100)", "Decreases exploration costs by ?%", 75.0)));
    public final Building banks = new Building("Banks", "banks", "amount percent be", Lists.newArrayList(
            new BuildingFormula("1.25*#percent#*#be#/100*(1-#percent#/100)", "Increases income by ?%", 31.25),
            new BuildingFormula("25*#amount#", "Produces ? gcs", null)));
    public final Building tgs = new Building("Training Grounds", "tgs", "percent be", Lists.newArrayList(
            new BuildingFormula("1.5*#percent#*#be#/100*(1-#percent#/100)", "Increases OME by ?%", 37.5)));
    public final Building armories = new Building("Armouries", "arms", "percent be", Lists.newArrayList(
            new BuildingFormula("2*#percent#*#be#/100*(1-#percent#/100)", "Decreases draft costs by ?%", 50.0),
            new BuildingFormula("2*#percent#*#be#/100*(1-#percent#/100)", "Decreases wage costs by ?%", 50.0),
            new BuildingFormula("1.5*#percent#*#be#/100*(1-#percent#/100)", "Decreases training costs by ?%", 37.5)));
    public final Building rax = new Building("Military Barracks", "rax", "percent be", Lists.newArrayList(
            new BuildingFormula("1.5*#percent#*#be#/100*(1-#percent#/100)", "Decreases attack time by ?", 37.5)));
    public final Building forts = new Building("Forts", "forts", "percent be", Lists.newArrayList(
            new BuildingFormula("1.5*#percent#*#be#/100*(1-#percent#/100)", "Increases DME by ?%", 37.5)));
    public final Building gs = new Building("Guard Stations", "gs", "percent be", Lists.newArrayList(
            new BuildingFormula("2*#percent#*#be#/100*(1-#percent#/100)", "Decreases resource losses when attacked by ?%", 50.0,
                    new Bonus("GS", BonusType.GAIN, BonusApplicability.DEFENSIVELY, false, 0.0))));
    public final Building hospitals = new Building("Hospitals", "hospitals", "percent be", Lists.newArrayList(
            new BuildingFormula("2*#percent#*#be#/100*(1-#percent#/100)", "Daily chance of curing the plague ?%", 50.0),
            new BuildingFormula("3*#percent#*#be#/100*(1-#percent#/100)", "Decreases military losses by ?%", 75.0)));
    public final Building guilds = new Building("Guilds", "guilds", "amount be", Lists.newArrayList(
            new BuildingFormula("0.02*#amount#*#be#/100", "Produces ? wizards", null)));
    public final Building towers = new Building("Towers", "towers", "amount be",
            Lists.newArrayList(new BuildingFormula("12*#amount#*#be#/100", "Produces ? runes", null)));
    public final Building tds = new Building("Thieves' Dens", "tds", "percent be", Lists.newArrayList(
            new BuildingFormula("4*#percent#*#be#/100*(1-#percent#/100)", "Decreases thief losses by ?%", 95.0),
            new BuildingFormula("3*#percent#*#be#/100*(1-#percent#/100)", "Increases TPA by ?%", 75.0,
                    new Bonus("TDs", BonusType.TPA, BonusApplicability.BOTH, true, 0.0))));
    public final Building wts = new Building("Watch Towers", "wts", "percent be", Lists.newArrayList(
            new BuildingFormula("2*#percent#*#be#/100*(1-#percent#/100)", "Cactches thieves ?% of the time", 50.0),
            new BuildingFormula("3*#percent#*#be#/100*(1-#percent#/100)", "Chance of repelling individual thieves: ?%", 75.0)));
    public final Building libs = new Building("Libraries", "libraries", "percent", Lists.newArrayList(
            new BuildingFormula("2*#percent#*(1-#percent#/100)", "Increases science effects by ?%", 50.0)));
    public final Building schools = new Building("Schools", "schools", "percent be", Lists.newArrayList(
            new BuildingFormula("1.5*#percent#*#be#/100*(1-#percent#/100)", "Decreases science costs by ?%", 37.5),
            new BuildingFormula("3.5*#percent#*#be#/100*(1-#percent#/100)", "Protects against learns by ?%", 87.5)));
    public final Building stables = new Building("Stables", "stables", "amount be",
            Lists.newArrayList(new BuildingFormula("20*#amount#", "Houses ? horses", null),
                    new BuildingFormula("1*#amount#*#be#/100", "Produces ? horses", null)));
    public final Building dungeons = new Building("Dungeons", "dungeons", "amount",
            Lists.newArrayList(new BuildingFormula("20*#amount#", "Houses ? prisoners", null)));
    public final Building barren = new Building("Barren Land", "barren", "amount",
            Lists.newArrayList(new BuildingFormula("15*#amount#", "Houses ? people", null),
                    new BuildingFormula("2*#amount#", "Produces ? bushels", null)));
    public final Building unknown = new Building("Unknown", "unknown", "", Lists.newArrayList(
            new BuildingFormula("0", "Unknown effects, don't know which buildings are being constructed", null)));

    private final Provider<BuildingDAO> buildingDAOProvider;
    private final Provider<BonusDAO> bonusDAOProvider;

    @Inject
    public Buildings(final Provider<BuildingDAO> buildingDAOProvider, final Provider<BonusDAO> bonusDAOProvider) {
        this.buildingDAOProvider = buildingDAOProvider;
        this.bonusDAOProvider = bonusDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        final List<Building> objects = Lists
                .newArrayList(homes, farms, mills, banks, tgs, armories, rax, forts, gs, hospitals, guilds, towers, tds, wts, libs, schools,
                        stables, dungeons, barren, unknown);
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                BuildingDAO buildingDAO = buildingDAOProvider.get();
                BonusDAO bonusDAO = bonusDAOProvider.get();
                if (!buildingDAO.getAllBuildings().isEmpty()) return;

                Set<Bonus> bonuses = new HashSet<>();
                for (Building building : objects) {
                    for (BuildingFormula buildingFormula : building.getFormulas()) {
                        buildingFormula.setBuilding(building);
                        if (buildingFormula.getBonus() != null) bonuses.add(buildingFormula.getBonus());
                    }
                }
                bonusDAO.save(bonuses);
                buildingDAO.save(objects);
            }
        });
    }
}
