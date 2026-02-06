package top.neu.entity;

import lombok.Data;
import top.neu.utils.Entity;

@Data
public class UserMenu extends Entity {

	private Integer userId;
	private Integer menuId;
}