package tools.user_activities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@AllArgsConstructor
@Getter
public class UnseenInfo {
    private final String type;
    private final int unseen;
    private final Date lastCheck;
}
