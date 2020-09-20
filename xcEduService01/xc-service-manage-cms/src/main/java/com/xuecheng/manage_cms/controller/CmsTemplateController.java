package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsSiteControllerApi;
import com.xuecheng.api.cms.CmsTemplateControllerApi;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import com.xuecheng.manage_cms.service.SiteListService;
import com.xuecheng.manage_cms.service.TemplateListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cms/template/")
public class CmsTemplateController implements CmsTemplateControllerApi {

    @Autowired
    TemplateListService templateListService;

    @Override
    @GetMapping("/list")
    public QueryResponseResult findTemplateListAll() {
        return templateListService.findTemplateList();
    }
}
