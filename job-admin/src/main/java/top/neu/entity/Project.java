package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;
import java.util.Date;

@Data
public class Project extends Entity {

	private Integer id;
	private String name;
	private String description;
	private Date startDate;
	private Date endDate;
	private Integer resumeId;

	private Resume resume;
}