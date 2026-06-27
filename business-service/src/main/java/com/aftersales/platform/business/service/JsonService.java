package com.aftersales.platform.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JsonService {
    private final ObjectMapper mapper;
    public JsonService(ObjectMapper mapper) { this.mapper = mapper; }
    public String toJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { return String.valueOf(value); }
    }
}
