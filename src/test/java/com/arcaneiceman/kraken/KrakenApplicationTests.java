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
import org.junit.After;
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

    @After
    public void clean() {
        requestRepository.deleteAll();
        passwordListRepository.deleteAll();
        workerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void DuplicateUserFail() throws Exception {
        // Register User
        AccountIO.Register.Request a1 = new AccountIO.Register.Request(
                "test@test.com",
                "helloworld",
                "firstname",
                "lastname");
        registerUser(a1, status().is4xxClientError());
    }

    @Test
    public void RegisterNewUser() throws Exception {
        // Register User
        AccountIO.Register.Request a1 = new AccountIO.Register.Request(
                "test2@test.com",
                "helloworld",
                "firstname",
                "lastname");
        registerUser(a1, status().is2xxSuccessful());
    }

    @Test
    public void DuplicateWorkerFail() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        WorkerIO.Create.Request b1 = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b1, authToken, status().is2xxSuccessful());

        WorkerIO.Create.Request b2 = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b2, authToken, status().is4xxClientError());
    }

    @Test
    public void AttemptToWorkerLoginWithoutWorkerExisting() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Worker Login Failure
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        workerLogin(c, authToken, status().is4xxClientError());
    }

    @Test
    public void workerAttemptToGetMoreThanOneJob() throws Exception {
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

        // Get Job Successfully
        getJob(workerAuthToken, status().is2xxSuccessful());

        // Get Job Failure
        getJob(workerAuthToken, status().is4xxClientError());
    }

    @Test
    public void workerCallsWithoutWorkerToken() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Get Job Should return error (no heartbeat)
        getJob(authToken, status().is4xxClientError());
    }

    @Test
    public void workerGetJobWhenOffline() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Worker Login Successfully
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        String workerAuthToken = workerLogin(c, authToken, status().isOk());

        // Get Job Should return error (no heartbeat)
        getJob(workerAuthToken, status().is4xxClientError());
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
    public void NoRequestNoJob() throws Exception {
        // Login Successfully
        AccountIO.Authenticate.Request a =
                new AccountIO.Authenticate.Request("test@test.com", "helloworld");
        String authToken = login(a, status().isOk());

        // Create Worker Successfully
        WorkerIO.Create.Request b = new WorkerIO.Create.Request(CPU, "Atlantis-Remote");
        createWorker(b, authToken, status().is2xxSuccessful());

        // Worker Login Successfully
        WorkerIO.Augment.Request c = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote");
        String workerAuthToken = workerLogin(c, authToken, status().isOk());

        // Worker Heartbeat Successfully
        workerHeartbeat(workerAuthToken, status().is2xxSuccessful());

        // Get Job Should return error
        getJob(workerAuthToken, status().is4xxClientError());
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
            Assert.assertNotNull(jobResponse);
            RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                    jobResponse.getRequestId(),
                    jobResponse.getListId(),
                    jobResponse.getJobId(),
                    TrackingStatus.COMPLETE,
                    null);
            reportJob(d, workerAuthToken, status().is2xxSuccessful());
        }

        // Request Should be complete (not exist)
        Assert.assertNotNull(request);
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
        assert jobResponse != null;
        RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                jobResponse.getRequestId(),
                jobResponse.getListId(),
                jobResponse.getJobId(),
                TrackingStatus.COMPLETE,
                "beautiful");
        reportJob(d, workerAuthToken, status().is2xxSuccessful());

        // Request Should be complete (not exist)
        assert request != null;
        Assert.assertFalse(requestRepository.existsById(request.getId()));
    }

    @Test
    public void ExecuteMatchRequestConcurrentWorkers() throws Exception {
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

        // Create Worker 1 Successfully
        WorkerIO.Create.Request b1 = new WorkerIO.Create.Request(CPU, "Atlantis-Remote-1");
        createWorker(b1, authToken, status().is2xxSuccessful());
        // Worker 1 Login Successfully
        WorkerIO.Augment.Request c1 = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote-1");
        String workerAuthToken1 = workerLogin(c1, authToken, status().isOk());
        // Worker 1 Heartbeat Successfully
        workerHeartbeat(workerAuthToken1, status().is2xxSuccessful());

        // Create Worker 2 Successfully
        WorkerIO.Create.Request b2 = new WorkerIO.Create.Request(CPU, "Atlantis-Remote-2");
        createWorker(b2, authToken, status().is2xxSuccessful());
        // Worker 2 Login Successfully
        WorkerIO.Augment.Request c2 = new WorkerIO.Augment.Request(CPU, "Atlantis-Remote-2");
        String workerAuthToken2 = workerLogin(c2, authToken, status().isOk());
        // Worker 1 Heartbeat Successfully
        workerHeartbeat(workerAuthToken2, status().is2xxSuccessful());

        for (int i = 0; i < 5; i++) {
            // Get Job 1 Successfully
            RequestIO.GetJob.Response jobResponse1 = getJob(workerAuthToken1, status().is2xxSuccessful());

            // Get Job 2 Successfully
            RequestIO.GetJob.Response jobResponse2 = getJob(workerAuthToken2, status().is2xxSuccessful());

            // Report Job 1 Complete Successfully
            assert jobResponse1 != null;
            RequestIO.ReportJob.Request d1 = new RequestIO.ReportJob.Request(
                    jobResponse1.getRequestId(),
                    jobResponse1.getListId(),
                    jobResponse1.getJobId(),
                    TrackingStatus.COMPLETE,
                    null);
            reportJob(d1, workerAuthToken1, status().is2xxSuccessful());

            // Report Job 2 Complete Successfully
            assert jobResponse2 != null;
            RequestIO.ReportJob.Request d2 = new RequestIO.ReportJob.Request(
                    jobResponse2.getRequestId(),
                    jobResponse2.getListId(),
                    jobResponse2.getJobId(),
                    TrackingStatus.COMPLETE,
                    null);
            reportJob(d2, workerAuthToken2, status().is2xxSuccessful());
        }

        // Get Job 1 Successfully
        RequestIO.GetJob.Response jobResponse1 = getJob(workerAuthToken1, status().is2xxSuccessful());
        // Report Job 1 Complete Successfully
        assert jobResponse1 != null;
        RequestIO.ReportJob.Request d1 = new RequestIO.ReportJob.Request(
                jobResponse1.getRequestId(),
                jobResponse1.getListId(),
                jobResponse1.getJobId(),
                TrackingStatus.COMPLETE,
                null);
        reportJob(d1, workerAuthToken1, status().is2xxSuccessful());

        // Request Should be complete (not exist)
        assert request != null;
        Assert.assertFalse(requestRepository.existsById(request.getId()));
    }

    @Test
    public void ExecuteWPARequest() throws Exception {

    }

    @Test
    public void ExhaustJobs() throws Exception {
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

        for (int i = 0; i < 11; i++) {
            // Get Job Successfully
            RequestIO.GetJob.Response jobResponse = getJob(workerAuthToken, status().is2xxSuccessful());

            // Report Job Complete Successfully
            assert jobResponse != null;
            RequestIO.ReportJob.Request d = new RequestIO.ReportJob.Request(
                    jobResponse.getRequestId(),
                    jobResponse.getListId(),
                    jobResponse.getJobId(),
                    TrackingStatus.COMPLETE,
                    null);
            reportJob(d, workerAuthToken, status().is2xxSuccessful());
        }

        // Get Job
        getJob(workerAuthToken, status().is4xxClientError());
    }

    @Test
    public void ReportPasswordListJobError() throws Exception {

    }

    @Test
    public void ReportCrunchListJobError() throws Exception {

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
        assert jobResponse != null;
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

    private AccountIO.Register.Response registerUser(AccountIO.Register.Request req, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/account/register")
                .with(request -> {
                    request.addHeader("Content-Type", "application/json");
                    return request;
                }).content(mapper.writeValueAsString(req)))
                .andExpect(resultMatcher)
                .andReturn();
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), AccountIO.Register.Response.class);
        } catch (Exception e) {
            return null;
        }
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
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), Worker.class);
        } catch (Exception e) {
            return null;
        }
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
        try {
            return mapper.readValue(tokenResult.getResponse().getContentAsString(), WorkerIO.Augment.Response.class).getToken();
        } catch (Exception e) {
            return null;
        }
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
        mapper.addMixIn(Request.class, IgnoreRequestDetailsMixIn.class);
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), Request.class);
        } catch (Exception e) {
            return null;
        }
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
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), RequestIO.GetJob.Response.class);
        } catch (Exception e) {
            return null;
        }
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
