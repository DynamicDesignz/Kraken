package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.arcaneiceman.kraken.domain.CandidateValueList;
import com.arcaneiceman.kraken.domain.embedded.JobDelimter;
import com.arcaneiceman.kraken.repository.CandidateValueListRepository;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Wali on 4/22/2018.
 */
@Service
@Transactional
public class CandidateValueListService {

    private static Logger log = LoggerFactory.getLogger(CandidateValueListService.class);

    @Value("${application.candidate-value-list-settings.folder-prefix}")
    private String storagePath;

    @Value("${application.candidate-value-list-settings.job-size}")
    private String jobSize;

    private final AmazonS3Configuration amazonS3Configuration;
    private final CandidateValueListRepository candidateValueListRepository;

    public CandidateValueListService(AmazonS3Configuration amazonS3Configuration,
                                     CandidateValueListRepository candidateValueListRepository) {
        this.amazonS3Configuration = amazonS3Configuration;
        this.candidateValueListRepository = candidateValueListRepository;

    }

    @PostConstruct
    public void checkValues() throws IOException {
        if (storagePath == null || storagePath.isEmpty())
            throw new RuntimeException("Application Candidate Value List Updater : Storage Folder Not Specified ");
        if (jobSize == null || jobSize.isEmpty())
            throw new RuntimeException("Application Candidate Value List Updater : Job Size Not Specified");
        //checkS3Bucket();
    }

//    @Scheduled(cron = "0 0 1 * * ?")
//    public void checkS3Bucket() throws IOException {
//        log.info("Fetching Candidate Value Lists From Amazon S3...");
//        List<String> krakenList = candidateValueListRepository.findAll().stream()
//                .map(CandidateValueList::getName)
//                .collect(Collectors.toList());
//        List<String> s3List = amazonS3Configuration.generateClient().listObjects(
//                amazonS3Configuration.getAmazonS3BucketName(), storagePath).getObjectSummaries().stream()
//                .map(S3ObjectSummary::getKey)
//                .collect(Collectors.toList());
//
//        // Remove Lists that dont exist any more
//        for (String existingList : krakenList)
//            if (!s3List.contains(existingList))
//                candidateValueListRepository.deleteById(existingList);
//
//        // Add Lists that are now present
//        for (String s3ListItem : s3List)
//            if (!krakenList.contains(s3ListItem)) {
//
//                // Get Input Stream
//                S3Object s3Object = amazonS3Configuration.generateClient().getObject(
//                        amazonS3Configuration.getAmazonS3BucketName(), s3ListItem);
//                InputStream fileStream = new BufferedInputStream(s3Object.getObjectContent());
//                InputStreamReader decoder = new InputStreamReader(fileStream, "UTF-8");
//                BufferedReader buffered = new BufferedReader(decoder);
//
//                // Initialize Variables
//                String thisLine;
//                int numOfLinesRead = 0;
//                long jobStartMarker = 0;
//                long jobOffsetMarker = 0;
//                Set<JobDelimter> jobDelimiterSet = new HashSet<>();
//
//                // While there are lines to be read...
//                while ((thisLine = buffered.readLine()) != null) {
//                    // Increment number of lines and the jobOffsetMarker
//                    numOfLinesRead++;
//                    jobOffsetMarker = jobOffsetMarker + thisLine.length() + 1;
//
//                    // If job size limit is reached...
//                    if (numOfLinesRead == Integer.parseInt(jobSize)) {
//                        // Add job to delimiter set
//                        jobDelimiterSet.add(new JobDelimter(jobStartMarker, jobOffsetMarker));
//                        // Reset the jobStartMarker
//                        jobStartMarker = jobOffsetMarker;
//                        // Reset number of lines read
//                        numOfLinesRead = 0;
//                    }
//                }
//
//                // If there are left over lines that werent put into the list, put them now
//                if (numOfLinesRead > 0)
//                    jobDelimiterSet.add(new JobDelimter(jobStartMarker, jobOffsetMarker));
//
//                // Save Candidate Value list
//                candidateValueListRepository.save(new CandidateValueList(s3ListItem.substring(s3ListItem.lastIndexOf("/") + 1), null, jobDelimiterSet));
//            }
//        log.info("Candidate Value Lists Fetch From Amazon S3 Complete!");
//    }

    public CandidateValueList get(String name) {
        return candidateValueListRepository.findById(name).orElseThrow(() -> new RuntimeException("Not Found"));
    }


}
