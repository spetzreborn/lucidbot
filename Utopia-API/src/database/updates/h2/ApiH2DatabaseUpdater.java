package database.updates.h2;

import api.database.updates.AbstractH2DatabaseUpdater;

public abstract class ApiH2DatabaseUpdater extends AbstractH2DatabaseUpdater {

    @Override
    public String forArtifact() {
        return "Utopia-API";
    }

}
