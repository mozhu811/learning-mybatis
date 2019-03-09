package com.crzmy.mapper;

import com.crzmy.entity.Employee;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * ProjectName: LearningMybatis
 * Description: 雇员的Mapper接口
 * CreateDate: 2018/08/21 19:36
 * UpdateDate: 2018/08/21 19:36
 * UpdateRemark: 添加方法 findEmpWithDeptByEmpId,查询雇员信息及其所在部门
 *
 * @author Chen Rui
 * @version 1.0
 **/
public interface EmployeeMapper {

    /**
     * 雇员登录
     * @param empId 雇员id
     * @param empPwd 雇员密码
     * @return  数据库中的记录数
     */
    Integer empLogin(@Param("empId") Integer empId, @Param("empPwd") String empPwd);

    /**
     * 通过雇员id查询雇员
     * @param empId 雇员id
     * @return  Employee对象
     */
    Employee findEmpByEmpId(Integer empId);

    /**
     * 根据性别查询雇员集合
     * @param gender 性别
     * @return Employee对象组成的List集合
     */
    List<Employee> findEmpsByGender(Integer gender);

    /**
     * 根据雇员id获取雇员
     * @param empId 雇员id
     * @return  由Employee属性为key, 对应值为value的Map映射
     */
    Map<String, Object> findEmpByEmpIdReturnMap(Integer empId);

    /**
     * 根据性别查询雇员
     * @param gender    性别
     * @return  由主键作为key, Employee对象作为value的Map
     */
    @MapKey("empId")
    Map<String, Employee> findEmpsByGenderReturnMap(Integer gender);

    /**
     * 根据雇员id查询雇员并获取所在部门
     * @param empId 雇员id
     * @return Employee对象
     */
    Employee findEmpWithDeptByEmpId(Integer empId);

    /**
     * 通过id,名字和性别查找雇员
     * @param empId 雇员id
     * @param empName   雇员名字
     * @param gender    雇员性别
     * @return  Employee对象
     */
    Employee findByIdAndNameWithGender(@Param("empId") Integer empId, @Param("empName") String empName, Integer gender);
    /**
     * 添加雇员
     * @param emp 雇员对象
     * @return 返回雇员id
     */
    Integer insertEmp(Employee emp);

    /**
     * 修改雇员信息
     * @param emp 雇员对象
     * @return 是否修改成功
     */
    boolean modifyEmp(Employee emp);

    /**
     * 删除雇员
     * @param empId 雇员id
     * @return 是否删除成功
     */
    boolean deleteEmpById(Integer empId);







    /**
     * 测试DataSourceProductId
     * 指定不同的数据库厂商实现多种数据库操作
     * @param empId 雇员id
     * @return Employee对象
     */
    Employee testDataSourceProductId(Integer empId);
}
