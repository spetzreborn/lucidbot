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

import api.database.daos.NicknameDAO;
import api.database.models.Nickname;
import api.tools.text.StringUtil;
import database.daos.ProvinceDAO;
import database.models.Province;

import javax.inject.Inject;

/**
 * A class that provides way of finding the closest matching Province for any given searchterm that's either a nickname or a province name.
 */
public class BestMatchFinder {
    private final ProvinceDAO provinceDAO;
    private final NicknameDAO nicknameDAO;

    @Inject
    public BestMatchFinder(final NicknameDAO nicknameDAO, final ProvinceDAO provinceDAO) {
        this.nicknameDAO = nicknameDAO;
        this.provinceDAO = provinceDAO;
    }

    /**
     * Finds the best matching Province for the specified search term. It matches the search term against both nicknames and province names.
     * It prefers nicknames if both options are equally close to the search term.
     * <p/>
     * Examples:<br>
     * Search term: woohoo<br>
     * Closest matching province: awoohooa<br>
     * Closest matching nickname: woohoo<br>
     * Result: woohoo's province is returned, if one is registered, otherwise the province awoohooa is returned
     * <p/>
     * Search term: wooh<br>
     * Closest matching province: wooh<br>
     * Closest matching nickname: woohaaaaaaa<br>
     * Result: the province wooh is returned
     * <p/>
     * Search term: burp<br>
     * Closest matching province: woo_burp<br>
     * Closest matching nickname: none<br>
     * Result: woo_burp is returned since no nickname matched
     * <p/>
     * Returns null if no matches are found for either nickname or province name.
     *
     * @param userOrProv the search term, either a nickname or a province name
     * @return the best match as described above
     */
    public Province findBestMatch(final String userOrProv) {
        Province province = provinceDAO.getClosestMatch(userOrProv);
        return findBestMatch(province, userOrProv);
    }

    /**
     * See {@link #findBestMatch(String)}. This differs in that it only searches provinces owned by someone.
     */
    public Province findBestMatchWithOwner(final String userOrProv) {
        Province province = provinceDAO.getClosestMatchWithOwner(userOrProv);
        return findBestMatch(province, userOrProv);
    }

    private Province findBestMatch(final Province matchingProvince, final String searchTerm) {
        Nickname nick = nicknameDAO.getClosestMatch(searchTerm);
        if (matchingProvince == null && nick == null) return null;
        int provDistance =
                matchingProvince == null ? Integer.MAX_VALUE : StringUtil.getLevenshteinDistance(matchingProvince.getName(), searchTerm);
        int userDistance = nick == null ? Integer.MAX_VALUE : StringUtil.getLevenshteinDistance(nick.getName(), searchTerm);
        if (nick == null) return matchingProvince;
        else if (matchingProvince == null) return provinceDAO.getProvinceForUser(nick.getUser());
        else return provDistance < userDistance ? matchingProvince : provinceDAO.getProvinceForUser(nick.getUser());
    }
}
