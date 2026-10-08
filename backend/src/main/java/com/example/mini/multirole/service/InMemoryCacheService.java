package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.TerminalType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link CacheService} 内存实现（演示 / 单测）。
 * 生产替换为 Redis，key 维度建议：{@code perm:{userId}:{terminal}}、
 * {@code menu:tree:{userId}:{terminal}:{v}}、{@code bizlist:{userId}:{terminal}}（ADD §5.3）。
 */
@Service
public class InMemoryCacheService implements CacheService {

    /** key = userId@terminal -> 业务列表缓存 */
    private final Map<String, Object> businessLists = new ConcurrentHashMap<>();

    private static String key(Long userId, TerminalType terminal) {
        return userId + "@" + terminal.getCode();
    }

    @Override
    public void putBusinessList(Long userId, TerminalType terminal, Object payload) {
        businessLists.put(key(userId, terminal), payload);
    }

    @Override
    public Object getBusinessList(Long userId, TerminalType terminal) {
        return businessLists.get(key(userId, terminal));
    }

    @Override
    public void clearRoleCache(Long userId, TerminalType terminal) {
        // 清除业务列表缓存；权限/菜单在生产由缓存层按相同 key 维度统一失效
        businessLists.remove(key(userId, terminal));
    }
}
