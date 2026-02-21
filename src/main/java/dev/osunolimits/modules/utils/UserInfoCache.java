package dev.osunolimits.modules.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.models.Group;
import dev.osunolimits.models.UserInfoObject;

public class UserInfoCache {

    private final static Logger log = (Logger) LoggerFactory.getLogger("RedisUserInfoCache");

    public static void populateIfNeeded() {
        // User info is read directly from MySQL, no pre-population is required.
    }


    public static UserInfoObject getUserInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        try {
            return getUserInfo(Integer.parseInt(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static UserInfoObject getUserInfo(int userId) {
        try (MySQL mysql = Database.getConnection()) {
            return getUserInfo(mysql, userId);
        } catch (SQLException e) {
            log.error("SQL Error: ", e);
            return null;
        }
    }

    public static UserInfoObject getUserInfo(MySQL mysql, int userId) {
        try {
            ResultSet userRs = mysql.Query("SELECT `id`, `name`, `safe_name`, `priv` FROM `users` WHERE `id` = ?", userId);
            if (userRs == null || !userRs.next()) {
                return null;
            }

            UserInfoObject user = new UserInfoObject();
            user.id = userRs.getInt("id");
            user.name = userRs.getString("name");
            user.safe_name = userRs.getString("safe_name");
            user.priv = userRs.getInt("priv");
            user.groups = new ArrayList<>();

            ResultSet userGroupRs = mysql.Query("SELECT `group_id` FROM `sh_groups_users` WHERE `user_id` = ?", userId);
            while (userGroupRs != null && userGroupRs.next()) {
                int groupId = userGroupRs.getInt("group_id");
                ResultSet groupRs = mysql.Query("SELECT `id`, `name`, `emoji` FROM `sh_groups` WHERE `id` = ?", groupId);
                if (groupRs != null && groupRs.next()) {
                    Group group = new Group();
                    group.id = groupRs.getInt("id");
                    group.name = groupRs.getString("name");
                    group.emoji = groupRs.getString("emoji");
                    user.groups.add(group);
                }
            }

            return user;
        } catch (SQLException e) {
            log.error("SQL Error: ", e);
            return null;
        }
    }

    public static void reloadUser(int userId) {
        // No cache to reload: method intentionally left for backward compatibility.
        getUserInfo(userId);
    }

    public static void reloadUserIfNotPresent(int userId) {
        // No cache fallback is required anymore.
        getUserInfo(userId);
    }

}
