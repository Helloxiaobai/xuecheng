package com.xuecheng.ucenter.dao;

import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestXcMenuMapper {
    @Autowired
    XcMenuMapper xcMenuMapper;

    @Autowired
    XcUserRepository xcUserRepository;
    @Test
    public void xcMenuMapperTest(){
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId("49");
        System.out.println(xcMenus);
    }

    @Test
    public void testxcUserRepository(){
        XcUser test02 = xcUserRepository.findByUsername("itcast");
        System.out.println(test02);
    }
}
