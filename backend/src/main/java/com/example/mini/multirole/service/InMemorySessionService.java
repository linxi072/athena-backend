package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SessionService} 的内存实现（演示 / 单测用）。
 *
 * <p>真实部署替换为 Sa-Token：{@code StpUtil.login(userId, terminal)} 与
 * {@code StpUtil.switchTokenToRole(...)}，并保持“按端隔离”语义不变。</p>
 */
@Service
public class InMemorySessionService implements SessionService {

    /** 端会话：key = userId@terminal */
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    /** token -> key 反向索引，便于鉴权时定位 */
    private final Map<String, String> tokenIndex = new ConcurrentHashMap<>();

    private static String key(Long userId, TerminalType terminal) {
        return userId + "@" + terminal.getCode();
    }

    @Override
    public synchronized String login(Long userId, TerminalType terminal, RoleCode currentRole) {
        String k = key(userId, terminal);
        String token = "tok-" + k + "-" + System.nanoTime();
        sessions.put(k, new SessionInfo(token, currentRole));
        tokenIndex.put(token, k);
        return token;
    }

    @Override
    public synchronized String switchRoleInSession(Long userId, TerminalType terminal, RoleCode newRole) {
        String k = key(userId, terminal);
        SessionInfo old = sessions.get(k);
        if (old == null) {
            return login(userId, terminal, newRole); // 端未登录则直接登录态建立
        }
        String token = "tok-" + k + "-" + System.nanoTime();
        sessions.put(k, new SessionInfo(token, newRole));
        tokenIndex.remove(old.token());
        tokenIndex.put(token, k);
        return token;
    }

    @Override
    public RoleCode getCurrentRole(Long userId, TerminalType terminal) {
        SessionInfo info = sessions.get(key(userId, terminal));
        return info == null ? null : info.role();
    }

    @Override
    public boolean isLoggedIn(Long userId, TerminalType terminal) {
        return sessions.containsKey(key(userId, terminal));
    }

    @Override
    public synchronized void invalidateAll(Long userId) {
        for (TerminalType t : TerminalType.values()) {
            SessionInfo info = sessions.remove(key(userId, t));
            if (info != null) {
                tokenIndex.remove(info.token());
            }
        }
    }

    /** 会话内部态（token + 当前角色）。使用 record 便于不可变快照。 */
    public record SessionInfo(String token, RoleCode role) {
    }
}
