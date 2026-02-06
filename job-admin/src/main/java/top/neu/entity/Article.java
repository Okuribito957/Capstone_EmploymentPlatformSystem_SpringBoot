package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

import java.util.Date;

@Data
public class Article extends Entity {

    private Integer id;
    private Integer channelId;
    private String title;
    private String titleImg;
    private String summary;
    private String author;
    private String url;
    private String content;
    private Integer sort;
    private Date createDate;
    private Integer createUser;
    private Date updateDate;

    private Integer views; //浏览量
}