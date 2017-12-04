package com.wali.kraken.domain.overwire;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Wali on 12/3/2017.
 */
@Data
@AllArgsConstructor
public class Reply implements Serializable{

    private boolean found;

    private String answer;

}
