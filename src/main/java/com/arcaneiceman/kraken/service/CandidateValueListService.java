package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.arcaneiceman.kraken.domain.CandidateValueList;
import com.arcaneiceman.kraken.repository.CandidateValueListRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Wali on 4/22/2018.
 */
@Service
@Transactional
public class CandidateValueListService {

    @Value("${amazon.s3.bucketname}")
    private String bucketName;

    private final String listPrefix = "/candidate-value-lists";

    private final AmazonS3 amazonS3;

    private final CandidateValueListRepository candidateValueListRepository;
    private final ListJobDescriptorService listJobDescriptorService;

    public CandidateValueListService(AmazonS3 amazonS3,
                                     CandidateValueListRepository candidateValueListRepository,
                                     ListJobDescriptorService listJobDescriptorService) {
        this.amazonS3 = amazonS3;
        this.candidateValueListRepository = candidateValueListRepository;
        this.listJobDescriptorService = listJobDescriptorService;
    }

    public CandidateValueList get(String name) {
        return candidateValueListRepository.findById(name).orElseThrow(() -> new RuntimeException("Not Found"));
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void checkS3Bucket() {
        List<String> krakenList = candidateValueListRepository.findAll().stream()
                .map(CandidateValueList::getName)
                .collect(Collectors.toList());
        List<String> s3List = amazonS3.listObjects(bucketName, listPrefix).getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());

        // Deletion Phase
        for (String krakenListItem : krakenList)
            if (!s3List.contains(krakenListItem))
                listJobDescriptorService.deleteByOwner(candidateValueListRepository.getOne(krakenListItem));

        // Addition Phase
        for (String s3ListItem : s3List)
            if (!krakenList.contains(s3ListItem)){
                S3Object s3Object = amazonS3.getObject(bucketName, s3ListItem);
                InputStream fileStream = new BufferedInputStream(s3Object.getObjectContent());
//        GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
//        InputStreamReader decoder = new InputStreamReader(gzipStream, "UTF-8");
//        BufferedReader buffered = new BufferedReader(decoder);
            }

    }
}
