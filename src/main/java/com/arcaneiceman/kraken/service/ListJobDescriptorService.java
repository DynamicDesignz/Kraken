package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.CandidateValueList;
import com.arcaneiceman.kraken.domain.ListJobDescriptor;
import com.arcaneiceman.kraken.repository.ListJobDescriptorRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Created by Wali on 4/22/2018.
 */
@Service
@Transactional
public class ListJobDescriptorService {

    private final ListJobDescriptorRepository listJobDescriptorRepository;

    public ListJobDescriptorService(
            ListJobDescriptorRepository listJobDescriptorRepository) {
        this.listJobDescriptorRepository = listJobDescriptorRepository;
    }

    public ListJobDescriptor create(CandidateValueList owner, Long startByte, Long endByte) {
        ListJobDescriptor listJobDescriptor = new ListJobDescriptor();
        listJobDescriptor.setStartByte(startByte);
        listJobDescriptor.setEndByte(endByte);
        listJobDescriptor.setOwner(owner);
        return listJobDescriptorRepository.save(listJobDescriptor);
    }

    public void deleteByOwner(CandidateValueList owner){

    }

    public void delete(Long id) {
        listJobDescriptorRepository.deleteById(id);
    }

}
