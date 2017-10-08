package com.wali.kraken.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class PasswordRequest {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Column
    private String SSIDToFind;

    @Column
    private String passwordCaptureInBase64;

    @Column
    private String colonDelimitedPasswordListNames;

    @Column
    private String result;

}
