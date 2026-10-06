package com.airportrouteplanner.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

@SpringBootTest
class WebAppApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithoutPersistenceLibraries() {
        ClassLoader classLoader = applicationContext.getClassLoader();

        assertThat(applicationContext).isNotNull();
        assertThat(ClassUtils.isPresent("jakarta.persistence.Entity", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("org.flywaydb.core.Flyway", classLoader)).isFalse();
        assertThat(ClassUtils.isPresent("org.h2.Driver", classLoader)).isFalse();
    }
}
