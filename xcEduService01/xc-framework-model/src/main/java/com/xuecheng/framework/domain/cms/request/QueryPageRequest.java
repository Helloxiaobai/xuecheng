package com.xuecheng.framework.domain.cms.request;

import lombok.Data;

@Data
public class QueryPageRequest {

    //页面查询条件
    private String siteId;

    private String pageId;

    private String pageName;

    private String pageAliase;

    private String templateId;

}
