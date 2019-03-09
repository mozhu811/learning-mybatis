package com.crzmy.entity;


/**
 * ProjectName: LearningMybatis
 * Description: 雇员表实体类
 * CreateDate: 2018/08/21 19:36
 *
 * @author Chen Rui
 * @version 1.0
 **/

public class Employee {
	/**
	 * 雇员id
	 */
	private Integer empId;

	/**
	 * 雇员密码
	 */
	private String empPwd;

	/**
	 * 雇员名字
	 */
	private String empName;

	/**
	 * 雇员性别
	 */
	private Integer empGender;

	/**
	 * 雇员邮箱
	 */
	private String empEmail;

	/**
	 * 雇员部门
	 */
	private Department department;


    public Employee(Integer empId, String empPwd, String empName, Integer empGender, String empEmail, Department department) {
        this.empId = empId;
        this.empPwd = empPwd;
        this.empName = empName;
        this.empGender = empGender;
        this.empEmail = empEmail;
        this.department = department;
    }

    public Employee() {
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmpPwd() {
        return empPwd;
    }

    public void setEmpPwd(String empPwd) {
        this.empPwd = empPwd;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public Integer getEmpGender() {
        return empGender;
    }

    public void setEmpGender(Integer empGender) {
        this.empGender = empGender;
    }

    public String getEmpEmail() {
        return empEmail;
    }

    public void setEmpEmail(String empEmail) {
        this.empEmail = empEmail;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", empPwd='" + empPwd + '\'' +
                ", empName='" + empName + '\'' +
                ", empGender='" + empGender + '\'' +
                ", empEmail='" + empEmail + '\'' +
                ", department=" + department +
                '}';
    }
}
