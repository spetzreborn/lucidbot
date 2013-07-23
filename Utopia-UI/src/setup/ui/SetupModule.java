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

package setup.ui;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import setup.ui.custom.InstallTab;
import setup.ui.custom.LazyTab;
import setup.ui.custom.VerticalLayoutLazyTab;
import setup.ui.panel.*;

public class SetupModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<InstallTab> initializerTabMultibinder = Multibinder.newSetBinder(binder(), InstallTab.class);
        initializerTabMultibinder.addBinding().to(InstallDBSettingsPanel.class);

        Multibinder<LazyTab> lazyTabMultibinder = Multibinder.newSetBinder(binder(), LazyTab.class);
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(BasicSettingsPanel.class), "Basics"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(ChannelSettingsPanel.class), "Channels"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(BotInstanceSettingsPanel.class), "Bots"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(BotUsersSettingsPanel.class), "Users"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(DragonsSettingsPanel.class), "Dragons"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(RacesSettingsPanel.class), "Races"));
        lazyTabMultibinder.addBinding()
                .toInstance(new VerticalLayoutLazyTab(getProvider(PersonalitiesSettingsPanel.class), "Personalities"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(TitlesSettingsPanel.class), "Titles"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(OpTypesSettingsPanel.class), "Ops"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(SpellTypesSettingsPanel.class), "Spells"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(ScienceTypesSettingsPanel.class), "Sciences"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(BuildingsSettingsPanel.class), "Buildings"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(BonusesSettingsPanel.class), "Bonuses"));
        lazyTabMultibinder.addBinding().toInstance(new VerticalLayoutLazyTab(getProvider(AliasesSettingsPanel.class), "Aliases"));
    }
}
