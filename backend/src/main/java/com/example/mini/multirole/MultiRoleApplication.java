package com.example.mini.multirole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 多角色切换模块启动类。
 *
 * <p>基线：Spring Boot 3.4 + Java 17。会话/权限以内存实现演示，
 * 生产环境将 {@code SessionService}/{@code CacheService} 替换为 Sa-Token / Redis 实现即可（SRS §3.2）。</p>
 */
@SpringBootApplication
public class MultiRoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiRoleApplication.class, args);
    }
}
