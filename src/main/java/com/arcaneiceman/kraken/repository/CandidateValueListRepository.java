package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.CandidateValueList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by Wali on 4/22/2018.
 */
@Repository
public interface CandidateValueListRepository extends JpaRepository<CandidateValueList, String> {

}
