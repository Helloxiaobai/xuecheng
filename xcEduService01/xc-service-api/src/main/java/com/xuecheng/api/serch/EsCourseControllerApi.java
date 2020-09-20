package com.xuecheng.api.serch;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.QueryResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;

@Api(value = "课程搜索",description = "课程搜索",tags = {"课程搜索"})
public interface EsCourseControllerApi {
    //搜索课程信息
    @ApiOperation("提供课程搜搜接口")
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam);

    @ApiOperation("根据课程id查询课程信息")
    public Map<String,CoursePub> getall(String courseId);


    @ApiOperation("根据课程计划id查询课程媒资信息")
    public TeachplanMediaPub getmedia(String teachplanId);
}
