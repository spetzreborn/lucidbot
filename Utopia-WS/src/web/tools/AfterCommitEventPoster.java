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

package web.tools;

import api.database.TransactionManager;
import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for appending events to be posted to the EventBus once the current Transaction is over
 */
public class AfterCommitEventPoster implements Runnable {
    private final EventBus eventBus;
    private final List<Supplier<?>> events = new ArrayList<>();

    @Inject
    public AfterCommitEventPoster(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * @param eventProvider a supplier of an event to post later
     */
    public void addEventToPost(final Supplier<?> eventProvider) {
        if (events.isEmpty()) TransactionManager.addAfterCommitAction(this);
        events.add(eventProvider);
    }

    @Override
    public void run() {
        for (Supplier<?> eventProvider : events) {
            eventBus.post(eventProvider.get());
        }
    }
}
