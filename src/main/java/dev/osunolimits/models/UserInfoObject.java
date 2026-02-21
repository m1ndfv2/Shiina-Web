package dev.osunolimits.models;

import java.util.List;

import dev.osunolimits.modules.utils.UserInfoCache;
import lombok.Data;

@Data
public class UserInfoObject {
    public UserInfoObject() {
    }

    public UserInfoObject(int id) {
        UserInfoObject userInfo = UserInfoCache.getUserInfo(id);
        if (userInfo == null) {
            return;
        }
        this.id = userInfo.id;
        this.name = userInfo.name;
        this.safe_name = userInfo.safe_name;
        this.priv = userInfo.priv;
        this.groups = userInfo.groups;
    }

    public int id;
    public String name;
    public String safe_name;
    public int priv;
    public List<Group> groups;
}
