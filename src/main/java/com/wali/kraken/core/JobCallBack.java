package com.wali.kraken.core;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;

/**
 * Created by Wali on 10/14/2017.
 */
public class JobCallBack implements GearmanJobEventCallback<String> {

    private ProcessingCore processingCore;

    public JobCallBack(ProcessingCore processingCore) {
        this.processingCore = processingCore;
    }

    @Override
    public void onEvent(String s, GearmanJobEvent gearmanJobEvent) {

    }
}
