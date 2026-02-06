package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

@Data
public class Intention extends Entity {

	private Integer id;
	private String name;
	private String salary;
	private String city;
	private String type;
	private String industry;
	private Integer resumeId;

	private Resume resume;
}