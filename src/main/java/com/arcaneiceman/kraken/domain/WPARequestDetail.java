package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Created by Wali on 4/21/2018.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "wpa_request_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class WPARequestDetail extends RequestDetail implements Serializable{

    @Column
    private String ssid;

    @Column
    private String passwordCaptureFileKey;

    @Transient
    private String passwordCaptureFileUrl;

}
