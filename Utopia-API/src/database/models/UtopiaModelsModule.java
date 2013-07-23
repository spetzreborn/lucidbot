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

package database.models;

import api.database.HibernateMapped;
import api.database.models.Alias;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class UtopiaModelsModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<HibernateMapped> binder = Multibinder.newSetBinder(binder(), HibernateMapped.class);

        binder.addBinding().toInstance(new HibernateMapped(HelpTopicCollection.class));
        binder.addBinding().toInstance(new HibernateMapped(HelpTopic.class));
        binder.addBinding().toInstance(new HibernateMapped(TickChannelMessage.class));

        binder.addBinding().toInstance(new HibernateMapped(PrivateMessage.class));
        binder.addBinding().toInstance(new HibernateMapped(Alarm.class));
        binder.addBinding().toInstance(new HibernateMapped(Notification.class));
        binder.addBinding().toInstance(new HibernateMapped(Quote.class));
        binder.addBinding().toInstance(new HibernateMapped(UserActivities.class));
        binder.addBinding().toInstance(new HibernateMapped(UserCheckIn.class));
        binder.addBinding().toInstance(new HibernateMapped(Alias.class));
        binder.addBinding().toInstance(new HibernateMapped(Note.class));

        binder.addBinding().toInstance(new HibernateMapped(Bonus.class));
        binder.addBinding().toInstance(new HibernateMapped(Dragon.class));
        binder.addBinding().toInstance(new HibernateMapped(DragonBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(DragonProject.class));
        binder.addBinding().toInstance(new HibernateMapped(DragonAction.class));
        binder.addBinding().toInstance(new HibernateMapped(Kingdom.class));
        binder.addBinding().toInstance(new HibernateMapped(Personality.class));
        binder.addBinding().toInstance(new HibernateMapped(PersonalityBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(HonorTitle.class));
        binder.addBinding().toInstance(new HibernateMapped(HonorTitleBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(Race.class));
        binder.addBinding().toInstance(new HibernateMapped(RaceBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(RaceSpellType.class));
        binder.addBinding().toInstance(new HibernateMapped(OpTypeBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(Province.class));
        binder.addBinding().toInstance(new HibernateMapped(Bindings.class));
        binder.addBinding().toInstance(new HibernateMapped(RaceBinding.class));
        binder.addBinding().toInstance(new HibernateMapped(PersonalityBinding.class));
        binder.addBinding().toInstance(new HibernateMapped(BotUserBinding.class));
        binder.addBinding().toInstance(new HibernateMapped(Aid.class));
        binder.addBinding().toInstance(new HibernateMapped(Army.class));
        binder.addBinding().toInstance(new HibernateMapped(Attack.class));
        binder.addBinding().toInstance(new HibernateMapped(Target.class));
        binder.addBinding().toInstance(new HibernateMapped(TargetHitter.class));
        binder.addBinding().toInstance(new HibernateMapped(OpType.class));
        binder.addBinding().toInstance(new HibernateMapped(OpTypeBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(SpellType.class));
        binder.addBinding().toInstance(new HibernateMapped(SpellTypeBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(UserSpellOpTarget.class));
        binder.addBinding().toInstance(new HibernateMapped(DurationSpell.class));
        binder.addBinding().toInstance(new HibernateMapped(DurationOp.class));
        binder.addBinding().toInstance(new HibernateMapped(InstantSpell.class));
        binder.addBinding().toInstance(new HibernateMapped(InstantOp.class));

        binder.addBinding().toInstance(new HibernateMapped(Building.class));
        binder.addBinding().toInstance(new HibernateMapped(BuildingFormula.class));
        binder.addBinding().toInstance(new HibernateMapped(ScienceType.class));
        binder.addBinding().toInstance(new HibernateMapped(ScienceTypeBonus.class));
        binder.addBinding().toInstance(new HibernateMapped(SoM.class));
        binder.addBinding().toInstance(new HibernateMapped(SoS.class));
        binder.addBinding().toInstance(new HibernateMapped(SoSEntry.class));
        binder.addBinding().toInstance(new HibernateMapped(SoT.class));
        binder.addBinding().toInstance(new HibernateMapped(Survey.class));
        binder.addBinding().toInstance(new HibernateMapped(SurveyEntry.class));

        binder.addBinding().toInstance(new HibernateMapped(Build.class));
        binder.addBinding().toInstance(new HibernateMapped(BuildEntry.class));
        binder.addBinding().toInstance(new HibernateMapped(OrderCategory.class));
        binder.addBinding().toInstance(new HibernateMapped(Order.class));
        binder.addBinding().toInstance(new HibernateMapped(WebLink.class));

        binder.addBinding().toInstance(new HibernateMapped(ForumSection.class));
        binder.addBinding().toInstance(new HibernateMapped(ForumThread.class));
        binder.addBinding().toInstance(new HibernateMapped(ForumPost.class));

        binder.addBinding().toInstance(new HibernateMapped(Event.class));
        binder.addBinding().toInstance(new HibernateMapped(AttendanceStatus.class));
        binder.addBinding().toInstance(new HibernateMapped(NewsItem.class));
        binder.addBinding().toInstance(new HibernateMapped(Wait.class));
    }
}
