package com.juxa.legal_advice.service;
import com.google.cloud.storage.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AiBucketService {
    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "asesoria-legal-bucket";

    public String readUserHistoru
}
