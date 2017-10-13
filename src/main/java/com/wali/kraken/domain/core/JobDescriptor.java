package com.wali.kraken.domain.core;

import com.wali.kraken.domain.PasswordList;
import com.wali.kraken.enumerations.ProcessingStatus;
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
public class JobDescriptor {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Column
    private ProcessingStatus processingStatus;

    @ManyToOne
    @JoinColumn(name = "request_queue_number")
    private PasswordRequest passwordRequest;

    @ManyToOne
    @JoinColumn(name = "password_list_queue_number")
    private PasswordListDescriptor passwordListDescriptor;

    @ManyToOne
    @JoinColumn(name = "password_list")
    private PasswordList passwordList;

    @Column
    public long startLine;

    @Column
    public long end;

    @Column
    public long timeRunning;
}
