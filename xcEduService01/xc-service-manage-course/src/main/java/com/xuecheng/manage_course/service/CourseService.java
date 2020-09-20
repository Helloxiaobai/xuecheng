package com.xuecheng.manage_course.service;


import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    CourseMarketReRepository CourseMarketReRepository;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Autowired
    CoursePubRepository coursePubRepository;

    @Value("${course‐publish.dataUrlPre}")
    private String publish_dataUrlPre;

    @Value("${course‐publish.pagePhysicalPath}")
    private String publish_page_physicalpath;

    @Value("${course‐publish.pageWebPath}")
    private String publish_page_webpath;

    @Value("${course‐publish.siteId}")
    private String publish_siteId;

    @Value("${course‐publish.templateId}")
    private String publish_templateId;

    @Value("${course‐publish.previewUrl}")
    private String previewUrl;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;


    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }

    //添加课程计划
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        if(teachplan == null || StringUtils.isEmpty(teachplan.getCourseid())||StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //课程计划
        String courseid = teachplan.getCourseid();
        //parentId
        String parentid = teachplan.getParentid();
        if(StringUtils.isEmpty(parentid)){
            //取出该课程的根节点
            parentid  = this.getTeachplanRoot(courseid);
        }
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan parentNode = optional.get();
        String grade = parentNode.getGrade();
        //新添加的节点
        Teachplan teachplanNew = new Teachplan();
        BeanUtils.copyProperties(teachplan,teachplanNew);
        teachplanNew.setParentid(parentid);
        teachplanNew.setCourseid(courseid);
        if(grade.equals("1")){
            teachplanNew.setGrade("2");
        }else {
            teachplanNew.setGrade("3");
        }
        teachplanRepository.save(teachplanNew);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //查询课程的根节点 如果查询不到要自动添加根节点
    private String getTeachplanRoot(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(!optional.isPresent()){
            return null;
        }
        CourseBase courseBase = optional.get();
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndAndParentid(courseId, "0");
        if(teachplanList == null||teachplanList.size() <= 0){
            //查询不到 要自动添加根节点
            Teachplan teachplan = new Teachplan();
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setPname(courseBase.getName());
            teachplan.setCourseid(courseId);
            teachplan.setStatus("0");
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        //返回跟节点id
        return teachplanList.get(0).getId();
    }

    public QueryResponseResult<CourseInfo> findCourseList(String companyId,int page, int size, CourseListRequest courseListRequest) {
        if(courseListRequest == null){
            courseListRequest = new CourseListRequest();
        }
        courseListRequest.setCompanyId(companyId);
        PageHelper.startPage(page, size);
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);
        List<CourseInfo> result = courseListPage.getResult();
        long total = courseListPage.getTotal();
        QueryResult<CourseInfo> courseInfoQueryResult = new QueryResult<>();
        courseInfoQueryResult.setList(result);
        courseInfoQueryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS,courseInfoQueryResult);
    }

    public CategoryNode findCategoryList() {
        return categoryMapper.selectList();
    }

    public AddCourseResult addCourseBase(CourseBase courseBase) {
        courseBase.setStatus("202001");
        CourseBase course = courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,course.getId());
    }

    public CourseBase getCourseBaseById(String courseId) {
        if (courseId == null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CourseCode.COURSE_DATAISNULL);
        }
        return optional.get();
    }

    @Transactional
    public ResponseResult updateCourseBase(String courseId, CourseBase courseBase) {
        if(courseId == null || courseBase == null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(optional.isPresent()){
            CourseBase courseBaseNew = optional.get();
            courseBaseNew.setName(courseBase.getName());
            courseBaseNew.setMt(courseBase.getMt());
            courseBaseNew.setSt(courseBase.getSt());
            courseBaseNew.setGrade(courseBase.getGrade());
            courseBaseNew.setStudymodel(courseBase.getStudymodel());
            courseBaseNew.setUsers(courseBase.getUsers());
            courseBaseNew.setDescription(courseBase.getDescription());
            courseBaseRepository.save(courseBaseNew);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return null;
    }

    public CourseMarket getCourseMarketById(String courseId) {
        if(StringUtils.isEmpty(courseId)){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Optional<CourseMarket> optional = CourseMarketReRepository.findById(courseId);
        return optional.orElse(null);
    }

    public ResponseResult updateCourseMarket(String courseId, CourseMarket courseMarket) {
        if(StringUtils.isEmpty(courseId) || courseMarket == null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Optional<CourseMarket> optional = CourseMarketReRepository.findById(courseId);
        if(optional.isPresent()){
            CourseMarket courseMarketNew = optional.get();
            courseMarketNew.setCharge(courseMarket.getCharge());
            courseMarketNew.setPrice(courseMarket.getPrice());
            courseMarketNew.setValid(courseMarket.getValid());
            courseMarketNew.setStartTime(courseMarket.getStartTime());
            courseMarketNew.setEndTime(courseMarket.getEndTime());
            courseMarketNew.setQq(courseMarket.getQq());
            CourseMarketReRepository.save(courseMarketNew);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //向课程管理数据库添加课程与图片的关联信息
    @Transactional
    public ResponseResult addCoursePic(String courseId, String pic) {
        CoursePic coursePic = null;
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if(!optional.isPresent()){
            coursePic = new CoursePic();
            coursePic.setPic(pic);
            coursePic.setCourseid(courseId);
            coursePicRepository.save(coursePic);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        coursePic = optional.get();
        coursePic.setPic(pic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //查询课程图片
    public CoursePic findCoursePic(String courseId) {
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        System.out.println(optional);
        return optional.orElse(null);
    }

    public ResponseResult deleteCoursePic(String courseId) {
        coursePicRepository.deleteById(courseId);
        return new ResponseResult(CommonCode.SUCCESS);

    }

    //查询课程视图 信息 图片 营销 课程计划
    public CourseView getCourseView(String courseId) {
        CourseView courseView = new CourseView();
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(optional.isPresent()){
            CourseBase courseBase = optional.get();
            courseView.setCourseBase(courseBase);
        }
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if(picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            courseView.setCoursePic(coursePic);
        }
        //课程营销信息
        Optional<CourseMarket> marketOptional = CourseMarketReRepository.findById(courseId);
        if(marketOptional.isPresent()){
            CourseMarket courseMarket = marketOptional.get();
            courseView.setCourseMarket(courseMarket);
        }
        //课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    public CoursePublishResult preview(String id) {
        CourseBase courseBase = this.findCourseBaseById(id);
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setDataUrl(publish_dataUrlPre + id);
        cmsPage.setPageName(id+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setTemplateId(publish_templateId);
        //请求cms添加页面
        CmsPageResult cmsPageResult = cmsPageClient.saveCmsPage(cmsPage);
        if(!cmsPageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        CmsPage cmsPage1 = cmsPageResult.getCmsPage();
        //拼装页面预览的url
        String url = previewUrl + cmsPage1.getPageId();
        //返回页面预览对象
        return new CoursePublishResult(CommonCode.SUCCESS,url);
    }

    private CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        ExceptionCast.cast(CourseCode.COURSE_DATAISNULL);
        return null;
    }

    @Transactional
    public CoursePublishResult publish(String courseId) {
        //调用cms一键发布接口将课程详情页面发布到服务器
        //准备页面信息
        CourseBase courseBase = this.findCourseBaseById(courseId);
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setDataUrl(publish_dataUrlPre + courseId);
        cmsPage.setPageName(courseId+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setTemplateId(publish_templateId);
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if(!cmsPostPageResult.isSuccess()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //保存课程的发布状态为"已发布"
        CourseBase courseBaseSave = this.saveCoursePubState(courseBase);
        //得到页面url
        String pageUrl = cmsPostPageResult.getPageUrl();
        //保存课程索引信息
        //创建一个course对象
        CoursePub coursePub = createCoursePub(courseId);
        //将coursePub对象保存到数据库
        saveCoursePub(courseId,coursePub);
        //缓存课程的信息

        //向teachplanMediaPub中保存课程媒资信息
        saveTeachPlanMediaPub(courseId);
        return new CoursePublishResult(CommonCode.SUCCESS, pageUrl);
    }

    private void saveTeachPlanMediaPub(String courseId){
        //先删除teachplanMediaPub中的数据
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        //从teachplanMedia中查询数据
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
        //将teachplanMediaList插入到teachplanMediaPub
        ArrayList<TeachplanMediaPub> teachplanMediaPubs = new ArrayList<>();
        for (TeachplanMedia teachplanMedia: teachplanMediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            teachplanMediaPub.setCourseId(teachplanMedia.getCourseId());
            teachplanMediaPub.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
            teachplanMediaPub.setMediaId(teachplanMedia.getMediaId());
            teachplanMediaPub.setMediaUrl(teachplanMedia.getMediaUrl());
            teachplanMediaPub.setTeachplanId(teachplanMedia.getTeachplanId());
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubs.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubs);
    }

    private CoursePub saveCoursePub(String id , CoursePub coursePub){
        Optional<CoursePub> optional = coursePubRepository.findById(id);
        CoursePub coursePubNew = null;
        coursePubNew = optional.orElseGet(CoursePub::new);
        BeanUtils.copyProperties(coursePub,coursePubNew);
        coursePubNew.setId(id);
        coursePubNew.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String format = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(format);
        return coursePubRepository.save(coursePubNew);
    }

    private CoursePub createCoursePub(String id){
        CoursePub coursePub = new CoursePub();
        //copy课程信息
        CourseBase courseBase = getCourseBaseById(id);
        BeanUtils.copyProperties(courseBase,coursePub);
        //copy营销信息
        CourseMarket courseMarket = getCourseMarketById(id);
        BeanUtils.copyProperties(courseMarket,coursePub);
        //copy课程图片信息
        CoursePic coursePic = findCoursePic(id);
        BeanUtils.copyProperties(coursePic,coursePub);
        //copyTeachplan信息
        TeachplanNode teachplanList = findTeachplanList(id);
        String jsonString = JSON.toJSONString(teachplanList);
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    private CourseBase saveCoursePubState(CourseBase courseBase){
        courseBase.setStatus("202002");
        return courseBaseRepository.save(courseBase);
    }
    //保存课程计划与妹子文件的关联
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia) {
        if(teachplanMedia == null||StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CourseCode.COURSE_MEDIS_TEACHPLAY_ISNULL);
        }
        //校验课程计划是否是3级
        //查询课程计划
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Teachplan teachplan = optional.get();
        String grade = teachplan.getGrade();
        if(StringUtils.isEmpty(grade)||!"3".equals(grade)){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }

        Optional<TeachplanMedia> optionalTeachplanMedia = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia teachplanMediaOne = null;
        teachplanMediaOne = optionalTeachplanMedia.orElseGet(TeachplanMedia::new);
        teachplanMediaOne.setCourseId(teachplan.getCourseid());
        teachplanMediaOne.setMediaId(teachplanMedia.getMediaId());
        teachplanMediaOne.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        teachplanMediaOne.setMediaUrl(teachplanMedia.getMediaUrl());
        teachplanMediaOne.setTeachplanId(teachplanId);
        teachplanMediaRepository.save(teachplanMediaOne);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
