package dev.osunolimits.modules.cron;

import java.sql.ResultSet;

import com.google.gson.Gson;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.utils.UserInfoCache;
import dev.osunolimits.modules.cron.engine.RunnableCronTask;
import dev.osunolimits.utils.osu.PermissionHelper;
import dev.osunolimits.utils.osu.PermissionHelper.Privileges;

public class DonatorCleanUpTask extends RunnableCronTask {

    @Override
    public String getName() {
        return "DonatorCleanUpTask";
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        try (MySQL mysql = Database.getConnection()) {

            ResultSet rs = mysql.Query("SELECT `donor_end`, `priv`, `id` FROM `users`");

            try {
                while (rs.next()) {
                    long donorEnd = rs.getLong("donor_end");
                    int priv = rs.getInt("priv");
                    int id = rs.getInt("id");
                    long currentTime = System.currentTimeMillis() / 1000L;
                    
                    UserInfoObject userInfo = UserInfoCache.getUserInfo(id);

                    boolean shouldHaveSupporter = donorEnd > currentTime;
                    boolean hasSupporterInDb = PermissionHelper.hasPrivileges(priv, Privileges.SUPPORTER);
                    boolean hasSupporterInCache = userInfo != null && PermissionHelper.hasPrivileges(userInfo.priv, Privileges.SUPPORTER);

                    // Case 1: DB has supporter but shouldn't (expired or donor_end = 0)
                    if (hasSupporterInDb && !shouldHaveSupporter) {
                        int newPriv = Privileges.removePrivilege(priv, Privileges.SUPPORTER);
                        mysql.Exec("UPDATE `users` SET `priv` = ? WHERE `id` = ?", newPriv, id);
                        
                        if (userInfo != null) {
                            userInfo.priv = newPriv;
                            App.appCache.set("shiina:user:" + id, gson.toJson(userInfo));
                        }
                        
                        logger.info("Updated user ID " + id + ": removed SUPPORTER privilege from DB and cache.");
                        continue;
                    }

                    // Case 2: Cache has supporter but DB doesn't - sync cache with DB
                    if (hasSupporterInCache && !hasSupporterInDb) {
                        userInfo.priv = priv;
                        App.appCache.set("shiina:user:" + id, gson.toJson(userInfo));
                        logger.info("Updated user ID " + id + ": synced cache with DB (removed SUPPORTER from cache).");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}