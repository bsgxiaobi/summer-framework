package com.bishugui.summer.io;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Properties;

import static java.time.LocalTime.*;
import static org.junit.jupiter.api.Assertions.*;

class PropertyResolverTest {
    @Test
    public void propertyValue() {
        var props = new Properties();
        props.setProperty("app.title", "Summer Framework");
        props.setProperty("app.version", "v1.0");
        props.setProperty("jdbc.url", "jdbc:mysql://localhost:3306/simpsons");
        props.setProperty("jdbc.username", "bart");
        props.setProperty("jdbc.password", "51mp50n");
        props.setProperty("jdbc.pool-size", "20");
        props.setProperty("jdbc.auto-commit", "true");
        props.setProperty("scheduler.started-at", "2023-03-29T21:45:01");
        props.setProperty("scheduler.backup-at", "03:05:10");
        props.setProperty("scheduler.cleanup", "P2DT8H21M");
        props.setProperty("app.info", "Summer Framework app info,${app.title}");
        props.setProperty("app", "Summer Framework app ,${app.info}");

        var pr = new PropertyResolver(props);

        // 测试基本获取方法
        assertEquals("Summer Framework", pr.getProperty("app.title"));
        assertEquals("v1.0", pr.getProperty("app.version"));
        assertEquals("v1.0", pr.getProperty("app.version", "unknown"));
        assertNull(pr.getProperty("app.author"));
        assertEquals("Michael Liao", pr.getProperty("app.author", "Michael Liao"));

        assertTrue(pr.getProperty("jdbc.auto-commit", boolean.class));
        assertEquals(Boolean.TRUE, pr.getProperty("jdbc.auto-commit", Boolean.class));
        assertTrue(pr.getProperty("jdbc.detect-leak", boolean.class, true));

        assertEquals(20, pr.getProperty("jdbc.pool-size", int.class));
        assertEquals(20, pr.getProperty("jdbc.pool-size", int.class, 999));
        assertEquals(5, pr.getProperty("jdbc.idle", int.class, 5));

        assertEquals(LocalDateTime.parse("2023-03-29T21:45:01"), pr.getProperty("scheduler.started-at", LocalDateTime.class));
        assertEquals(parse("03:05:10"), pr.getProperty("scheduler.backup-at", LocalTime.class));
        assertEquals(parse("23:59:59"), pr.getProperty("scheduler.restart-at", LocalTime.class, parse("23:59:59")));
        assertEquals(Duration.ofMinutes((2 * 24 + 8) * 60 + 21), pr.getProperty("scheduler.cleanup", Duration.class));

        // 测试表达式
        assertEquals("Summer Framework", pr.getProperty("${app.title}"));
        assertEquals("app2", pr.getProperty("${app1:${app2:app2}}"));
        assertEquals("Summer Framework", pr.getProperty("${app.a}","${app.title}"));

        // 测试嵌套表达式
        assertEquals("title1", pr.getProperty("${app.a}","${app.title1:title1}"));
    }
}