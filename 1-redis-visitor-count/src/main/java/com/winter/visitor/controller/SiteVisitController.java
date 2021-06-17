package com.winter.visitor.controller;

import com.winter.visitor.dto.VisReqDTO;
import com.winter.visitor.service.VisitingFacade;
import com.winter.visitor.vo.SiteVisitVO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SiteVisitController {
    private final VisitingFacade visitingFacade;

    public SiteVisitController(VisitingFacade visitingFacade) {
        this.visitingFacade = visitingFacade;
    }

    @RequestMapping(path = "visit")
    @ResponseBody
    public SiteVisitVO siteVisit(VisReqDTO visReqDTO) {
        return this.visitingFacade.siteVisit(visReqDTO);
    }
}
