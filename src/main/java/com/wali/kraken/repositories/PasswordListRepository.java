package com.wali.kraken.repositories;

import com.wali.kraken.domain.PasswordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PasswordListRepository extends JpaRepository<PasswordList, String> {

    @Query("SELECT pl FROM PasswordList pl WHERE pl.listName = ?1")
    PasswordList findBylistName(String listName);
}
