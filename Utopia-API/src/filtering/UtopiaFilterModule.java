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

package filtering;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import filtering.filters.*;
import spi.filters.FilterBuilder;

public class UtopiaFilterModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<FilterBuilder> builders = Multibinder.newSetBinder(binder(), FilterBuilder.class);
        builders.addBinding().to(AgeFilter.Builder.class);
        builders.addBinding().to(ArchivedFilter.Builder.class);
        builders.addBinding().to(BpaFilter.Builder.class);
        builders.addBinding().to(BuildingFilter.Builder.class);
        builders.addBinding().to(CurrentDefenseFilter.Builder.class);
        builders.addBinding().to(CurrentOffenseFilter.Builder.class);
        builders.addBinding().to(DefenseFilter.Builder.class);
        builders.addBinding().to(ExpiringFilter.Builder.class);
        builders.addBinding().to(FoodFilter.Builder.class);
        builders.addBinding().to(HorsesFilter.Builder.class);
        builders.addBinding().to(KingdomLocationFilter.Builder.class);
        builders.addBinding().to(LandFilter.Builder.class);
        builders.addBinding().to(MoneyFilter.Builder.class);
        builders.addBinding().to(NetworthFilter.Builder.class);
        builders.addBinding().to(OffenseFilter.Builder.class);
        builders.addBinding().to(PeasantsFilter.Builder.class);
        builders.addBinding().to(PersonalityFilter.Builder.class);
        builders.addBinding().to(PmdFilter.Builder.class);
        builders.addBinding().to(PmoFilter.Builder.class);
        builders.addBinding().to(RaceFilter.Builder.class);
        builders.addBinding().to(ReadFilter.Builder.class);
        builders.addBinding().to(RecipientFilter.Builder.class);
        builders.addBinding().to(RunesFilter.Builder.class);
        builders.addBinding().to(ScienceBooksFilter.Builder.class);
        builders.addBinding().to(SenderFilter.Builder.class);
        builders.addBinding().to(SoldiersFilter.Builder.class);
        builders.addBinding().to(TpaFilter.Builder.class);
        builders.addBinding().to(WpaFilter.Builder.class);
    }
}
