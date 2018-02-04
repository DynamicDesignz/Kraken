package com.wali.kraken.domain;

import com.wali.kraken.config.Constants;
import com.wali.kraken.domain.enumerations.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateValueList {

    @Id
    @Column
    private String listPath;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestType listType;

    @Column
    private String listName;

    @Column
    private long lineCount;

    @Column
    private String characterSet;

    @Column
    private Long size;

    public CandidateValueList(String listPath, RequestType requestType) {
        this.listPath = listPath;
        this.listType = requestType;

        // If file does not exist in specified location, return error
        if (!Files.exists(Paths.get(listPath)))
            throw new RuntimeException("Password List File did not exist");

        if (!constructList())
            throw new RuntimeException("Could not read Password List File");
    }

    private boolean constructList() {
        this.lineCount = -1;
        for (Charset charset : Constants.SupportedCharsets) {
            try {
                this.lineCount = Files.lines(Paths.get(listPath), charset).count();
                if (this.lineCount != -1) {
                    this.characterSet = charset.name();
                    this.size = Files.size(Paths.get(listPath));
                    this.listName = Paths.get(listPath).getFileName().toString();
                    return true;
                }
            } catch (Exception e) {/*Unsupported Encoding Exception*/}
        }
        return false;
    }

}
