package com.example.mini.multirole.dto;

import com.example.mini.multirole.domain.RoleCode;
import lombok.Data;

/**
 * 角色视图对象（GET /api/mini/user/roles）。
 */
@Data
public class RoleVO {

    private String code;
    private String desc;
    private int level;
    /** 是否为当前生效角色 */
    private boolean current;

    public static RoleVO from(RoleCode role, boolean current) {
        RoleVO v = new RoleVO();
        v.code = role.getCode();
        v.desc = role.getDesc();
        v.level = role.getLevel();
        v.current = current;
        return v;
    }
}
