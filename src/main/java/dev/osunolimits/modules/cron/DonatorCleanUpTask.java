package dev.osunolimits.modules.cron;

import java.sql.ResultSet;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
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
        try (MySQL mysql = Database.getConnection()) {

            ResultSet rs = mysql.Query("SELECT `donor_end`, `priv`, `id` FROM `users`");

            try {
                while (rs.next()) {
                    long donorEnd = rs.getLong("donor_end");
                    int priv = rs.getInt("priv");
                    int id = rs.getInt("id");
                    long currentTime = System.currentTimeMillis() / 1000L;
                    
                    boolean shouldHaveSupporter = donorEnd > currentTime;
                    boolean hasSupporterInDb = PermissionHelper.hasPrivileges(priv, Privileges.SUPPORTER);
                    // Case 1: DB has supporter but shouldn't (expired or donor_end = 0)
                    if (hasSupporterInDb && !shouldHaveSupporter) {
                        int newPriv = Privileges.removePrivilege(priv, Privileges.SUPPORTER);
                        mysql.Exec("UPDATE `users` SET `priv` = ? WHERE `id` = ?", newPriv, id);
                        
                        logger.info("Updated user ID " + id + ": removed SUPPORTER privilege from DB.");
                        continue;
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