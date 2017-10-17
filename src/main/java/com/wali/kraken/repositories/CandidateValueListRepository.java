package com.wali.kraken.repositories;

import com.wali.kraken.domain.CandidateValueList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CandidateValueListRepository extends JpaRepository<CandidateValueList, String> {

    @Query("SELECT cl FROM CandidateValueList cl WHERE cl.listName = ?1")
    CandidateValueList findByListName(String listName);
}
