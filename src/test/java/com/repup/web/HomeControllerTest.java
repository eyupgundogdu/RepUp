package com.repup.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@TestPropertySource(properties = "app.base-url=https://repup.example")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageRendersInsideSharedLayout() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("<title>RepUp – Personalised strength-training planner")))
                .andExpect(content().string(containsString("<meta name=\"description\" content=\"RepUp builds")))
                .andExpect(content().string(containsString("Train with a plan.")))
                .andExpect(content().string(containsString("Skip to content")))
                .andExpect(content().string(containsString("id=\"theme-toggle\"")));
    }

    @Test
    void homePageHasSeoElements() throws Exception {
        mockMvc.perform(get("/").queryParam("utm_source", "test"))
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"https://repup.example/\">")))
                .andExpect(content().string(not(containsString("utm_source"))))
                .andExpect(content().string(containsString("\"@type\": \"Organization\"")))
                // Thymeleaf escapes "/" as "\/" inside scripts, which is still valid JSON.
                .andExpect(content().string(containsString("\"url\": \"https:\\/\\/repup.example\"")));
    }

    @Test
    void themeIsAppliedInHeadBeforeStylesheet() throws Exception {
        String html = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        int themeScript = html.indexOf("localStorage.getItem('theme')");
        int stylesheet = html.indexOf("rel=\"stylesheet\"");
        org.assertj.core.api.Assertions.assertThat(themeScript).isPositive().isLessThan(stylesheet);
    }
}
