package com.wali.kraken.repositories;

import com.wali.kraken.domain.file.readers.CandidateValueListReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Wali on 12/3/2017.
 */
@Component
public class FileReaderRepository {

    private Map<String, CandidateValueListReader> map;

    @Autowired
    public FileReaderRepository(){
        map = new HashMap<>();
    }

    public CandidateValueListReader get(String key){
        return map.get(key);
    }

    public void put(String key, CandidateValueListReader reader){
        map.put(key,reader);
    }

    public void delete(String key){
        map.remove(key);
    }
}
