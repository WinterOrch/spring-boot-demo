package com.winter.visitor.dto;

import lombok.Data;

@Data
public class VisReqDTO {
    private String visitorApp;
    private String visitorIpAddr;
    private String visitingURI;
}
