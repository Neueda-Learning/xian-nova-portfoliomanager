package org.xian.protfoliomanage.Controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageControllerTest {

    @Test
    void rootRedirectsToIndex() {
        PageController controller = new PageController();

        assertEquals("redirect:/index.html", controller.root());
    }
}

