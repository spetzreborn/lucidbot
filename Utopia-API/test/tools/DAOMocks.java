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

import api.tools.collections.MapFactory;
import api.tools.text.StringUtil;
import com.google.common.collect.Lists;
import database.daos.*;
import database.models.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DAOMocks {
    private DAOMocks() {
    }

    public static ProvinceDAO getProvinceDAO(final Province... provinces) throws SQLException {
        List<Province> provs = Lists.newArrayList(provinces);
        final Map<String, Province> provinceMap = MapFactory.newNameToObjectMapping(provs);
        ProvinceDAO provinceDAO = Mockito.mock(ProvinceDAO.class);
        Mockito.when(provinceDAO.getProvince(Mockito.anyString())).then(new Answer<Province>() {
            @Override
            public Province answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return provinceMap.get(arguments[0].toString());
            }
        });
        return provinceDAO;
    }

    public static ScienceTypeDAO getScienceDAO() throws SQLException {
        ScienceTypeDAO sciDAO = Mockito.mock(ScienceTypeDAO.class);
        List<ScienceType> scienceTypes = Lists
                .newArrayList(new ScienceType("Alchemy", "Income", 1.4), new ScienceType("Tools", "Building Effectiveness", 1),
                        new ScienceType("Housing", "Population Limits", 0.65), new ScienceType("Food", "Food Production", 8),
                        new ScienceType("Military", "Gains in Combat", 1.4), new ScienceType("Crime", "Thievery Effectiveness", 6),
                        new ScienceType("Channeling", "Magic Effectiveness & Rune Production", 6));
        final Map<String, ScienceType> scienceTypeMap = MapFactory.newNameToObjectMapping(scienceTypes);
        Mockito.when(sciDAO.getAllScienceTypes()).thenReturn(scienceTypes);
        Mockito.when(sciDAO.getScienceType(Mockito.anyString())).then(new Answer<ScienceType>() {
            @Override
            public ScienceType answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return scienceTypeMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(sciDAO.getScienceTypeGroup()).thenCallRealMethod();
        return sciDAO;
    }

    public static BuildingDAO getBuildingDAO() throws SQLException {
        BuildingDAO buildingDAO = Mockito.mock(BuildingDAO.class);
        List<Building> buildings = Lists.newArrayList(new Building("Barren Land", "barren"),
                new Building("Homes", "homes", new BuildingFormula("8*#amount#", "Houses ?", null),
                        new BuildingFormula("4*#percent#*#be#/100*(1-#percent#/100)",
                                "Increases birth rates by ?%", 100.0)),
                new Building("Farms", "farms",
                        new BuildingFormula("70*#amount#*#be#/100", "Produces ? bushels", null)),
                new Building("Mills", "mills"), new Building("Banks", "banks"),
                new Building("Training Grounds", "tgs"), new Building("Armouries", "armories"),
                new Building("Military Barracks", "barracks"), new Building("Forts", "forts",
                new BuildingFormula(
                        "1.5*#percent#*#be#/100*(1-#percent#/100)",
                        "Increases DME by ?",
                        37.5)),
                new Building("Guard Stations", "gs"), new Building("Hospitals", "hospitals"),
                new Building("Guilds", "guilds"), new Building("Towers", "towers"),
                new Building("Thieves' Dens", "tds"), new Building("Watch Towers", "wts"),
                new Building("Libraries", "libraries"), new Building("Schools", "schools"),
                new Building("Stables", "stables"), new Building("Dungeons", "dungeons",
                new BuildingFormula("20*#amount#",
                        "Houses ? prisoners",
                        null)));
        final Map<String, Building> buildingsMap = MapFactory.newNameToObjectMapping(buildings);
        Mockito.when(buildingDAO.getAllBuildings()).thenReturn(buildings);
        Mockito.when(buildingDAO.getBuilding(Mockito.anyString())).then(new Answer<Building>() {
            @Override
            public Building answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return buildingsMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(buildingDAO.getBuildingGroup()).thenCallRealMethod();
        return buildingDAO;
    }

    public static RaceDAO getRaceDAO() throws SQLException {
        List<Race> races = Lists.newArrayList(new Race("Elf", "EL", "Rangers", "Archers", "Elf Lords"),
                new Race("Dwarf", "DW", "Warriors", "Axemen", "Berserkers"),
                new Race("Halfling", "HA", "Strongarms", "Slingers", "Brutes"),
                new Race("Undead", "UD", "Skeletons", "Zombies", "Ghouls"),
                new Race("Orc", "OR", "Goblins", "Trolls", "Ogres"),
                new Race("Faery", "FA", "Magicians", "Druids", "Beastmasters"),
                new Race("Avian", "AV", "Griffins", "Harpies", "Drakes"),
                new Race("Human", "HU", "Swordsmen", "Archers", "Knights"));
        final Map<String, Race> raceMap = MapFactory.newNameToObjectMapping(races);
        RaceDAO raceDAO = Mockito.mock(RaceDAO.class);
        Mockito.when(raceDAO.getAllRaces()).thenReturn(races);
        Mockito.when(raceDAO.getRace(Mockito.anyString())).then(new Answer<Race>() {
            @Override
            public Race answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return raceMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(raceDAO.getRaceGroup()).thenCallRealMethod();
        return raceDAO;
    }

    public static PersonalityDAO getPersonalityDAO() throws SQLException {
        List<Personality> personalities = Lists.newArrayList(new Personality("Merchant", "Wealthy"), new Personality("Shepherd", "Humble"),
                new Personality("Sage", "Wise"), new Personality("Rogue", "Rogue"),
                new Personality("Mystic", "Sorcerer|Sorceress"),
                new Personality("Warrior", "Warrior"),
                new Personality("Tactician", "Conniving"),
                new Personality("Cleric", "Blessed"));
        final Map<String, Personality> persMap = MapFactory.newNameToObjectMapping(personalities);
        PersonalityDAO personalityDAO = Mockito.mock(PersonalityDAO.class);
        Mockito.when(personalityDAO.getAllPersonalities()).thenReturn(personalities);
        Mockito.when(personalityDAO.getPersonality(Mockito.anyString())).then(new Answer<Personality>() {
            @Override
            public Personality answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return persMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(personalityDAO.getPersonalityGroup()).thenCallRealMethod();
        return personalityDAO;
    }

    public static HonorTitleDAO getHonorTitleDAO() throws SQLException {
        List<HonorTitle> honorTitles = Lists.newArrayList(new HonorTitle("Peasant", "Mr\\.|Mrs\\."), new HonorTitle("Knight", "Sir|Lady"),
                new HonorTitle("Lord", "Noble Lady"), new HonorTitle("Baron", "Baroness"),
                new HonorTitle("Viscount", "Viscountess"), new HonorTitle("Count", "Countess"),
                new HonorTitle("Marquis", "Marchioness"), new HonorTitle("Duke", "Duchess"),
                new HonorTitle("Prince", "Princess"));
        final Map<String, HonorTitle> titleMap = MapFactory.newNameToObjectMapping(honorTitles);
        HonorTitleDAO honorTitleDAO = Mockito.mock(HonorTitleDAO.class);
        Mockito.when(honorTitleDAO.getAllHonorTitles()).thenReturn(honorTitles);
        Mockito.when(honorTitleDAO.getHonorTitle(Mockito.anyString())).then(new Answer<HonorTitle>() {
            @Override
            public HonorTitle answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return titleMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(honorTitleDAO.getHonorTitleGroup()).thenCallRealMethod();
        return honorTitleDAO;
    }

    public static OpDAO getOpDAO() throws SQLException {
        List<OpType> opTypes = Lists
                .newArrayList(new OpType("Riots", "Causes a 10% income drop"), new OpType("Kidnappings", "Kidnaps peasants"),
                        new OpType("Spy On Throne", "Retrieves the throne page intel"));
        final Map<String, OpType> opMap = MapFactory.newNameToObjectMapping(opTypes);
        OpDAO opDAO = Mockito.mock(OpDAO.class);
        Mockito.when(opDAO.getAllOpTypes()).thenReturn(opTypes);
        Mockito.when(opDAO.getOpType(Mockito.anyString())).then(new Answer<OpType>() {
            @Override
            public OpType answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return opMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(opDAO.getOpTypeGroup()).thenCallRealMethod();
        return opDAO;
    }

    public static SpellDAO getSpellDAO() throws SQLException {
        List<SpellType> spellTypes = Lists.newArrayList(new SpellType("Fertile Lands", "Increases food production by x%"),
                new SpellType("Paradise", "Creates new land, on average 5 acres per cast"),
                new SpellType("Tornadoes", "Destroys buildings"), new SpellType("Bloodlust",
                "Causes you to kill 15% more troops and lose 5% more yourself"));
        final Map<String, SpellType> SpellMap = MapFactory.newNameToObjectMapping(spellTypes);
        SpellDAO SpellDAO = Mockito.mock(SpellDAO.class);
        Mockito.when(SpellDAO.getAllSpellTypes()).thenReturn(spellTypes);
        Mockito.when(SpellDAO.getSpellType(Mockito.anyString())).then(new Answer<SpellType>() {
            @Override
            public SpellType answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return SpellMap.get(StringUtil.capitalizeFirstLetters(arguments[0].toString(), true));
            }
        });
        Mockito.when(SpellDAO.getSpellTypeGroup()).thenCallRealMethod();
        return SpellDAO;
    }
}
