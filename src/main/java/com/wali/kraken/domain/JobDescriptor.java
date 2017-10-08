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
public class JobDescriptor {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @ManyToOne
    @JoinColumn(name = "request_queue_number")
    private PasswordRequest passwordRequest;

    @ManyToOne
    @JoinColumn(name = "password_list_queue_number")
    private PasswordListDescriptor passwordListDescriptor;

    @Column
    public long startLine;

    @Column
    public long end;

    @Column
    public long timeRunning;
}
