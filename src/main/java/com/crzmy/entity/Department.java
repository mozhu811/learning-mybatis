package com.crzmy.entity;


/**
 * ProjectName: LearningMybatis
 * Description: 部门表实体类
 * CreateDate: 2018/08/21 19:36
 *
 * @author Chen Rui
 * @version 1.0
 **/

public class Department {

	/**
	 * 部门id
	 */
	private Integer deptId;

	/**
	 * 部门名
	 */
	private String deptName;

	public Department(Integer deptId, String deptName) {
		this.deptId = deptId;
		this.deptName = deptName;
	}

    public Department() {
    }

    @Override
    public String toString() {
        return "Department{" +
                "deptId=" + deptId +
                ", deptName='" + deptName + '\'' +
                '}';
    }
}
