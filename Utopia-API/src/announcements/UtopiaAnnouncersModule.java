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

package announcements;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import spi.events.EventListener;

import javax.inject.Singleton;

public class UtopiaAnnouncersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TickAnnouncer.class).in(Singleton.class);
        bind(AidAddedAnnouncer.class).in(Singleton.class);
        bind(ArmyAddedAnnouncer.class).in(Singleton.class);
        bind(ArmyHomeAnnouncer.class).in(Singleton.class);
        bind(BuildAddedAnnouncer.class).in(Singleton.class);
        bind(EventAddedAnnouncer.class).in(Singleton.class);
        bind(EventAnnouncer.class).in(Singleton.class);
        bind(ExpiringOpsAnnouncer.class).in(Singleton.class);
        bind(ExpiringSpellsAnnouncer.class).in(Singleton.class);
        bind(IntelSavedAnnouncer.class).in(Singleton.class);
        bind(NapAddedAnnouncer.class).in(Singleton.class);
        bind(OpAddedAnnouncer.class).in(Singleton.class);
        bind(ReturningArmiesAnnouncer.class).in(Singleton.class);
        bind(SpellAddedAnnouncer.class).in(Singleton.class);
        bind(WaveAnnouncer.class).in(Singleton.class);

        Multibinder<EventListener> mBinder = Multibinder.newSetBinder(binder(), EventListener.class);
        mBinder.addBinding().to(TickAnnouncer.class);
        mBinder.addBinding().to(AidAddedAnnouncer.class);
        mBinder.addBinding().to(ArmyAddedAnnouncer.class);
        mBinder.addBinding().to(ArmyHomeAnnouncer.class);
        mBinder.addBinding().to(BuildAddedAnnouncer.class);
        mBinder.addBinding().to(EventAddedAnnouncer.class);
        mBinder.addBinding().to(EventAnnouncer.class);
        mBinder.addBinding().to(ExpiringOpsAnnouncer.class);
        mBinder.addBinding().to(ExpiringSpellsAnnouncer.class);
        mBinder.addBinding().to(IntelSavedAnnouncer.class);
        mBinder.addBinding().to(NapAddedAnnouncer.class);
        mBinder.addBinding().to(OpAddedAnnouncer.class);
        mBinder.addBinding().to(ReturningArmiesAnnouncer.class);
        mBinder.addBinding().to(SpellAddedAnnouncer.class);
        mBinder.addBinding().to(WaveAnnouncer.class);
    }
}
