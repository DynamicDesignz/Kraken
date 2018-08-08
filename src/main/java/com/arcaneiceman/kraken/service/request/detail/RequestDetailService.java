package com.arcaneiceman.kraken.service.request.detail;

import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequestDetailService {

    private WPARequestDetailService wpaRequestDetailService;
    private MatchRequestDetailService matchRequestDetailService;

    public RequestDetailService(WPARequestDetailService wpaRequestDetailService,

                                MatchRequestDetailService matchRequestDetailService) {
        this.wpaRequestDetailService = wpaRequestDetailService;
        this.matchRequestDetailService = matchRequestDetailService;
    }

    public RequestDetail get(RequestType requestType, Long id) {
        switch (requestType) {
            case WPA:
                return wpaRequestDetailService.get(id);
            case MATCH:
                return matchRequestDetailService.get(id);
            default:
                throw new RuntimeException("Request Type was null");
        }
    }

    public RequestDetail createWPARequest(WPARequestDetail wpaRequestDetail, MultipartFile passwordCaptureFile) {
        return wpaRequestDetailService.create(wpaRequestDetail, passwordCaptureFile);
    }

    public RequestDetail createMatchRequest(MatchRequestDetail matchRequestDetail) {
        return matchRequestDetailService.create(matchRequestDetail);
    }

    public void delete(RequestType requestType, Long id) {
        switch (requestType) {
            case WPA:
                wpaRequestDetailService.delete(id);
                break;
            case MATCH:
                matchRequestDetailService.delete(id);
                break;
            default:
                throw new RuntimeException("Request Type was null");
        }
    }

}
