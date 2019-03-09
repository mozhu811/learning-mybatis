package com.crzmy;

import com.crzmy.entity.Employee;
import com.crzmy.mapper.EmployeeMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;


public class AppTest {
	@Test
	public void test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Class<?> clazz = EmployeeMapper.class;
		Method method = clazz.getDeclaredMethod("findEmpByEmpId",Integer.class);
		System.out.println(method.getDeclaringClass());
	}

	/*
	1. SqlSession代表和数据库的一次会话,使用后必须关闭.
	2. SqlSession和Connection一样,都是非线程安全的.每次使用都应该去获取新的对象.
	3. Mapper接口没有实现类,但是mybatis会为其创建一个代理对象.
	4. 2个重要配置文件
		4.1. mybatis-config.xml文件:全局配置文件,包括数据库连接池信息,事务管理器等.
		4.2. *Mapper.xml:sql映射文件,保存sql语句的映射信息.
	 */



	/*
	1. 根据mybatis-config.xml文件创建一个SqlSessionFactory对象
	2. 通过SqlSessionFactory得到SqlSession对象,有SqlSession对象来进行数据库操作
	3. 使用*Mapper.xml配置文件中的sql标识来告知mybatis执行对应的sql语句
	 */
	/**
	 * 老版本的查询流程
	 * @throws IOException IO异常
	 */
	@Test
	public void test1() throws IOException {
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();

		// 获取SqlSession对象
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			Employee employee = sqlSession.selectOne("com.crzmy.mapper.EmployeeMapper.findEmpByEmpId",1);
			System.out.println(employee);
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * 新版本查询流程
	 * 接口式
	 *
	 * 解耦合,类型检查
	 * @throws IOException IO异常
	 */
	@Test
	public void test2() throws IOException{
		// 获取SqlSessionFactory对象
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		// 获取SqlSession对象
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			// 会为接口创建一个代理对象,由代理对象去执行数据库操作.
			/*
			org.apache.ibatis.binding.MapperProxy@3c0ecd4b
			 */
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Employee employee = mapper.findEmpByEmpId(1);
			System.out.println(employee);
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	@Test
	public void test3() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Employee employee = mapper.findByIdAndNameWithGender(1,"ray",1);
			System.out.println(employee);
		}
	}

	@Test
	public void test4() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			List<Employee> employees = mapper.findEmpsByGender(1);
			for (Employee e : employees){
				System.out.println(e);
			}
		}
	}

	/**
	 * 单条记录封装map
	 * @throws IOException
	 */
	@Test
	public void test5() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Map<String, Object> emp = mapper.findEmpByEmpIdReturnMap(1);
			System.out.println(emp);
		}
	}

	/**
	 * 多条记录封装map
	 * @throws IOException
	 */
	@Test
	public void test6() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Map<String, Employee> emps = mapper.findEmpsByGenderReturnMap(1);
			for (Map.Entry kv : emps.entrySet()){
				System.out.println(kv.getKey() + " - " + kv.getValue());
			}
		}
	}

	/**
	 * 联合查询, 获取雇员信息及所在部门信息
	 * @throws IOException
	 */
	@Test
	public void test7() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Employee emp = mapper.findEmpWithDeptByEmpId(1);
			System.out.println(emp);
		}
	}

	/**
	 * 测试员工登录
	 * @throws IOException IO异常
	 */
	@Test
	public void testLogin() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Integer count = mapper.empLogin(1, "123456");
			if (count > 0){
				System.out.println("登录成功!");
			} else {
				System.out.println("登录失败!");
			}
		}
	}

	/**
	 * 测试CRUD
	 */
	@Test
	public void testCRUD() throws IOException {
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			// 查询id为1的雇员
			Employee employee = mapper.findEmpByEmpId(1);
			System.out.println(employee);

			// 新建雇员对象
			Employee newEmp = new Employee(null,"123456","Bob",1,"Bob@163.com",null);

			// 添加雇员对象,获取添加后的主键
			mapper.insertEmp(newEmp);
			System.out.println(newEmp.getEmpId());

			boolean flag;
			// 修改雇员信息,并获取执行结果
			flag = mapper.modifyEmp(new Employee(5,"qweasd","Lucy",0,"lucy@163.com", null));
			System.out.println(flag);

			// 删除雇员信息,并获取执行结果
			flag = mapper.deleteEmpById(4);
			System.out.println(flag);
			sqlSession.commit();
		}
	}
	/**
	 * 根据配置文件创建一个SqlSessionFactory对象
	 * @throws IOException IO异常
	 */
	private SqlSessionFactory getSqlSessionFactory() throws IOException {
		String resource = "mybatis-config.xml";
		InputStream inputStream = Resources.getResourceAsStream(resource);
		// 获取SqlSessionFactory对象
		return new SqlSessionFactoryBuilder().build(inputStream);
	}
}
