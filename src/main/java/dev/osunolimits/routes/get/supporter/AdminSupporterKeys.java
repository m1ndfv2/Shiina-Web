package dev.osunolimits.routes.get.supporter;

import java.util.List;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class AdminSupporterKeys extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 14);

        if (!shiina.loggedIn) {
            res.redirect("/login?path=/admin/supporter-keys");
            return notFound(res, shiina);
        }

        if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        shiina.data.put("seo", new SEOBuilder("Admin • Supporter Keys", App.customization.get("homeDescription").toString()));
        shiina.data.put("pageStyles", List.of("/css/supporter-keys.css"));
        shiina.data.put("pageScripts", List.of("/js/supporter-keys.js"));

        return renderTemplate("admin/supporter-keys.html", shiina, res, req);
    }
}
