package com.winter.visitor.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorVO implements Serializable {
    // Every visiting ip address should be counted for only once per page
    private Long page_vis;

    // Total visiting ip for page
    private Long unique_vis;

    // Rank for current ip
    private Long rank;

    // Every visit counts
    private Long temperature;

    public VisitorVO(VisitorVO visitorVO) {
        this.page_vis = visitorVO.page_vis;
        this.unique_vis = visitorVO.unique_vis;
        this.rank = visitorVO.rank;
        this.temperature = visitorVO.temperature;
    }
}
