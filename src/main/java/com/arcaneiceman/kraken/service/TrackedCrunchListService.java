package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedCrunchListRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedCrunchListPermissionLayer;
import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TrackedCrunchListService {

    private TrackedCrunchListRepository trackedCrunchListRepository;
    private TrackedCrunchListPermissionLayer trackedCrunchListPermissionLayer;

    public TrackedCrunchListService(TrackedCrunchListRepository trackedCrunchListRepository,
                                    TrackedCrunchListPermissionLayer trackedCrunchListPermissionLayer) {
        this.trackedCrunchListRepository = trackedCrunchListRepository;
        this.trackedCrunchListPermissionLayer = trackedCrunchListPermissionLayer;
    }

    public TrackedCrunchList create(Integer minSize,
                                    Integer maxSize,
                                    String characters,
                                    String startString,
                                    Request request) {
        // Validate Crunch List
        String response = ConsoleCommandUtil.executeCommandInConsole(
                500,
                TimeUnit.MILLISECONDS,
                ConsoleCommandUtil.OutputStream.ERROR,
                "crunch", minSize.toString(), maxSize.toString(), characters, "-s", startString);
        if(response == null || response.isEmpty())
            throw new SystemException(1231, "Could not create Crunch Request "
                    + minSize + " " + maxSize + " " + characters, Status.BAD_REQUEST );
        Pattern pattern = Pattern.compile("Crunch will now generate the following number of lines: (\\d+)");
        Matcher matcher = pattern.matcher(response);
        if(!matcher.find())
            throw new SystemException(1231, "Could not create Crunch Request "
                    + minSize + " " + maxSize + " " + characters, Status.BAD_REQUEST );
        Long totalJobs = Long.parseLong(matcher.group(1)) /
        TrackedCrunchList trackedCrunchList = new TrackedCrunchList(
                null,
                minSize,
                maxSize,
                characters,
                TrackingStatus.PENDING,
                totalJobs,
                1L,
                startString,
                0L,
                0L,
                0L);


    }
}
