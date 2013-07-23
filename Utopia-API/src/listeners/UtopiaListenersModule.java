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

package listeners;

import api.events.DirectoryChangeEventObserver;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import spi.events.EventListener;

import javax.inject.Singleton;

public class UtopiaListenersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(NewAidRequestListener.class).in(Singleton.class);
        bind(InfiltrationListener.class).in(Singleton.class);
        bind(NewAttacksListener.class).in(Singleton.class);
        bind(NewBuildsListener.class).in(Singleton.class);
        bind(NewDragonActionsListener.class).in(Singleton.class);
        bind(NewEventsListener.class).in(Singleton.class);
        bind(NewWaveListener.class).in(Singleton.class);
        bind(NewNapsListener.class).in(Singleton.class);
        bind(NewNotesListener.class).in(Singleton.class);
        bind(NewIntelArmiesListener.class).in(Singleton.class);
        bind(NewOrdersListener.class).in(Singleton.class);
        bind(NewSpellsOpsListener.class).in(Singleton.class);
        bind(NewTargetsListener.class).in(Singleton.class);
        bind(AlarmSetOffListener.class).in(Singleton.class);
        bind(ReturningArmiesListener.class).in(Singleton.class);
        bind(UserActivitiesListener.class).in(Singleton.class);
        bind(UserCheckInsListener.class).in(Singleton.class);
        bind(UserLoginListener.class).in(Singleton.class);
        bind(UserWelcomeListener.class).in(Singleton.class);
        bind(UserReminderListener.class).in(Singleton.class);

        bind(AidManager.class).in(Singleton.class);
        bind(AlarmManager.class).in(Singleton.class);
        bind(ArmyManager.class).in(Singleton.class);
        bind(SpellsOpsManager.class).in(Singleton.class);
        bind(ScriptManager.class).in(Singleton.class);
        bind(EventManager.class).in(Singleton.class);
        bind(TickManager.class).in(Singleton.class);

        Multibinder<EventListener> multibinder = Multibinder.newSetBinder(binder(), EventListener.class);
        multibinder.addBinding().to(NewAidRequestListener.class);
        multibinder.addBinding().to(InfiltrationListener.class);
        multibinder.addBinding().to(NewAttacksListener.class);
        multibinder.addBinding().to(NewBuildsListener.class);
        multibinder.addBinding().to(NewDragonActionsListener.class);
        multibinder.addBinding().to(NewEventsListener.class);
        multibinder.addBinding().to(NewWaveListener.class);
        multibinder.addBinding().to(NewNapsListener.class);
        multibinder.addBinding().to(NewNotesListener.class);
        multibinder.addBinding().to(NewIntelArmiesListener.class);
        multibinder.addBinding().to(NewOrdersListener.class);
        multibinder.addBinding().to(NewSpellsOpsListener.class);
        multibinder.addBinding().to(NewTargetsListener.class);
        multibinder.addBinding().to(AlarmSetOffListener.class);
        multibinder.addBinding().to(ReturningArmiesListener.class);
        multibinder.addBinding().to(UserActivitiesListener.class);
        multibinder.addBinding().to(UserCheckInsListener.class);
        multibinder.addBinding().to(UserLoginListener.class);
        multibinder.addBinding().to(UserWelcomeListener.class);
        multibinder.addBinding().to(UserReminderListener.class);

        multibinder.addBinding().to(AidManager.class);
        multibinder.addBinding().to(AlarmManager.class);
        multibinder.addBinding().to(ArmyManager.class);
        multibinder.addBinding().to(SpellsOpsManager.class);
        multibinder.addBinding().to(ScriptManager.class);
        multibinder.addBinding().to(EventManager.class);
        multibinder.addBinding().to(TickManager.class);

        bind(ScriptManager.class).in(Singleton.class);

        Multibinder<DirectoryChangeEventObserver> multibinder2 = Multibinder.newSetBinder(binder(), DirectoryChangeEventObserver.class);
        multibinder2.addBinding().to(ScriptManager.class);
    }
}
