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

package tools;

import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;

@Test
public class CommonEntitiesAccessTest {
    public void testRaceRelated() throws SQLException {
        final RaceDAO dao = DAOMocks.getRaceDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, null, null, null, null, null, new Provider<RaceDAO>() {
            @Override
            public RaceDAO get() {
                return dao;
            }
        }, new EventBus());

        access.reloadRaces();

        Assert.assertEquals(access.getRaceGroup(), dao.getRaceGroup());
        Assert.assertEquals(access.getOffSpecGroup(), "Rangers|Warriors|Strongarms|Skeletons|Goblins|Magicians|Griffins|Swordsmen");
        Assert.assertEquals(access.getDefSpecGroup(), "Archers|Axemen|Slingers|Zombies|Trolls|Druids|Harpies|Archers");
        Assert.assertEquals(access.getEliteGroup(), "Elf Lords|Berserkers|Brutes|Ghouls|Ogres|Beastmasters|Drakes|Knights");
        Assert.assertEquals(access.getRace("HU"), dao.getRace("Human"));
        Assert.assertEquals(access.getRace("Orc"), dao.getRace("Orc"));
        Assert.assertEquals(access.getRace("oRC"), dao.getRace("Orc"));
        Assert.assertNull(access.getRace("NonExistant"));
    }

    public void testBuildingRelated() throws SQLException {
        final BuildingDAO dao = DAOMocks.getBuildingDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, null, null, null, new Provider<BuildingDAO>() {
            @Override
            public BuildingDAO get() {
                return dao;
            }
        }, null, null, new EventBus());

        access.reloadBuildings();

        Assert.assertEquals(access.getBuildingGroup(), dao.getBuildingGroup());
        Assert.assertEquals(access.getAllBuildings(), dao.getAllBuildings());
        Assert.assertEquals(access.getBuilding("Farms"), dao.getBuilding("Farms"));
        Assert.assertEquals(access.getBuilding("wts"), dao.getBuilding("Watch Towers"));
        Assert.assertEquals(access.getBuilding("wATCH tOWERS"), dao.getBuilding("Watch Towers"));
        Assert.assertNull(access.getBuilding("NonExistant"));
    }

    public void testScienceRelated() throws SQLException {
        final ScienceTypeDAO dao = DAOMocks.getScienceDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, null, null, new Provider<ScienceTypeDAO>() {
            @Override
            public ScienceTypeDAO get() {
                return dao;
            }
        }, null, null, null, new EventBus());

        access.reloadSciences();

        Assert.assertEquals(access.getScienceTypeGroup(), dao.getScienceTypeGroup());
        Assert.assertEquals(access.getAllScienceTypes(), dao.getAllScienceTypes());
        Assert.assertEquals(access.getScienceType("Alchemy"), dao.getScienceType("Alchemy"));
        Assert.assertEquals(access.getScienceType("Magic Effectiveness & Rune Production"), dao.getScienceType("Channeling"));
        Assert.assertEquals(access.getScienceType("fOOD"), dao.getScienceType("Food"));
        Assert.assertNull(access.getScienceType("NonExistant"));
    }

    public void testPersonalityRelated() throws SQLException {
        final PersonalityDAO dao = DAOMocks.getPersonalityDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, null, null, null, null, new Provider<PersonalityDAO>() {
            @Override
            public PersonalityDAO get() {
                return dao;
            }
        }, null, new EventBus());

        access.reloadPersonalities();

        Assert.assertEquals(access.getPersonalityGroup(), dao.getPersonalityGroup());
        Assert.assertEquals(access.getPersonality("Merchant"), dao.getPersonality("Merchant"));
        Assert.assertEquals(access.getPersonality("Humble"), dao.getPersonality("Shepherd"));
        Assert.assertEquals(access.getPersonality("shepHERD"), dao.getPersonality("Shepherd"));
        Assert.assertNull(access.getPersonality("NonExistant"));
    }

    public void testHonorTitleRelated() throws SQLException {
        final HonorTitleDAO dao = DAOMocks.getHonorTitleDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, null, new Provider<HonorTitleDAO>() {
            @Override
            public HonorTitleDAO get() {
                return dao;
            }
        }, null, null, null, null, new EventBus());

        access.reloadHonorTitles();

        Assert.assertEquals(access.getHonorTitleGroup(), dao.getHonorTitleGroup());
        Assert.assertEquals(access.getHonorTitle("Knight"), dao.getHonorTitle("Knight"));
        Assert.assertEquals(access.getHonorTitle("Mrs."), dao.getHonorTitle("Peasant"));
        Assert.assertEquals(access.getHonorTitle("dUKE"), dao.getHonorTitle("Duke"));
        Assert.assertEquals(access.getLowestRankingHonorTitle(), dao.getHonorTitle("Peasant"));
        Assert.assertNull(access.getHonorTitle("NonExistant"));
    }

    public void testOpRelated() throws SQLException {
        final OpDAO dao = DAOMocks.getOpDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(null, new Provider<OpDAO>() {
            @Override
            public OpDAO get() {
                return dao;
            }
        }, null, null, null, null, null, new EventBus());

        access.reloadOpTypes();

        Assert.assertEquals(access.getAllOpTypes(), dao.getAllOpTypes());
        Assert.assertEquals(access.getOpGroup(true), dao.getOpTypeGroup());
        //TODO test the DurationOpGroup and InstantOpGroup too
        Assert.assertEquals(access.getOpType("Riots"), dao.getOpType("Riots"));
        Assert.assertEquals(access.getOpType("Spy On Throne"), dao.getOpType("Spy On Throne"));
        Assert.assertEquals(access.getOpType("kidNaPPINGS"), dao.getOpType("Kidnappings"));
        Assert.assertNull(access.getOpType("NonExistant"));
    }

    public void testSpellRelated() throws SQLException {
        final SpellDAO dao = DAOMocks.getSpellDAO();
        CommonEntitiesAccess access = new CommonEntitiesAccess(new Provider<SpellDAO>() {
            @Override
            public SpellDAO get() {
                return dao;
            }
        }, null, null, null, null, null, null, new EventBus());

        access.reloadSpellTypes();

        Assert.assertEquals(access.getAllSpellTypes(), dao.getAllSpellTypes());
        Assert.assertEquals(access.getSpellGroup(true), dao.getSpellTypeGroup());
        //TODO test the DurationSpellGroup and InstantSpellGroup too
        Assert.assertEquals(access.getSpellType("Fertile Lands"), dao.getSpellType("Fertile Lands"));
        Assert.assertEquals(access.getSpellType("Tornadoes"), dao.getSpellType("Tornadoes"));
        Assert.assertEquals(access.getSpellType("blooDLUST"), dao.getSpellType("Bloodlust"));
        Assert.assertNull(access.getSpellType("NonExistant"));
    }
}
