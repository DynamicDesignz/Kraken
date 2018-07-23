package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.PasswordList;
import com.arcaneiceman.kraken.repository.PasswordListRepository;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

/**
 * Created by Wali on 4/22/2018.
 */
@Service
@Transactional
public class PasswordListService {

//    @Value("${application.password-list-settings.folder-prefix}")
//    private String storagePath;
//
//    @Value("${application.password-list-settings.job-size}")
//    private String jobSize;
//
//    private final AmazonS3Configuration amazonS3Configuration;
    private final PasswordListRepository passwordListRepository;

    public PasswordListService(
            //AmazonS3Configuration amazonS3Configuration,
                                     PasswordListRepository passwordListRepository) {
        //this.amazonS3Configuration = amazonS3Configuration;
        this.passwordListRepository = passwordListRepository;

    }

//    @PostConstruct
//    public void checkValues() throws IOException {
//        if (storagePath == null || storagePath.isEmpty())
//            throw new RuntimeException("Application Candidate Value List Updater : Storage Folder Not Specified ");
//        if (jobSize == null || jobSize.isEmpty())
//            throw new RuntimeException("Application Candidate Value List Updater : Job Size Not Specified");
//        //checkS3Bucket();
//    }

//    @Scheduled(cron = "0 0 1 * * ?")
//    public void checkS3Bucket() throws IOException {
//        log.info("Fetching Candidate Value Lists From Amazon S3...");
//        List<String> krakenList = passwordListRepository.findAll().stream()
//                .map(PasswordList::getName)
//                .collect(Collectors.toList());
//        List<String> s3List = amazonS3Configuration.generateClient().listObjects(
//                amazonS3Configuration.getAmazonS3BucketName(), storagePath).getObjectSummaries().stream()
//                .map(S3ObjectSummary::getKey)
//                .collect(Collectors.toList());
//
//        // Remove Lists that dont exist any more
//        for (String existingList : krakenList)
//            if (!s3List.contains(existingList))
//                passwordListRepository.deleteById(existingList);
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
//                Set<JobDelimiter> jobDelimiterSet = new HashSet<>();
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
//                        jobDelimiterSet.add(new JobDelimiter(jobStartMarker, jobOffsetMarker));
//                        // Reset the jobStartMarker
//                        jobStartMarker = jobOffsetMarker;
//                        // Reset number of lines read
//                        numOfLinesRead = 0;
//                    }
//                }
//
//                // If there are left over lines that werent put into the list, put them now
//                if (numOfLinesRead > 0)
//                    jobDelimiterSet.add(new JobDelimiter(jobStartMarker, jobOffsetMarker));
//
//                // Save Candidate Value list
//                passwordListRepository.save(new PasswordList(s3ListItem.substring(s3ListItem.lastIndexOf("/") + 1), null, jobDelimiterSet));
//            }
//        log.info("Candidate Value Lists Fetch From Amazon S3 Complete!");
//    }

    public Page<PasswordList> get(Pageable pageable) {
        return passwordListRepository.findAll(pageable);
    }

    public PasswordList get(String name) {
        return passwordListRepository.findById(name)
                .orElseThrow(() -> new SystemException(2342, "Could not find Candidate Value List", Status.NOT_FOUND));
    }

    public PasswordList getOrNull(String name){
        return passwordListRepository.getOne(name);
    }


}
