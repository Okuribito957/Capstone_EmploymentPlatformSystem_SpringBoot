package top.neu.entity;


import lombok.Data;
import top.neu.utils.Entity;
import java.util.Date;

@Data
public class Favor extends Entity {

	private Integer id;
	private Integer companyId;
	private Integer postId;
	private Integer studentId;
	private Date favorDate;

	private Post post;
	private Company company;
	private Student student;
}