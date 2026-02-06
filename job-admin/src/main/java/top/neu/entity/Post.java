package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

@Data
public class Post extends Entity {

	private Integer id;
	private String name;
	private String education;
	private String salary;
	private String description;
	private Integer companyId;
}