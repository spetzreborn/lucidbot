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

public class AttackingActivities {
    private int hitsMade;
    private int damageDone;
    private int landTaken;
    private int hitsReceived;
    private int damageTaken;
    private int landLost;

    public int getHitsMade() {
        return hitsMade;
    }

    public int getDamageDone() {
        return damageDone;
    }

    public int getHitsReceived() {
        return hitsReceived;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public int getLandTaken() {
        return landTaken;
    }

    public int getLandLost() {
        return landLost;
    }

    public void addHitMade(final int gain, final int damageMade) {
        ++hitsMade;
        damageDone += damageMade;
        landTaken += gain;
    }

    public void addHitReceived(final int loss, final int damageReceived) {
        ++hitsReceived;
        damageTaken += damageReceived;
        landLost += loss;
    }

    public boolean isEmpty() {
        return getTotalHits() == 0;
    }

    public int getTotalHits() {
        return hitsMade + hitsReceived;
    }

    public int getTotalResultsOut() {
        return landTaken + damageDone;
    }

    public int getTotalResultsIn() {
        return landLost + damageTaken;
    }
}
