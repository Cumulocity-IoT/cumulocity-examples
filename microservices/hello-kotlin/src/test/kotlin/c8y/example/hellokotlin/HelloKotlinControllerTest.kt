package c8y.example.hellokotlin

import c8y.example.hellokotlin.setup.SecurityMocksInitializer
import c8y.example.hellokotlin.setup.SecurityMocksInitializer.Companion.USER
import com.cumulocity.microservice.security.service.SecurityExpressionService
import com.cumulocity.microservice.security.service.SecurityUserDetailsService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.server.autoconfigure.ServerProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
// Spring Boot 4's @WebMvcTest slice no longer includes ServerProperties, which the microservice
// security auto-configuration requires; register it explicitly.
@EnableConfigurationProperties(ServerProperties::class)
@Import(SecurityMocksInitializer::class)
internal class HelloKotlinControllerTest(@Autowired val mockMvc: MockMvc, @Autowired val mocksInitializer: SecurityMocksInitializer) {

    @MockkBean
    private lateinit var securityExpressionService: SecurityExpressionService
    @MockkBean
    private lateinit var userDetailsService: SecurityUserDetailsService

    @BeforeEach
    fun setUp() {
        mocksInitializer.init(userDetailsService)
    }

    @Test
    @WithMockUser(username = USER)
    fun `Call hello world endpoint`() {
        // when & then
        mockMvc.perform(
                        get("/hello")
                        .queryParam("who", USER)
                        .accept(MediaType.APPLICATION_JSON)
                 )
                .andExpect(status().isOk)
                .andExpect(content().string("Hello $USER in Kotlin world!"))
    }
}
