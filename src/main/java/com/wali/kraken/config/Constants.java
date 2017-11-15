package com.wali.kraken.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {
    /**
     * AIR CRACK ERRORS
     */
    public static final String VALID_FILE = "Please specify a dictionary";
    public static final String QUIT_SEQUENCE = "Quitting aircrack-ng...";
    public static final String INVALID_MAC = "Invalid BSSID";
    public static final String INVALID_FILE = "Unsupported file format";
    /**
     * New Request Responses
     */
    public static final String new_passwordrequest_response_success = "Request Added!";
    /**
     * Add Password HTML Responses
     **/
    public static final String add_password_list_response_success = "Password List Added!";
    /**
     * SUPPORTED PASSWORD LIST ENCODINGS
     */
    public static Charset[] SupportedCharsets =
            new Charset[]{StandardCharsets.ISO_8859_1,
                    StandardCharsets.UTF_8,
                    StandardCharsets.UTF_16,
                    StandardCharsets.US_ASCII};

    /**
     * Startup Mode Options
     */
    public enum StartUpMode {
        ServerOnly, ServerAndWorker, WorkerOnly
    }

    public static final String CANDIDATE_VALUE_LIST_DIRECTORY = "candidate-value-lists";

}