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

package database;

import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import api.tools.text.RegexUtil;
import api.tools.text.StringUtil;
import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;
import database.daos.*;
import database.models.*;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.lowerCase;
import static tools.UtopiaPropertiesConfig.CACHE_UPDATE_INTERVAL;

/**
 * This class is a cache that can be used to access common entities that are used often enough to warrant keeping them in memory
 */
@Singleton
@Log4j
@ParametersAreNonnullByDefault
public class CommonEntitiesAccess {
    private static final Pattern PIPE_PATTERN = Pattern.compile("\\|");

    private final Provider<RaceDAO> raceDAOProvider;
    private final Provider<PersonalityDAO> personalityDAOProvider;
    private final Provider<BuildingDAO> buildingDAOProvider;
    private final Provider<ScienceTypeDAO> scienceTypeDAOProvider;
    private final Provider<HonorTitleDAO> honorTitleDAOProvider;
    private final Provider<OpDAO> opDAOProvider;
    private final Provider<SpellDAO> spellDAOProvider;
    private final EventBus eventBus;

    private final Map<Class<?>, ReentrantReadWriteLock> locks = new HashMap<>();
    private final List<Race> races = new ArrayList<>();
    private final Map<String, Race> raceMap = new HashMap<>();
    private final Set<String> offSpecs = new HashSet<>();
    private final Set<String> defSpecs = new HashSet<>();
    private final Set<String> elites = new HashSet<>();
    private final List<Building> buildings = new ArrayList<>();
    private final Map<String, Building> buildingMap = new HashMap<>();
    private final List<ScienceType> sciences = new ArrayList<>();
    private final Map<String, ScienceType> scienceMap = new HashMap<>();
    private final List<Personality> personalities = new ArrayList<>();
    private final Map<String, Personality> personalityMap = new HashMap<>();
    private final List<HonorTitle> honorTitles = new ArrayList<>();
    private final Map<String, HonorTitle> honorTitleMap = new HashMap<>();
    private final List<SpellType> spellTypes = new ArrayList<>();
    private final Map<String, SpellType> spellTypeMap = new HashMap<>();
    private final List<OpType> opTypes = new ArrayList<>();
    private final Map<String, OpType> opTypeMap = new HashMap<>();

    @Inject
    public CommonEntitiesAccess(final Provider<SpellDAO> spellDAOProvider,
                                final Provider<OpDAO> opDAOProvider,
                                final Provider<HonorTitleDAO> honorTitleDAOProvider,
                                final Provider<ScienceTypeDAO> scienceTypeDAOProvider,
                                final Provider<BuildingDAO> buildingDAOProvider,
                                final Provider<PersonalityDAO> personalityDAOProvider,
                                final Provider<RaceDAO> raceDAOProvider,
                                final EventBus eventBus) {
        this.spellDAOProvider = spellDAOProvider;
        this.opDAOProvider = opDAOProvider;
        this.honorTitleDAOProvider = honorTitleDAOProvider;
        this.scienceTypeDAOProvider = scienceTypeDAOProvider;
        this.buildingDAOProvider = buildingDAOProvider;
        this.personalityDAOProvider = personalityDAOProvider;
        this.raceDAOProvider = raceDAOProvider;
        this.eventBus = eventBus;

        locks.put(Race.class, new ReentrantReadWriteLock(true));
        locks.put(Personality.class, new ReentrantReadWriteLock(true));
        locks.put(Building.class, new ReentrantReadWriteLock(true));
        locks.put(ScienceType.class, new ReentrantReadWriteLock(true));
        locks.put(HonorTitle.class, new ReentrantReadWriteLock(true));
        locks.put(OpType.class, new ReentrantReadWriteLock(true));
        locks.put(SpellType.class, new ReentrantReadWriteLock(true));
    }

    /**
     * Schedules a recurring task that flushes and refills the cache. Will generally be done by the DI framework, so it shouldn't
     * really need to be called manually
     *
     * @param threadingManager .
     * @param properties       .
     */
    @Inject
    public void scheduleUpdates(final ThreadingManager threadingManager, final PropertiesCollection properties) {
        threadingManager.scheduleRecurring(new Runnable() {
            @Override
            public void run() {
                reloadAll();
            }
        }, 100, properties.getInteger(CACHE_UPDATE_INTERVAL), TimeUnit.MINUTES);
    }

    /**
     * Updates the whole cache, for all types of entities
     */
    @Inject
    public void reloadAll() {
        reloadRaces();
        reloadBuildings();
        reloadSciences();
        reloadPersonalities();
        reloadHonorTitles();
        reloadOpTypes();
        reloadSpellTypes();
        eventBus.post(new CacheReloadEvent());
    }

