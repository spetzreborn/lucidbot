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

package commands.news;

import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;
import com.google.common.collect.Sets;
import database.models.AttackType;
import database.models.GainsSpecification;
import database.models.Kingdom;
import database.models.NewsItem;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;

import java.util.*;

import static tools.parsing.NonAttackNewsTypes.*;

@Log4j
public class SummaryView {
    private static final Collection<String> WAR_TYPE_SET = Sets.newHashSet(INCOMING_WAR_DECLARED, OUTGOING_WAR_DECLARED,
            MAX_HOSTILITY_AUTO_WAR_DECLARATION, INCOMING_WITHDRAWAL,
            INCOMING_MUTUAL_PEACE_ACCEPTED, OUTGOING_MUTUAL_PEACE_ACCEPTED, OUTGOING_WITHDRAWAL);

    private final AttackingActivities selfGains;
    private final AttackingActivities enemyGains;
    private final List<TypeToDualActivities> kdStats;
    private final Map<String, List<TypeToActivities>> provinceStats;

    public SummaryView(final AttackingActivities selfGains,
                       final AttackingActivities enemyGains,
                       final List<TypeToDualActivities> kdStats,
                       final Map<String, List<TypeToActivities>> provinceStats) {
        this.selfGains = selfGains;
        this.enemyGains = enemyGains;
        this.kdStats = kdStats;
        this.provinceStats = provinceStats;
    }

    public AttackingActivities getSelfGains() {
        return selfGains;
    }

    public AttackingActivities getEnemyGains() {
        return enemyGains;
    }

    public List<TypeToDualActivities> getKdStats() {
        return Collections.unmodifiableList(kdStats);
    }

    public List<TypeToActivities> getProvinceStats(final String provinceName) {
        return provinceStats.get(provinceName);
    }

    public static SummaryView constructFromNews(final List<NewsItem> news, final Kingdom selfKd) {
        AttackingActivities selfGains = new AttackingActivities();
        AttackingActivities enemyGains = new AttackingActivities();
        Map<AttackType, AttackingActivities> selfStats = newStatsMap();
        Map<AttackType, AttackingActivities> enemyStats = newStatsMap();
        Map<String, Map<AttackType, AttackingActivities>> provinceStats = new HashMap<>();

        String atWarWith = null;
        for (NewsItem newsItem : news) {
            String newsType = newsItem.getNewsType();
            if (newsType != null && WAR_TYPE_SET.contains(newsType)) {
                atWarWith = deriveWarStatus(newsItem, newsType);
            } else if (newsType == null) {
                //Don't care about these atm
            } else {
                AttackType attackType = AttackType.fromName(newsItem.getNewsType().replace("Outgoing ", "").replace("Incoming ", ""));
                if (attackType == null) {
                    SummaryView.log.debug("Unknown news type: " + newsItem.getNewsType() + ", expected attack type");
                    continue;
                }
                String attackingKd = StringUtil.extractPartialString(UtopiaValidationType.KDLOC.getPatternString(), newsItem.getSource());
                String attacker = newsItem.getSource().replace(attackingKd, "").trim();
                if (!provinceStats.containsKey(attacker)) provinceStats.put(attacker, newStatsMap());

                String defendingKd = StringUtil.extractPartialString(UtopiaValidationType.KDLOC.getPatternString(), newsItem.getTarget());
                String defender = newsItem.getTarget().replace(defendingKd, "").trim();
                if (!provinceStats.containsKey(defender)) provinceStats.put(defender, newStatsMap());

                boolean isWar = atWarWith != null && (atWarWith.equals(attackingKd) || atWarWith.equals(defendingKd));
                boolean isIncomingHit = defendingKd.equals(selfKd.getLocation());

                AttackType.GainVsDamage gain =
                        attackType.getGainsSpecification() == GainsSpecification.NON_LAND ? AttackType.GainVsDamage.ZERO : attackType
                                .calcGain(NumberUtil.parseInt(newsItem.getItemValue()), isWar, isIncomingHit);
                if (attackingKd.equals(selfKd.getLocation())) {
                    selfGains.addHitMade(gain.getGain(), 0);
                    enemyGains.addHitReceived(gain.getDamage(), 0);
                    selfStats.get(attackType).addHitMade(gain.getGain(), gain.getNonLandDamage());
                    enemyStats.get(attackType).addHitReceived(gain.getDamage(), gain.getNonLandDamage());
                } else {
                    enemyGains.addHitMade(gain.getGain(), 0);
                    selfGains.addHitReceived(gain.getDamage(), 0);
                    enemyStats.get(attackType).addHitMade(gain.getGain(), gain.getNonLandDamage());
                    selfStats.get(attackType).addHitReceived(gain.getDamage(), gain.getNonLandDamage());
                }
                provinceStats.get(attacker).get(attackType).addHitMade(gain.getGain(), gain.getNonLandDamage());
                provinceStats.get(defender).get(attackType).addHitReceived(gain.getDamage(), gain.getNonLandDamage());
            }
        }
        return new SummaryView(selfGains, enemyGains, mapDualActivitiesToList(selfStats, enemyStats), mapConversion(provinceStats));
    }

    private static Map<AttackType, AttackingActivities> newStatsMap() {
        Map<AttackType, AttackingActivities> map = new EnumMap<>(AttackType.class);
        for (AttackType attackType : AttackType.values()) {
            map.put(attackType, new AttackingActivities());
        }
        return map;
    }

    private static List<TypeToDualActivities> mapDualActivitiesToList(final Map<AttackType, AttackingActivities> firstMap,
                                                                      final Map<AttackType, AttackingActivities> secondMap) {
        List<TypeToDualActivities> out = new ArrayList<>(firstMap.size());
        for (AttackType type : AttackType.values()) {
            out.add(new TypeToDualActivities(type, firstMap.get(type), secondMap.get(type)));
        }
        Collections.sort(out);
        return out;
    }

    private static Map<String, List<TypeToActivities>> mapConversion(final Map<String, Map<AttackType, AttackingActivities>> map) {
        Map<String, List<TypeToActivities>> out = new HashMap<>();
        for (Map.Entry<String, Map<AttackType, AttackingActivities>> entry : map.entrySet()) {
            List<TypeToActivities> pairs = new ArrayList<>(entry.getValue().size());
            for (Map.Entry<AttackType, AttackingActivities> typeActivityMapping : entry.getValue().entrySet()) {
                pairs.add(new TypeToActivities(typeActivityMapping.getKey(), typeActivityMapping.getValue()));
            }
            if (!pairs.isEmpty()) {
                Collections.sort(pairs);
                out.put(entry.getKey(), pairs);
            }
        }
        return out;
    }

    private static String deriveWarStatus(NewsItem newsItem, String newsType) {
        switch (newsType) {
            case INCOMING_WAR_DECLARED:
                return newsItem.getSource();
            case OUTGOING_WAR_DECLARED:
            case MAX_HOSTILITY_AUTO_WAR_DECLARATION:
                return newsItem.getTarget();
            default:
                return null;
        }
    }
}
