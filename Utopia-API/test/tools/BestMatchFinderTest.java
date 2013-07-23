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
import api.database.models.BotUser;
import api.database.models.Nickname;
import database.daos.ProvinceDAO;
import database.models.Province;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Test
public class BestMatchFinderTest {
    public void testFindBestMatch() throws SQLException {
        Province awoohooa = new Province("awoohooa", null);
        Province wooh = new Province("wooh", null);
        Province woo_burp = new Province("woo_burp", null);
        Province aProvince = new Province("a", null);
        Province bProvince = new Province("b", null);

        Province userProv = new Province("userProv", null);
        BotUser botUser = new BotUser();
        userProv.setProvinceOwner(botUser);

        Nickname woohoo = new Nickname("woohoo", botUser);
        Nickname woohaaaaaaa = new Nickname("woohaaaaaaa", botUser);
        Nickname aNick = new Nickname("a", botUser);
        Nickname bNick = new Nickname("b", new BotUser());

        ProvinceDAO provinceDAO = mock(ProvinceDAO.class);
        when(provinceDAO.getClosestMatch("woohoo")).thenReturn(awoohooa);
        when(provinceDAO.getClosestMatch("wooh")).thenReturn(wooh);
        when(provinceDAO.getClosestMatch("burp")).thenReturn(woo_burp);
        when(provinceDAO.getClosestMatch("a")).thenReturn(aProvince);
        when(provinceDAO.getClosestMatch("null")).thenReturn(null);
        when(provinceDAO.getClosestMatch("b")).thenReturn(bProvince);
        NicknameDAO nicknameDAO = mock(NicknameDAO.class);
        when(nicknameDAO.getClosestMatch("woohoo")).thenReturn(woohoo);
        when(nicknameDAO.getClosestMatch("wooh")).thenReturn(woohaaaaaaa);
        when(nicknameDAO.getClosestMatch("burp")).thenReturn(null);
        when(nicknameDAO.getClosestMatch("a")).thenReturn(aNick);
        when(nicknameDAO.getClosestMatch("null")).thenReturn(null);
        when(nicknameDAO.getClosestMatch("b")).thenReturn(bNick);

        BestMatchFinder finder = new BestMatchFinder(nicknameDAO, provinceDAO);
        assertEquals(finder.findBestMatch("woohoo"), userProv);
        assertEquals(finder.findBestMatch("wooh"), wooh);
        assertEquals(finder.findBestMatch("burp"), woo_burp);
        assertEquals(finder.findBestMatch("a"), userProv);
        assertEquals(finder.findBestMatch("null"), null);
        assertEquals(finder.findBestMatch("b"), bProvince);
    }
}
