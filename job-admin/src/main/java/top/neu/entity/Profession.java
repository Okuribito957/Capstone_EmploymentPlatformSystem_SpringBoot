package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

@Data
public class Profession extends Entity {

	private Integer id;
	private String name;
	private Integer parentId;
}