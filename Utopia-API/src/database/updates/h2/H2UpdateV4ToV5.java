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

package database.updates.h2;

import api.database.DatabaseUpdateAction;
import api.tools.common.CleanupUtil;
import com.google.common.collect.Lists;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class H2UpdateV4ToV5 extends ApiH2DatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 5;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(new DatabaseUpdateAction() {
            @Override
            public void runDatabaseAction(final Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT DISTINCT constraint_name FROM information_schema.constraints WHERE table_name=? and column_list=?");
                statement.setString(1, "DRAGON_PROJECT");
                statement.setString(2, "TYPE");
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    String constraintName = resultSet.getString("constraint_name");

                    CleanupUtil.closeSilently(resultSet);
                    CleanupUtil.closeSilently(statement);

                    statement = connection.prepareStatement("ALTER TABLE DRAGON_PROJECT DROP CONSTRAINT " + constraintName);
                    statement.executeUpdate();
                    CleanupUtil.closeSilently(statement);
                }
            }
        });
    }
}
