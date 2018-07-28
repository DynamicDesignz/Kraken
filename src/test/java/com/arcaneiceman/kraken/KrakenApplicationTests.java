package com.arcaneiceman.kraken;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.PasswordList;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.embedded.JobDelimiter;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.repository.PasswordListRepository;
import com.arcaneiceman.kraken.repository.RequestRepository;
import com.arcaneiceman.kraken.repository.UserRepository;
import com.arcaneiceman.kraken.repository.WorkerRepository;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.ArrayList;

import static com.arcaneiceman.kraken.domain.enumerations.WorkerType.CPU;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class KrakenApplicationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private PasswordListRepository passwordListRepository;
    @Autowired
    private WorkerRepository workerRepository;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void prepare() {
        // Add User
        userRepository.save(new User(null, "test@test.com", "test-first-name", "test-last-name",
                passwordEncoder.encode("helloworld"),
                null, null, true, AuthoritiesConstants.CONSUMER));

        // Add Password List
        passwordListRepository.save(new PasswordList("test.txt",
                "UTF-8",
                new ArrayList<JobDelimiter>() {{
                    add(new JobDelimiter(0, 0L, 2105248L));
                    add(new JobDelimiter(1, 2105248L, 2818049L));
                }}));
    }


    @Test
    public void RegisterNewUser() {

    }

    @Test
    public void RegisterNewWorker() {

    }

    @Test
    public void attemptToWorkerLoginWithoutWorkerExisting() {

    }

    @Test
    public void workerAttemptToGetMoreThanOneJob() {

    }

    @Test
    public void workerCallsWithoutWorkerToken() {

    }

    @Test
    public void workerGetJobWhenOffline() {

    }

    @Test
    public void RemoveWorkerWhileItHasJob() {

    }

    @Test
    public void LetWorkerTimeOutWithoutJob() {

    }

    @Test
    public void LetWorkerTimeoutWithJob() {

    }

    @Test
    public void NoRequestNoJob() {

    }

    @Test
    public void ExecuteMatchRequest() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Request Successfully
        RequestIO.Create.Request bb = new RequestIO.Create.Request(
                RequestType.MATCH,
                new MatchRequestDetail("hellohello"),
                new ArrayList<String>() {{
                    add("test.txt");
                }},
                new ArrayList<RequestIO.Create.Request.CrunchParams>() {{
                    add(new RequestIO.Create.Request.CrunchParams(4, 4, "abcdefghijklmnopqrstuvwxyz0123456789", "aaaa"));
                }});
        Request request = createMatchRequest(bb, authToken, status().is2xxSuccessful());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Worker Login Successfully
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        String workerAuthToken = workerLogin(c, authToken, status().isOk());

        // Worker Heartbeat Successfully
        workerHeartbeat(workerAuthToken, status().is2xxSuccessful());

        for (int i = 0; i < 11; i++) {
            // Get Job Successfully
            RequestIO.GetJob.Response jobResponse = getJob(workerAuthToken, status().is2xxSuccessful());

            // Report Job Complete Successfully
            RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                    jobResponse.getRequestId(),
                    jobResponse.getListId(),
                    jobResponse.getJobId(),
                    TrackingStatus.COMPLETE,
                    null);
            reportJob(d, workerAuthToken, status().is2xxSuccessful());
        }

        // Request Should be complete (not exist)
        Assert.assertFalse(requestRepository.existsById(request.getId()));
    }

    @Test
    public void ExecuteMatchRequestFound() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Request Successfully
        RequestIO.Create.Request bb = new RequestIO.Create.Request(
                RequestType.MATCH,
                new MatchRequestDetail("beautiful"),
                new ArrayList<String>() {{
                    add("test.txt");
                }},
                new ArrayList<RequestIO.Create.Request.CrunchParams>() {{
                    add(new RequestIO.Create.Request.CrunchParams(4, 4, "abcdefghijklmnopqrstuvwxyz0123456789", "aaaa"));
                }});
        Request request = createMatchRequest(bb, authToken, status().is2xxSuccessful());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Worker Login Successfully
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        String workerAuthToken = workerLogin(c, authToken, status().isOk());

        // Worker Heartbeat Successfully
        workerHeartbeat(workerAuthToken, status().is2xxSuccessful());

        // Get Job Successfully
        RequestIO.GetJob.Response jobResponse = getJob(workerAuthToken, status().is2xxSuccessful());

        // Report Job Complete Successfully
        RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                jobResponse.getRequestId(),
                jobResponse.getListId(),
                jobResponse.getJobId(),
                TrackingStatus.COMPLETE,
                "beautiful");
        reportJob(d, workerAuthToken, status().is2xxSuccessful());

        // Request Should be complete (not exist)
        Assert.assertFalse(requestRepository.existsById(request.getId()));
    }

    @Test
    public void ExecuteMatchRequestConcurrentWorkers() throws Exception {

    }

    @Test
    public void ExecuteWPARequest() throws Exception {

    }

    @Test
    public void ExhaustJobs() throws Exception {

    }

    @Test
    public void ReportPasswordListJobError() {

    }

    @Test
    public void ReportCrunchListJobError() {

    }

    @Test
    public void RemovePasswordListWhileRunningJob() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Request Successfully
        RequestIO.Create.Request bb = new RequestIO.Create.Request(
                RequestType.MATCH,
                new MatchRequestDetail("beautiful"),
                new ArrayList<String>() {{
                    add("test.txt");
                }},
                new ArrayList<>());
        Request request = createMatchRequest(bb, authToken, status().is2xxSuccessful());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Worker Login Successfully
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        String workerAuthToken = workerLogin(c, authToken, status().isOk());

        // Worker Heartbeat Successfully
        workerHeartbeat(workerAuthToken, status().is2xxSuccessful());

        // Get Job Successfully
        RequestIO.GetJob.Response jobResponse = getJob(workerAuthToken, status().is2xxSuccessful());

        // Report Job Complete Successfully
        RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                jobResponse.getRequestId(),
                jobResponse.getListId(),
                jobResponse.getJobId(),
                TrackingStatus.COMPLETE,
                "beautiful");
        reportJob(d, workerAuthToken, status().is2xxSuccessful());

        // Delete PasswordList
        passwordListRepository.deleteAll();

        // Get Job Successfully
        getJob(workerAuthToken, status().is4xxClientError());
    }

    @Test
    public void tooManyCrunchJobs() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Request Unsuccessfully
        RequestIO.Create.Request bb = new RequestIO.Create.Request(
                RequestType.MATCH,
                new MatchRequestDetail("beautiful"),
                new ArrayList<>(),
                new ArrayList<RequestIO.Create.Request.CrunchParams>() {{
                    add(new RequestIO.Create.Request.CrunchParams(4, 16, "abcdefghijklmnopqrstuvwxyz0123456789", "aaaa"));
                }});
        createMatchRequest(bb, authToken, status().is4xxClientError());
    }

    @Test
    public void wrongBadWPACapFile() {

    }

    private String login(AccountIO.Authenticate.Request authRequest, ResultMatcher resultMatcher) throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/api/account/authenticate")
                .with(request -> {
                    request.addHeader("Content-Type", "application/json");
                    return request;
                }).content(mapper.writeValueAsString(authRequest)))
                .andExpect(resultMatcher)
                .andReturn();
        return mapper.readValue(tokenResult.getResponse().getContentAsString(), AccountIO.Authenticate.Response.class).getToken();
    }

    private Worker createWorker(WorkerIO.Create.Request req, String authToken, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/worker")
                .with(request -> {
                    request.addHeader("Authorization", "Bearer " + authToken);
                    request.addHeader("Content-Type", "application/json");
                    return request;
                }).content(mapper.writeValueAsString(req)))
                .andExpect(resultMatcher)
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), Worker.class);
    }

    private String workerLogin(WorkerIO.Augment.Request workerRequest, String authToken, ResultMatcher resultMatcher) throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/api/worker/augment-token")
                .with(request -> {
                    request.addHeader("Content-Type", "application/json");
                    request.addHeader("Authorization", "Bearer " + authToken);
                    return request;
                }).content(mapper.writeValueAsString(workerRequest)))
                .andExpect(resultMatcher)
                .andReturn();
        return mapper.readValue(tokenResult.getResponse().getContentAsString(), WorkerIO.Augment.Response.class).getToken();
    }

    private void workerHeartbeat(String workerAuthToken, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(post("/api/worker/heartbeat")
                .with(request -> {
                    request.addHeader("Content-Type", "application/json");
                    request.addHeader("Authorization", "Bearer " + workerAuthToken);
                    return request;
                }))
                .andExpect(resultMatcher);
    }

    private Request createMatchRequest(RequestIO.Create.Request req, String authToken, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests")
                .with(request -> {
                    request.addHeader("Authorization", "Bearer " + authToken);
                    return request;
                }).param("details", mapper.writeValueAsString(req)))
                .andExpect(resultMatcher)
                .andReturn();
        if (!resultMatcher.equals(status().is2xxSuccessful()))
            return null;
        mapper.addMixIn(Request.class, IgnoreRequestDetailsMixIn.class);
        return mapper.readValue(result.getResponse().getContentAsString(), Request.class);
    }

    private Request getRequest(Long id, String authToken, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/requests/" + id.toString())
                .with(request -> {
                    request.addHeader("Authorization", "Bearer " + authToken);
                    return request;
                }))
                .andExpect(resultMatcher)
                .andReturn();
        mapper.addMixIn(Request.class, IgnoreRequestDetailsMixIn.class);
        return mapper.readValue(result.getResponse().getContentAsString(), Request.class);
    }

    abstract class IgnoreRequestDetailsMixIn {
        @JsonIgnore
        public RequestDetail requestDetail;
    }

    private RequestIO.GetJob.Response getJob(String workerAuthToken, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests/get-job")
                .with(request -> {
                    request.addHeader("Authorization", "Bearer " + workerAuthToken);
                    return request;
                })).andExpect(resultMatcher)
                .andReturn();
        if (!resultMatcher.equals(status().is2xxSuccessful()))
            return null;
        return mapper.readValue(result.getResponse().getContentAsString(), RequestIO.GetJob.Response.class);
    }

    private void reportJob(RequestIO.ReportJob.Request req,
                           String workerAuthToken,
                           ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(post("/api/requests/report-job")
                .with(request -> {
                    request.addHeader("Authorization", "Bearer " + workerAuthToken);
                    request.addHeader("Content-Type", "application/json");
                    return request;
                }).content(mapper.writeValueAsString(req)))
                .andExpect(resultMatcher);
    }

}
