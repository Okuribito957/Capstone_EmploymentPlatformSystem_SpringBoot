package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

@Data
public class Resume extends Entity {

	private Integer id;
	private String name;
	private String jobStatus;
	private String evaluate;
	private String skill;
	private Integer studentId;
	private Integer status; // 0关闭|1开放
}