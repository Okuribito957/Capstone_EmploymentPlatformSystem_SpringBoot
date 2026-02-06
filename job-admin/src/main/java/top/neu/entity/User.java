package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

import java.util.Date;

@Data
public class User extends Entity {

    private Integer id;
    private String userName;
    private String name;
    private String password;
    private Date loginTime;
}
