package com.crm.service;

import java.io.IOException;

public interface TessaractService {
    String parseIMG(String bucket, String key, String token) throws IOException;
}
