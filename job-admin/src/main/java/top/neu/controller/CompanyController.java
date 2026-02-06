package top.neu.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.neu.entity.Company;
import top.neu.framework.redis.RedisUtil;
import top.neu.framework.role.RequiresRoles;
import top.neu.framework.role.Role;
import top.neu.service.CompanyService;
import top.neu.utils.Result;
import top.neu.utils.UserThreadLocal;
import top.neu.vo.UserData;

import java.util.Map;

@RestController
@RequestMapping("/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/create")
    public Result create(@RequestBody Company company) {
        int flag = companyService.create(company);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/delete")
    public Result delete(String ids) {
        int flag = companyService.delete(ids);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody Company company) {
        int flag = companyService.update(company);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/detail")
    public Result detail(Integer id) {
        return Result.success(companyService.detail(id));
    }

    @PostMapping("/query")
    public Map<String, Object> query(@RequestBody Company company) {
        PageInfo<Company> pageInfo = companyService.query(company);
        return Result.success(pageInfo);
    }

    @PostMapping("/info")
    @RequiresRoles(type = Role.COMPANY)
    public Result info() {
        //获取登录用户的信息
        String token = UserThreadLocal.get();
        UserData userData = (UserData) redisUtil.get(token);
        return Result.success(companyService.detail(userData.getId()));
    }

}
