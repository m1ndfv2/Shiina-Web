package dev.osunolimits.modules.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.models.Group;
import dev.osunolimits.models.UserInfoObject;

public class UserInfoCache {

    private final static Logger log = (Logger) LoggerFactory.getLogger("UserInfoCache");

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
        } catch (Exception e) {
            log.error("SQL Error: ", e);
            return null;
        }
    }

    public static UserInfoObject getUserInfo(MySQL mysql, String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        try {
            return getUserInfo(mysql, Integer.parseInt(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static UserInfoObject getUserInfo(MySQL mysql, int userId) {
        try {
            ResultSet rs = mysql.Query(
                    "SELECT u.`id`, u.`name`, u.`safe_name`, u.`priv`, g.`id` AS `group_id`, g.`name` AS `group_name`, g.`emoji` AS `group_emoji` " +
                            "FROM `users` u " +
                            "LEFT JOIN `sh_groups_users` gu ON gu.`user_id` = u.`id` " +
                            "LEFT JOIN `sh_groups` g ON g.`id` = gu.`group_id` " +
                            "WHERE u.`id` = ?",
                    userId);

            if (rs == null || !rs.next()) {
                return null;
            }

            UserInfoObject user = new UserInfoObject();
            user.id = rs.getInt("id");
            user.name = rs.getString("name");
            user.safe_name = rs.getString("safe_name");
            user.priv = rs.getInt("priv");
            user.groups = new ArrayList<>();

            do {
                int groupId = rs.getInt("group_id");
                if (!rs.wasNull()) {
                    Group group = new Group();
                    group.id = groupId;
                    group.name = rs.getString("group_name");
                    group.emoji = rs.getString("group_emoji");
                    user.groups.add(group);
                }
            } while (rs.next());

            return user;
        } catch (SQLException e) {
            log.error("SQL Error: ", e);
            return null;
        }
    }

    public static void reloadUser(int userId) {
        // No cache to reload: method intentionally left for backward compatibility.
    }

    public static void reloadUserIfNotPresent(int userId) {
        // No cache fallback is required anymore.
    }

}