    /**
     * Reloads the race cache
     */
    public void reloadRaces() {
        locks.get(Race.class).writeLock().lock();
        try {
            races.clear();
            raceMap.clear();
            offSpecs.clear();
            defSpecs.clear();
            elites.clear();
            races.addAll(raceDAOProvider.get().getAllRaces());
            for (Race race : races) {
                defSpecs.add(race.getDefSpecName());
                offSpecs.add(race.getOffSpecName());
                elites.add(race.getEliteName());
                raceMap.put(lowerCase(race.getName()), race);
                raceMap.put(lowerCase(race.getShortName()), race);
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(Race.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the building cache
     */
    public void reloadBuildings() {
        locks.get(Building.class).writeLock().lock();
        try {
            buildings.clear();
            buildingMap.clear();
            buildings.addAll(buildingDAOProvider.get().getAllBuildings());
            for (Building building : buildings) {
                buildingMap.put(lowerCase(building.getName()), building);
                buildingMap.put(lowerCase(building.getShortName()), building);
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(Building.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the science cache
     */
    public void reloadSciences() {
        locks.get(ScienceType.class).writeLock().lock();
        try {
            sciences.clear();
            scienceMap.clear();
            sciences.addAll(scienceTypeDAOProvider.get().getAllScienceTypes());
            for (ScienceType science : sciences) {
                scienceMap.put(lowerCase(science.getName()), science);
                scienceMap.put(lowerCase(science.getAngelName()), science);
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(ScienceType.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the personality cache
     */
    public void reloadPersonalities() {
        locks.get(Personality.class).writeLock().lock();
        try {
            personalities.clear();
            personalityMap.clear();
            personalities.addAll(personalityDAOProvider.get().getAllPersonalities());
            for (Personality personality : personalities) {
                personalityMap.put(lowerCase(personality.getName()), personality);
                String alias = personality.getAlias();
                if (alias != null) {
                    String[] aliases = PIPE_PATTERN.split(alias);
                    for (String ali : aliases) {
                        personalityMap.put(lowerCase(ali), personality);
                    }
                }
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(Personality.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the honor titles cache
     */
    public void reloadHonorTitles() {
        locks.get(HonorTitle.class).writeLock().lock();
        try {
            honorTitles.clear();
            honorTitleMap.clear();
            honorTitles.addAll(honorTitleDAOProvider.get().getAllHonorTitles());
            Collections.sort(honorTitles);
            for (HonorTitle title : honorTitles) {
                honorTitleMap.put(lowerCase(title.getName()), title);
                if (title.getAlias() != null) {
                    String aliasRegex = lowerCase(title.getAlias());
                    String[] split = RegexUtil.PIPE_PATTERN.split(aliasRegex);
                    for (String s : split) {
                        honorTitleMap.put(s, title);
                    }
                }
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(HonorTitle.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the op types cache
     */
    public void reloadOpTypes() {
        locks.get(OpType.class).writeLock().lock();
        try {
            opTypes.clear();
            opTypeMap.clear();
            opTypes.addAll(opDAOProvider.get().getAllOpTypes());
            for (OpType type : opTypes) {
                opTypeMap.put(lowerCase(type.getName()), type);
                if (type.getShortName() != null) opTypeMap.put(lowerCase(type.getShortName()), type);
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(OpType.class).writeLock().unlock();
        }
    }

    /**
     * Reloads the spell types cache
     */
    public void reloadSpellTypes() {
        locks.get(SpellType.class).writeLock().lock();
        try {
            spellTypes.clear();
            spellTypeMap.clear();
            spellTypes.addAll(spellDAOProvider.get().getAllSpellTypes());
            for (SpellType type : spellTypes) {
                spellTypeMap.put(lowerCase(type.getName()), type);
                if (type.getShortName() != null)
                    spellTypeMap.put(lowerCase(type.getShortName()), type);
            }
        } catch (Exception e) {
            CommonEntitiesAccess.log.error("Could not update cache of common entities", e);
        } finally {
            locks.get(SpellType.class).writeLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching races
     */
    public String getRaceGroup() {
        locks.get(Race.class).readLock().lock();
        try {
            List<String> names = new ArrayList<>(races.size() * 2);
            for (Race race : races) {
                names.add(race.getName());
                names.add(race.getShortName());
            }
            return StringUtil.merge(names, '|');
        } finally {
            locks.get(Race.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrShortName the name of the Race
     * @return the matching Race or null
     */
    public Race getRace(final String nameOrShortName) {
        locks.get(Race.class).readLock().lock();
        try {
            return raceMap.get(lowerCase(nameOrShortName));
        } finally {
            locks.get(Race.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching off specs
     */
    public String getOffSpecGroup() {
        locks.get(Race.class).readLock().lock();
        try {
            return StringUtil.merge(offSpecs, '|');
        } finally {
            locks.get(Race.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching def specs
     */
    public String getDefSpecGroup() {
        locks.get(Race.class).readLock().lock();
        try {
            return StringUtil.merge(defSpecs, '|');
        } finally {
            locks.get(Race.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching elites
     */
    public String getEliteGroup() {
        locks.get(Race.class).readLock().lock();
        try {
            return StringUtil.merge(elites, '|');
        } finally {
            locks.get(Race.class).readLock().unlock();
        }
    }

    /**
     * @return a List of all buildings
     */
    public List<Building> getAllBuildings() {
        locks.get(Building.class).readLock().lock();
        try {
            return Collections.unmodifiableList(buildings);
        } finally {
            locks.get(Building.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrShortName the name of the Building
     * @return the matching Building or null
     */
    public Building getBuilding(final String nameOrShortName) {
        locks.get(Building.class).readLock().lock();
        try {
            return buildingMap.get(lowerCase(nameOrShortName));
        } finally {
            locks.get(Building.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching buildings
     */
    public String getBuildingGroup() {
        locks.get(Building.class).readLock().lock();
        try {
            List<String> names = new ArrayList<>(buildings.size() * 2);
            for (Building building : buildings) {
                names.add(building.getName());
                if (!building.getShortName().equalsIgnoreCase(building.getName())) names.add(building.getShortName());
            }
            return StringUtil.merge(names, '|');
        } finally {
            locks.get(Building.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrAngelName the name of the science type
     * @return the matching ScienceType or null
     */
    public ScienceType getScienceType(final String nameOrAngelName) {
        locks.get(ScienceType.class).readLock().lock();
        try {
            return scienceMap.get(lowerCase(nameOrAngelName));
        } finally {
            locks.get(ScienceType.class).readLock().unlock();
        }
    }

    /**
     * @return a Collection of all science types
     */
    public Collection<ScienceType> getAllScienceTypes() {
        ReentrantReadWriteLock.ReadLock lock = locks.get(ScienceType.class).readLock();
        lock.lock();
        try {
            return new ArrayList<>(sciences);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching science types
     */
    public String getScienceTypeGroup() {
        locks.get(ScienceType.class).readLock().lock();
        try {
            List<String> names = new ArrayList<>(sciences.size() * 2);
            for (ScienceType type : sciences) {
                names.add(type.getName());
                names.add(type.getAngelName());
            }
            return StringUtil.merge(names, '|');
        } finally {
            locks.get(ScienceType.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching personalities
     */
    public String getPersonalityGroup() {
        locks.get(Personality.class).readLock().lock();
        try {
            List<String> names = new ArrayList<>();
            for (Personality personality : personalities) {
                names.add(personality.getName());
                names.add(personality.getAlias());
            }
            return StringUtil.merge(names, '|');
        } finally {
            locks.get(Personality.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrAlias the name or alias of the Personality
     * @return the matching Personality or null
     */
    public Personality getPersonality(final String nameOrAlias) {
        locks.get(Personality.class).readLock().lock();
        try {
            return personalityMap.get(lowerCase(nameOrAlias));
        } finally {
            locks.get(Personality.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrAlias the name or alias of the HonorTitle
     * @return the matching HonorTitle or null
     */
    public HonorTitle getHonorTitle(final String nameOrAlias) {
        locks.get(HonorTitle.class).readLock().lock();
        try {
            return honorTitleMap.get(lowerCase(nameOrAlias));
        } finally {
            locks.get(HonorTitle.class).readLock().unlock();
        }
    }

    /**
     * @return the lowest ranking HonorTitle
     */
    public HonorTitle getLowestRankingHonorTitle() {
        locks.get(HonorTitle.class).readLock().lock();
        try {
            return honorTitles.get(0);
        } finally {
            locks.get(HonorTitle.class).readLock().unlock();
        }
    }

    /**
     * @return a string of a regex expression for matching honor titles
     */
    public String getHonorTitleGroup() {
        locks.get(HonorTitle.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(honorTitles.size() * 3);
            for (HonorTitle honorTitle : honorTitles) {
                list.add(honorTitle.getName());
                String alias = honorTitle.getAlias();
                if (StringUtil.isNotNullOrEmpty(alias) && !alias.equalsIgnoreCase(honorTitle.getName()))
                    list.add(alias);
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(HonorTitle.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrShortName the name of the OpType
     * @return the matching OpType or null
     */
    public OpType getOpType(final String nameOrShortName) {
        locks.get(OpType.class).readLock().lock();
        try {
            return opTypeMap.get(lowerCase(nameOrShortName));
        } finally {
            locks.get(OpType.class).readLock().unlock();
        }
    }

    /**
     * @return a Collection of all op types
     */
    public Collection<OpType> getAllOpTypes() {
        ReentrantReadWriteLock.ReadLock lock = locks.get(OpType.class).readLock();
        lock.lock();
        try {
            return new ArrayList<>(opTypes);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return a Collection of all op types
     */
    public Collection<OpType> getOpTypesNotLike(final SpellOpCharacter exclude, final SpellOpCharacter... excludeMore) {
        locks.get(OpType.class).readLock().lock();
        try {
            Collection<SpellOpCharacter> excludes = EnumSet.of(exclude, excludeMore);
            List<OpType> out = new ArrayList<>(opTypes.size());
            for (OpType opType : opTypes) {
                if (!excludes.contains(opType.getOpCharacter())) out.add(opType);
            }
            return out;
        } finally {
            locks.get(OpType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the ops in the regex
     * @return a string of a regex expression for matching op types
     */
    public String getOpGroup(final boolean includeShort) {
        locks.get(OpType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(opTypes.size() * 2);
            for (OpType opType : opTypes) {
                String name = opType.getName();
                list.add(name);
                String shortName = opType.getShortName();
                if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(OpType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the ops in the regex
     * @return a string of a regex expression for matching DurationOp types
     */
    public String getDurationOpGroup(final boolean includeShort) {
        locks.get(OpType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(opTypes.size() * 2);
            for (OpType opType : opTypes) {
                if (!opType.getOpCharacter().isInstant()) {
                    String name = opType.getName();
                    list.add(name);
                    String shortName = opType.getShortName();
                    if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
                }
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(OpType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the ops in the regex
     * @return a string of a regex expression for matching InstantOp types
     */
    public String getInstantOpGroup(final boolean includeShort) {
        locks.get(OpType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(opTypes.size() * 2);
            for (OpType opType : opTypes) {
                if (opType.getOpCharacter().isInstant()) {
                    String name = opType.getName();
                    list.add(name);
                    String shortName = opType.getShortName();
                    if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
                }
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(OpType.class).readLock().unlock();
        }
    }

    /**
     * Not case sensitive
     *
     * @param nameOrShortName the name of the SpellType
     * @return the matching SpellType or null
     */
    public SpellType getSpellType(final String nameOrShortName) {
        locks.get(SpellType.class).readLock().lock();
        try {
            return spellTypeMap.get(lowerCase(nameOrShortName));
        } finally {
            locks.get(SpellType.class).readLock().unlock();
        }
    }

    /**
     * @return a Collection of all spell types
     */
    public Collection<SpellType> getAllSpellTypes() {
        ReentrantReadWriteLock.ReadLock lock = locks.get(SpellType.class).readLock();
        lock.lock();
        try {
            return new ArrayList<>(spellTypes);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return a Collection of all op types
     */
    public Collection<SpellType> getSpellTypesNotLike(final SpellOpCharacter exclude,
                                                      final SpellOpCharacter... excludeMore) {
        locks.get(SpellType.class).readLock().lock();
        try {
            Collection<SpellOpCharacter> excludes = EnumSet.of(exclude, excludeMore);
            List<SpellType> out = new ArrayList<>(spellTypes.size());
            for (SpellType spellType : spellTypes) {
                if (!excludes.contains(spellType.getSpellCharacter())) out.add(spellType);
            }
            return out;
        } finally {
            locks.get(SpellType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the spells in the regex
     * @return a string of a regex expression for matching spell types
     */
    public String getSpellGroup(final boolean includeShort) {
        locks.get(SpellType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(spellTypes.size() * 2);
            for (SpellType spellType : spellTypes) {
                String name = spellType.getName();
                list.add(name);
                String shortName = spellType.getShortName();
                if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(SpellType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the spells in the regex
     * @return a string of a regex expression for matching DurationSpell types
     */
    public String getDurationSpellGroup(final boolean includeShort) {
        locks.get(SpellType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(spellTypes.size() * 2);
            for (SpellType spellType : spellTypes) {
                if (!spellType.getSpellCharacter().isInstant()) {
                    String name = spellType.getName();
                    list.add(name);
                    String shortName = spellType.getShortName();
                    if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
                }
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(SpellType.class).readLock().unlock();
        }
    }

    /**
     * @param includeShort whether to include the short names of the spells in the regex
     * @return a string of a regex expression for matching InstantSpell types
     */
    public String getInstantSpellGroup(final boolean includeShort) {
        locks.get(SpellType.class).readLock().lock();
        try {
            List<String> list = new ArrayList<>(spellTypes.size() * 2);
            for (SpellType spellType : spellTypes) {
                if (spellType.getSpellCharacter().isInstant()) {
                    String name = spellType.getName();
                    list.add(name);
                    String shortName = spellType.getShortName();
                    if (shortName != null && !shortName.equalsIgnoreCase(name) && includeShort) list.add(shortName);
                }
            }
            return StringUtil.merge(list, '|');
        } finally {
            locks.get(SpellType.class).readLock().unlock();
        }
    }
}
