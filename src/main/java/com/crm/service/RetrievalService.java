package com.crm.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

public interface RetrievalService {
    HashMap<String, Object> saveDocument(MultipartFile file, String clipToken, String username, String fileUrl) throws IOException;
}
