package com.winter.visitor.service;

import com.winter.visitor.common.consts.RedisKey;
import com.winter.visitor.common.util.DateUtils;
import com.winter.visitor.dto.VisReqDTO;
import com.winter.visitor.vo.SiteVisitVO;
import com.winter.visitor.vo.VisitorVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;

@Service
public class VisitingFacade {
    private final VisitingService visServ;

    public VisitingFacade(VisitingService visitingService) {
        this.visServ = visitingService;
    }

    /**
     * URI 访问统计
     */
    public SiteVisitVO siteVisit(VisReqDTO visReqDTO) {
        URI originURI = URI.create(visReqDTO.getVisitingURI());
        // Get Host
        String host = originURI.getHost();
        if (originURI.getPort() > 0 && originURI.getPort() != 80) {
            host = host + ":80";
        }

        VisitorVO siteVisitorVO = this.doVisit(visReqDTO.getVisitorApp(), host, visReqDTO.getVisitorIpAddr());
        VisitorVO uriVisitorVO;
        // Get BaseURI
        String baseURI = originURI.getPath();
        if (originURI.getFragment() != null) {
            baseURI = baseURI + "#" + originURI.getFragment();
        }
        if (StringUtils.isEmpty(baseURI.trim())) {
            uriVisitorVO = new VisitorVO(siteVisitorVO);
        } else {
            uriVisitorVO = this.doVisit(visReqDTO.getVisitorApp(), host + baseURI, visReqDTO.getVisitorIpAddr());
        }

        return new SiteVisitVO(siteVisitorVO, uriVisitorVO);
    }

    private VisitorVO doVisit(String app_name, String uri, String visitor_ip) {
        String pvKey = "SITE_CNT:" + app_name;
        String temperatureKey = "TEMP_CNT:" + app_name;
        String uvKey = "URI_RANK:" + app_name + ":" + uri;
        String visitorTodayKey = "URI_TAG:" + DateUtils.getToday() + ":" + uri + ":" + RedisKey.VIS_TODAY_BLOOM_FILTER_SUFFIX;

        Long temperature = visServ.incrTemp(temperatureKey, uri);
        Long pv = visServ.fetchPV(pvKey, uri);
        if (pv == null || pv == 0) {
            // Page's first visit
            visServ.incrPV(pvKey, uri);
            visServ.setUV(uvKey, visitor_ip, 1L);
            visServ.markVisitToday(visitorTodayKey, visitor_ip);
            return new VisitorVO(1L, 1L, 1L, temperature);
        }


        VisitorVO res = visServ.fetchUV(app_name, uri, visitor_ip);
        res.setTemperature(temperature);

        if (visServ.visitToday(visitorTodayKey, visitor_ip)) {
            res.setPage_vis(pv);

        } else {
            visServ.incrPV(pvKey, uri);

            if (res.getUnique_vis() == 0L) {
                // Page's first visit, pv += 1; uv += 1
                visServ.setUV(uvKey, visitor_ip, 1L);

                res.setPage_vis(pv + 1);
                res.setRank(1L);
                res.setUnique_vis(1L);
            } else if (res.getRank() == 0L) {
                // IP's first visit, pv += 1; uv += 1
                Long uv = res.getUnique_vis();
                visServ.setUV(uvKey, visitor_ip, uv + 1);

                res.setPage_vis(pv + 1);
                res.setUnique_vis(uv + 1);
                res.setRank(uv + 1);
            } else {
                // IP's first visit today， pv += 1 ; uv remains
                res.setPage_vis(pv + 1);
            }

            visServ.markVisitToday(visitorTodayKey, visitor_ip);
        }

        return res;
    }
}
