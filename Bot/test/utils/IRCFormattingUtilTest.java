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

package utils;

import api.irc.IRCFormatting;
import api.tools.text.IRCFormattingUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class IRCFormattingUtilTest {
    public void testRemoval() {
        Assert.assertEquals(IRCFormattingUtil.removeColors(IRCFormatting.BLACK + IRCFormatting.BLUE), "");
        Assert.assertEquals(IRCFormattingUtil.removeColors("Begin " + IRCFormatting.BLACK + "Some text" + IRCFormatting.BLUE + " And more"),
                            "Begin Some text And more");
        Assert.assertEquals(IRCFormattingUtil.removeColors("No colors here"), "No colors here");
        Assert.assertEquals(IRCFormattingUtil.removeColors(IRCFormatting.BOLD), IRCFormatting.BOLD);

        Assert.assertEquals(IRCFormattingUtil.removeFormatting(IRCFormatting.BOLD), "");
        Assert.assertEquals(IRCFormattingUtil.removeFormatting(IRCFormatting.BLACK + IRCFormatting.BLUE),
                            IRCFormatting.BLACK + IRCFormatting.BLUE);
        Assert.assertEquals(
                IRCFormattingUtil.removeFormatting("Begin " + IRCFormatting.BOLD + "Some text" + IRCFormatting.BOLD + " And more"),
                "Begin Some text And more");
    }
}
