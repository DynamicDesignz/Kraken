package com.arcaneiceman.kraken;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.arcaneiceman.kraken.domain.enumerations.WorkerType.CPU;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class KrakenApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;

    private String authWorkerToken;

    @Before
    public void prepare() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        // Authentication Token
        AccountIO.Authenticate.Request authRequest =
                new AccountIO.Authenticate.Request("wali@twotalltotems.com", "admin");
        MvcResult tokenResult = mockMvc.perform(post("/api/authenticate")
                .with(request -> {
                    request.addHeader("ContentType", "application/json");
                    return request;
                }).content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        authToken = objectMapper
                .readValue(tokenResult.getResponse().toString(), AccountIO.Authenticate.Response.class).getToken();
        // Worker Token
        WorkerIO.Augment.Request workerRequest = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        tokenResult =
                mockMvc.perform(post("/api/authenticate")
                        .with(request -> {
                            request.addHeader("ContentType", "application/json");
                            request.addHeader("Authentication", "Bearer " + authToken);
                            return request;
                        }).content(objectMapper.writeValueAsString(workerRequest)))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();
        authWorkerToken = objectMapper
                .readValue(tokenResult.getResponse().toString(), WorkerIO.Augment.Response.class).getToken();
    }


    @Test
    public void RegisterNewUser() {
        System.out.println("");
    }

    @Test
    public void RegisterNewWorker() {

    }

    @Test
    public void ExecuteMatchRequest() {

    }
}
