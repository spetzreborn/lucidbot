package database.models;

import api.common.HasNumericId;

import java.util.Date;
import java.util.Set;

public interface BindingsContainer {
    Set<? extends HasNumericId> getUsers();

    Set<? extends HasNumericId> getRaces();

    Set<? extends HasNumericId> getPersonalities();

    Date getExpiryDate();

    Date getPublishDate();

    boolean isAdminsOnly();
}
