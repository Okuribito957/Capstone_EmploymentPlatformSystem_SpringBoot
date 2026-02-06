package top.neu.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.neu.entity.Student;
import top.neu.framework.redis.RedisUtil;
import top.neu.framework.role.RequiresRoles;
import top.neu.framework.role.Role;
import top.neu.service.StudentService;
import top.neu.utils.Result;
import top.neu.utils.UserThreadLocal;
import top.neu.vo.UserData;

import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/create")
    public Result create(@RequestBody Student student) {
        int flag = studentService.create(student);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/delete")
    public Result delete(String ids) {
        int flag = studentService.delete(ids);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody Student student) {
        int flag = studentService.update(student);
        if (flag > 0) {
            return Result.success();
        } else {
            return Result.error();
        }
    }

    @PostMapping("/detail")
    public Result detail(Integer id) {
        return Result.success(studentService.detail(id));
    }

    @PostMapping("/query")
    public Map<String, Object> query(@RequestBody Student student) {
        PageInfo<Student> pageInfo = studentService.query(student);
        return Result.success(pageInfo);
    }

    @PostMapping("info")
    @RequiresRoles(type = Role.STUDENT)
    public Result info(){
        String token = UserThreadLocal.get();
        UserData userData = (UserData) redisUtil.get(token);
        return  Result.success(studentService.detail(userData.getId()));
    }
}
