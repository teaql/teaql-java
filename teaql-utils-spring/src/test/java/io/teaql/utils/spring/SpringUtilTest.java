package io.teaql.utils.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringUtilTest {

    @Test
    public void testSpringUtilWithoutContext() {
        assertNull(SpringUtil.getBean("someBean"));
        assertNull(SpringUtil.getBean(String.class));
        assertTrue(SpringUtil.getBeansOfType(String.class).isEmpty());

        SpringUtil util = new SpringUtil();
        util.setApplicationContext(null);
        assertNull(SpringUtil.getBean("someBean"));
    }
}
