package web.models;

import api.database.models.BotUser;
import database.models.DragonAction;
import database.models.DragonProject;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

import static com.google.common.base.Objects.firstNonNull;

@XmlType(name = "DragonAction")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_DragonAction {
    /**
     * The dragon project this action belongs to. Only the id is required to send in, no complete dragon project object.
     */
    @ExistsInDB(entity = DragonProject.class, message = "No such dragon project")
    @XmlElement(required = true, name = "DragonProjectID")
    private RS_DragonProject dragonProject;

    /**
     * The user that performed the action. Only the id is required, no complete user object.
     */
    @ExistsInDB(entity = BotUser.class, message = "No such user")
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The total contribution the user has made to this project. When you report a dragon action however, you only need to set this to
     * the current contribution. It will be added to the existing total automatically.
     */
    @NotNull(message = "The contribution must not be null")
    @Min(value = 1, message = "The contribution must be a positive number")
    @XmlElement(required = true, name = "Contribution")
    private Integer contribution;

    /**
     * When the user last did something. Never needs to be specified as it's updated automatically when contributions are registered.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Updated")
    private Date updated;

    public RS_DragonAction() {
    }

    RS_DragonAction(final RS_DragonProject dragonProject, final RS_User user, final int contribution, final Date updated) {
        this.dragonProject = dragonProject;
        this.user = user;
        this.contribution = contribution;
        this.updated = updated;
    }

    public static RS_DragonAction fromDragonAction(final DragonAction action) {
        return new RS_DragonAction(RS_DragonProject.fromDragonProject(action.getDragonProject(), false), RS_User.fromBotUser(action.getUser(), false),
                action.getContribution(), action.getUpdated());
    }

    public RS_DragonProject getDragonProject() {
        return dragonProject;
    }

    public RS_User getUser() {
        return user;
    }

    public int getContribution() {
        return firstNonNull(contribution, 0);
    }

    public Date getUpdated() {
        return updated;
    }
}
