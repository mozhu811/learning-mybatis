* [项目介绍](#项目介绍)
* [Mybatis参数处理方式](#mybatis参数处理方式)
	* [通常方式](#通常方式)
	* [通过POJO传递](#通过pojo传递)
	* [通过Map容器传递](#通过map容器传递)
	* [通过DTO传递](#通过dto传递)
	* [其他情况](#其他情况)
* [源码分析Mybatis的参数处理过程](#源码分析mybatis的参数处理过程)
* [Mybatis的参数值获取方式](#mybatis的参数值获取方式)
	* [#{}方式获取参数值的其他用法](#方式获取参数值的其他用法)
* [Select元素及部分属性用法](#select元素及部分属性用法)
	* [返回单个对象](#返回单个对象)
	* [返回List&lt;T&gt;](#返回listt)
	* [返回单条记录封装一个Map映射](#返回单条记录封装一个map映射)
	* [返回多条记录封装一个Map映射](#返回多条记录封装一个map映射)
	* [select元素的属性](#select元素的属性)
		* [部分属性使用示例](#部分属性使用示例)
    
## 项目介绍
该repo为学习Mybatis时的记录,该文档主要记录Mybatis的相关知识和源码分析.

## Mybatis参数处理方式
### 通常方式
1. 单个参数,mybatis不会做特殊处理   
\#{参数名}就可以取出参数值

2. 多个参数,mybatis会做特殊处理  
多个参数会封装成一个map  
key: param1,param2,param3...或者其他参数索引  
value: 传入的值  
\#{} 就是从map找那个获取指定的key对应的value
在mapper.xml文件中默认使用#{param1},#{param2}这样的方式来获取传入参数
  
3. 命名参数:明确的指定封装参数时map的key  
使用@Param(key)注解  
例如 Employee findByIdAndName(@Param("id") Integer id, @Param("name") String name);

### 通过POJO传递
如果多个参数是业务逻辑的数据类型,可以直接传入POJO  
使用#{属性字段}来取出POJO的属性值

### 通过Map容器传递
如果多个参数没有对应的POJO,则可以直接使用Map  
使用#{key}来取出对应的值

```java
public interface EmployeeMapper{
    /*...*/
    // EmployeeMapper中定义方法
    Employee getEmpByMap(Map<String, Object> map);
}

```
然后在EmployeeMapper.xml文件中如下配置

```xml
<select id="getEmpByMap" resultType="com.crzmy.entity.Employee">
    SELECT * FROM tb_employee WHERE emp_id = #{id} AND emp_name = #{name}
</select>
```
测试方法代码

```java

public class AppTest{
	@Test
	public void test(){
		/*
		...
		*/
		// 测试方法中主要代码
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		Map<String, Object> map = new HashMap<>();
		map.put("id",1);
		map.put("name", "jack");
		Employee emp = mapper.getEmpByMap(map);
		System.out.println(emp);
		/*
		...
		*/
    }
}

```
### 通过DTO传递
如果多个参数不是业务模型中的数据,但是经常使用,可以使用一个DTO即数据传输对象

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO{
	private Integer empId;
	private String empName;
}
```

### 其他情况
```java
public interface EmployeeMapper{
	/**
	* 通过id和名字查找雇员
	* 
	* 这种情况雇员id取值方式有1种
	* #{empId}或者#{param1}
	* 而雇员名字则只能通过1个方式
	* #{param2}
	* 
	* @param empId 雇员id
	* @param empName 雇员名字
	* @return 雇员对象
	*/
	Employee getEmp(@Param("empId") Integer empId, String empName);
    
	/**
	* 通过id和雇员信息更新对象
	* 
	* 这种情况雇员id取值方式只有1种
	* #{param1}
	* 而雇员对象中的属性有2种方式,以雇员名字为例
	* #{param2.empName} 或者 #{emp.empName}
	* 
	* @param empId  待更新的雇员id
	* @param emp    新的雇员对象
	* @return  是否更改成功
	*/
	boolean updateEmp(Integer empId, @Param("emp") Employee emp);
    
	/**
	* 通过一个雇员id列表批量查询雇员信息
	* 
	* mybatis对Collection或者数组也会特殊处理
	* 把集合或者数组封装在Map中
	* 
	* 如果是List集合也可以直接使用"list"这个key来获取
	* 比如取出list中的第一个id
	* #{list[0]}
	* 
	* @param ids    雇员id列表
	* @return   雇员对象列表
	*/
	List<Employee> getEmpsByIds(List<Integer> ids);
}
```
## 源码分析Mybatis的参数处理过程
Mapper接口

```java
public interface EmployeeMapper{
	/**
	* 通过id,名字和性别查询雇员
	* @param id 雇员id
	* @param name   雇员名字
	* @param gender 雇员性别
	* @return   Employee对象
	*/
	Employee findByIdAndNameWithGender(@Param("empId") Integer id, @Param("empName") String name, String gender);
}
```
分析入口

```java
public class AppTest{
	@Test
	public void test(){
		/*
		...
		*/
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		Employee employee = mapper.findByIdAndNameWithGender(1,"cruii","1");
		/*
		...
		*/
	}
}
```
第一行代码,mybatis会使用MapperProxyFactory类中的newInstance(MapperProxy<T> mapperProxy)方法来使用JDK动态代理生成EmployeeMapper代理对象  
通过代理对象来与数据库进行会话. 
 
```java
public class MapperProxyFactory{
	/*
	...
	*/
	protected T newInstance(MapperProxy<T> mapperProxy) {
	    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
	}
	    
	public T newInstance(SqlSession sqlSession) {
	    final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
	    return newInstance(mapperProxy);
	}
}
```
Employee employee = mapper.findByIdAndNameWithGender(1,"cruii","1");
该行代码会调用代理对象的invoke方法

```java
public class MapperProxy<T> implements InvocationHandler, Serializable {
/*
...
 */
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	try {
		if (Object.class.equals(method.getDeclaringClass())) {
			return method.invoke(this, args);
		} else if (isDefaultMethod(method)) {
			return invokeDefaultMethod(proxy, method, args);
		}
	} catch (Throwable t) {
		throw ExceptionUtil.unwrapThrowable(t);
	}
	
	final MapperMethod mapperMethod = cachedMapperMethod(method);
	return mapperMethod.execute(sqlSession, args);
	}
	
	/*
	...
	*/
}
```
如果是Object类里的方法,如toString(),hashCode()等方法,则直接通过反射执行  
 
> MapperProxy是一个InvocationHandler,在使用JDK动态代理生成对象时使用,  
> 会根据该接口生成动态代理对象,然后利用反射调用实际对象的目标方法.  
> 然而动态代理对象里面的方法是有接口(interface)声明的.  
> 但是动态代理对象也能调用toString(),hashCode()等方法,而这些方法就是从Object类继承过来的.  
> 所以if (Object.class.equals(method.getDeclaringClass()))这行代码的作用就是:  
> 如果利用动态代理对象调用的是toString(),hashCode()等从Object类继承的方法,则直接反射调用.  
> 如果是接口声明的方法,则通过下面的MapperMethod执行.

此时,传入的方法是com.crzmy.mapper.EmployeeMapper.findByIdAndNameWithGender  
通过method.getDeclaringClass()得到的结果是interface com.crzmy.mapper.EmployeeMapper  
所以直接通过MapperProxy类的cachedMapperMethod方法生成一个MapperMethod对象.

```java
public class MapperProxy {
	/*
	...
	 */
	private MapperMethod cachedMapperMethod(Method method) {
		MapperMethod mapperMethod = methodCache.get(method);
		if (mapperMethod == null) {
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			methodCache.put(method, mapperMethod);
		}
		return mapperMethod;
	}
	/*
	...
	 */
}
```

该方法首先在缓存中查找是否存在目标方法,如果不存在,则创建一个新的MapperMethod对象并且缓存,每一个MapperMethod对象都代表了SQL映射文件mapper.xml里的一个SQL语句或者FLUSH配置,对应的SQL语句通过全类名和方法名从Configuration对象中获得.  
这样当以后再次调用同一个mapper方法时直接返回缓存中的对象,不必再次创建,节省内存.  
当获取了MapperMethod对象后,则通过该对象的execute方法执行目标方法.  
MapperMethod类中有两个成员变量,SqlCommand对象和MethodSignature对象.  
在创建MapperMethod对象时,会同时初始化这两个对象.  

* SqlCommand类

该类负责封装SQL语句的标签类型(如:SELECT,UPDATE,DELETE,INSERT)和目标方法名  

SqlCommand类部分源码如下

```java
public static class SqlCommand {
	/*
	name 负责存放调用的目标方法名  
	type 负责存放SQL语句的类型
	*/
	private final String name;
	private final SqlCommandType type;
	
	public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
		final String methodName = method.getName();
		final Class<?> declaringClass = method.getDeclaringClass();
		MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
		  configuration);
		if (ms == null) {
			if (method.getAnnotation(Flush.class) != null) {
			  name = null;
			  type = SqlCommandType.FLUSH;
			} else {
			  throw new BindingException("Invalid bound statement (not found): "
			      + mapperInterface.getName() + "." + methodName);
			}
		} else {
			/*
			获取name和type
			*/
			name = ms.getId();
			type = ms.getSqlCommandType();
			if (type == SqlCommandType.UNKNOWN) {
			  throw new BindingException("Unknown execution method for: " + name);
			}
		}
	}
	    
	/*
	...
	*/
}
```
MapperMethod中的resolveMappedStatement方法

```java
public class MapperMethod{

	/*
	...
	*/
	 
	private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
	Class<?> declaringClass, Configuration configuration) {
		String statementId = mapperInterface.getName() + "." + methodName;
		if (configuration.hasStatement(statementId)) {
			return configuration.getMappedStatement(statementId);
		} else if (mapperInterface.equals(declaringClass)) {
			return null;
		}
		for (Class<?> superInterface : mapperInterface.getInterfaces()) {
			if (declaringClass.isAssignableFrom(superInterface)) {
				MappedStatement ms = resolveMappedStatement(superInterface, methodName,
				  declaringClass, configuration);
				if (ms != null) {
					return ms;
				}
			}
		}
		return null;
	}
	    
	/*
	...
	*/
}
```
在实例化SqlCommand的过程中,在构造方法里首先获取到目标方法的方法名以及目标方法声明的类对应的Class对象.  
进入resolveMappedStatement方法,首先通过传入的EmployeeMapper接口的Class对象,获取全类名,然后拼接目标方法名组成statementId.再判断Configuration的mappedStatements中是否有对应的key,若true,则返回mappedStatements中对应key的MappedStatement对象.  
回到SqlCommand的构造器中,此时MappedStatement ms已被赋值为对应的目标方法的MappedStatement对象.直接通过ms.getId()和ms.getSqlCommandType()方法获取目标方法名和SQL类型.  
在本例中,即:  
name: com.crzmy.mapper.EmployeeMapper.findByIdAndNameWithGender  
type: SELECT  
至此SqlCommand对象初始化完毕.

* MethodSignature类

该类负责封装方法的参数和返回值类型等信息  

MethodSignature部分源码

```java
public static class MethodSignature {
	
	private final boolean returnsMany;
	private final boolean returnsMap;
	private final boolean returnsVoid;
	private final boolean returnsCursor;
	private final Class<?> returnType;
	private final String mapKey;
	private final Integer resultHandlerIndex;
	private final Integer rowBoundsIndex;
	private final ParamNameResolver paramNameResolver;
	
	public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
		/*
		获取返回值类型
		*/
		Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
		  
		/*
		根据不同的返回值类型进行处理,并赋值给returnType
		*/
		if (resolvedReturnType instanceof Class<?>) {
			this.returnType = (Class<?>) resolvedReturnType;
		} else if (resolvedReturnType instanceof ParameterizedType) {
			this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
		} else {
			this.returnType = method.getReturnType();
		}
		this.returnsVoid = void.class.equals(this.returnType);
		this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
		this.returnsCursor = Cursor.class.equals(this.returnType);
		this.mapKey = getMapKey(method);
		this.returnsMap = this.mapKey != null;
		this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
		this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
		this.paramNameResolver = new ParamNameResolver(configuration, method);
	}
	/*
	...
	*/
}
```
MethodSignature类的构造方法会首先调用TypeParameterResolver类的resolveReturnType方法来获取目标方法的返回值类型,传入的参数就是目标方法对应的Method对象和EmployeeMapper类对象  

TypeParameterResolver类部分源码

```java
public class TypeParameterResolver {
	/*
	...
	*/
	  
	public static Type resolveReturnType(Method method, Type srcType) {
		Type returnType = method.getGenericReturnType();
		Class<?> declaringClass = method.getDeclaringClass();
		return resolveType(returnType, srcType, declaringClass);
	}
	  
	/*
	...
	*/
	  
	private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
		if (type instanceof TypeVariable) {
			return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
		} else if (type instanceof ParameterizedType) {
			return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
		} else if (type instanceof GenericArrayType) {
			return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
		} else {
			return type;
		}
	}
	  
	/*
	...
	*/
}
```
resolveReturnType直接获取到目标方法的返回值类型,在该例子中即class com.crzmy.entity.Employee  
然后获取方法所属的类对象,即interface com.crzmy.mapper.EmployeeMapper  
再进入resolveType方法,该方法判断返回值类型是否是TypeVariable(类型变量), ParameterizedType(参数化类型)或者GenericArrayType(泛型数组).

> void method(E e){}中的E就是类型变量  
> Map<String, Integer> map; map的Type就是ParameterizedType  
> List<String>[] list; list的Type就是GenericArrayType

很显然,本例子中以上都不是,则直接返回class com.crzmy.entity.Employee.  
然后在MethodSignature类的构造方法的最后一句

```java
this.paramNameResolver = new ParamNameResolver(configuration, method);
```
该条语句实例化了一个ParamNameResolver类对象,该类主要的作用就是解析参数.

ParamNameResolver部分源码

```java
public class ParamNameResolver {

	private static final String GENERIC_NAME_PREFIX = "param";
	
	private final SortedMap<Integer, String> names;
	
	private boolean hasParamAnnotation;
	
	public ParamNameResolver(Configuration config, Method method) {
		/*
		存储目标方法的参数对应的Class对象
		*/
		final Class<?>[] paramTypes = method.getParameterTypes();
		    
		/*
		存储目标方法的注解对象数组,每一个方法的参数都有一个注解数组
		*/
		final Annotation[][] paramAnnotations = method.getParameterAnnotations();
		final SortedMap<Integer, String> map = new TreeMap<Integer, String>();
		    
		/*
		存储目标方法的参数个数
		*/
		int paramCount = paramAnnotations.length;
		// get names from @Param annotations
		for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
			if (isSpecialParameter(paramTypes[paramIndex])) {
				// skip special parameters
				continue;
			}
			String name = null;
			for (Annotation annotation : paramAnnotations[paramIndex]) {
				if (annotation instanceof Param) {
					hasParamAnnotation = true;
					name = ((Param) annotation).value();
					break;
				}
			}
			if (name == null) {
				// @Param was not specified.
				if (config.isUseActualParamName()) {
					name = getActualParamName(method, paramIndex);
				}
				if (name == null) {
					// use the parameter index as the name ("0", "1", ...)
					// gcode issue #71
					name = String.valueOf(map.size());
				}
			}
			map.put(paramIndex, name);
		}
		names = Collections.unmodifiableSortedMap(map);
	}
	  
	private String getActualParamName(Method method, int paramIndex) {
		if (Jdk.parameterExists) {
		  return ParamNameUtil.getParamNames(method).get(paramIndex);
		}
		return null;
	}
	  
	/*
	...
	*/
 
}
```
ParamNameUtil工具类源码

```java
@UsesJava8
public class ParamNameUtil {
	public static List<String> getParamNames(Method method) {
		return getParameterNames(method);
	}
	
	public static List<String> getParamNames(Constructor<?> constructor) {
		return getParameterNames(constructor);
	}
	
	private static List<String> getParameterNames(Executable executable) {
		final List<String> names = new ArrayList<String>();
		final Parameter[] params = executable.getParameters();
		for (Parameter param : params) {
			names.add(param.getName());
		}
		return names;
	}
	
	private ParamNameUtil() {
		super();
	}
}
```

通过isSpecialParameter(paramTypes[paramIndex])判断是否是RowBounds和ResultHandler特殊类型,如果true,则跳过.  
紧接着判断每一个注解是否是Param注解,如果true,则hasParamAnnotation赋值为true表示该方法有@Param注解,然后直接把Param注解的value值赋值给name,在本例子中即empId和empName.  
如果没有使用@Param注解,则判断是否开启了useActualParamName,  

* 如果为true,则调用getActualParamName方法,并通过ParamNameUtil工具类获取目标方法的参数名,再把参数名存储到List中,接着根据传入的索引获取对应的参数名.然后把参数索引和参数名存放到map中.  

* 如果为false,那么就会使用参数索引作为name.  

当所有参数都判断之后,通过Collections.unmodifiableSortedMap(map)返回一个只读的Map容器赋值给names,同样存放着参数索引和参数名的映射关系,即:  
当useActualParamName()为true时:  
0 -> "id"  
1 -> "name"  
2 -> "gender"  

当useActualParamName()为false时:  
0 -> "id"  
1 -> "name"  
2 -> 2

> 补充  
> 从JDK8开始,可以通过打开javac -parameters,然后通过method.getParameters()获取到参数的名称.  
> 上面代码中的Executable对象就是Java的方法Method类和构造器Constructor类的父类,拥有getParameters()方法.
> 在本例中即:id,name,gender  
> 如果是JDK7及以下,则获取到的是arg0,arg1,arg2等无意义的参数名.

<strong>本例默认是useActualParamName()为false,所以names的存储情况为第二种情况</strong>  

至此ParamNameResolver对象实例化完成,然后赋值给MethodSignature的paramNameResolver变量.紧接着MethodSignature对象也实例化完成,同时MapperMethod也初始化完成,最后通过methodCache.put(method, mapperMethod);将mapperMethod对象缓存,存储的是目标方法的Method对象和mapperMethod的映射.  
现在就正式开始进入MapperMethod的execute方法,该方法执行对应的SQL语句并且根据返回值类型返回值.

MapperMethod类execute方法

```java
public class MapperMethod {
	private final SqlCommand command;
	private final MethodSignature method;
	
	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		this.command = new SqlCommand(config, mapperInterface, method);
		this.method = new MethodSignature(config, mapperInterface, method);
	}
	
	public Object execute(SqlSession sqlSession, Object[] args) {
		Object result;
		switch (command.getType()) {
			case INSERT: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.insert(command.getName(), param));
				break;
			}
			case UPDATE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.update(command.getName(), param));
				break;
			}
			case DELETE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.delete(command.getName(), param));
				break;
			}
			case SELECT:
				if (method.returnsVoid() && method.hasResultHandler()) {
					executeWithResultHandler(sqlSession, args);
					result = null;
				} else if (method.returnsMany()) {
					result = executeForMany(sqlSession, args);
				} else if (method.returnsMap()) {
					result = executeForMap(sqlSession, args);
				} else if (method.returnsCursor()) {
					result = executeForCursor(sqlSession, args);
				} else {
					Object param = method.convertArgsToSqlCommandParam(args);
					result = sqlSession.selectOne(command.getName(), param);
				}
				break;
			case FLUSH:
				result = sqlSession.flushStatements();
				break;
			default:
				throw new BindingException("Unknown execution method for: " + command.getName());
		}
		if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
			throw new BindingException("Mapper method '" + command.getName() 
			+ " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
		}
		return result;
	}
	  
	/*
	...
	*/
}
```
首先通过SqlCommand的实例化对象command获取SQL类型,在本例中即SELECT,进入对应的case语句块.  
其次根据MethodSignature的实例化对象method存储的目标方法的返回值类型判断,在本例中返回值为一个Employee对象,所以直接到达Object param = method.convertArgsToSqlCommandParam(args);  
然后进入method.convertArgsToSqlCommandParam方法

```java
public static class MethodSignature {
	/*
	...
	*/
		
	public Object convertArgsToSqlCommandParam(Object[] args) {
		return paramNameResolver.getNamedParams(args);
	}
		
	/*
	...
	*/
}
```
该方法又调用了ParamNameResolver的getNamedParams方法,该方法是解析参数的核心方法,进入该方法

```java
public class ParamNameResolver {
	private static final String GENERIC_NAME_PREFIX = "param";
	
	private final SortedMap<Integer, String> names;
	
	private boolean hasParamAnnotation;
		
	/*
	...
	*/
		
	public Object getNamedParams(Object[] args) {
		final int paramCount = names.size();
		if (args == null || paramCount == 0) {
			return null;
		} else if (!hasParamAnnotation && paramCount == 1) {
			return args[names.firstKey()];
		} else {
			final Map<String, Object> param = new ParamMap<Object>();
			int i = 0;
			for (Map.Entry<Integer, String> entry : names.entrySet()) {
				param.put(entry.getValue(), args[entry.getKey()]);
				// add generic param names (param1, param2, ...)
				final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
				// ensure not to overwrite parameter named with @Param
				if (!names.containsValue(genericParamName)) {
					param.put(genericParamName, args[entry.getKey()]);
				}
				i++;
			}
			return param;
		}
	}
}
```
调用的目标方法

```java
Employee findByIdAndNameWithGender(@Param("empId") Integer id, @Param("empName") String name, String gender);
```
此时传入的参数args的内容是

```text
1,"cruii","1"
```
而names存储的内容是

```text
0 -> "id"  
1 -> "name"  
2 -> 2 
```
先来看只有一个参数的情况  
假设现在传入的参数args数组里只有一个Integer类型的1,没有使用@Param注解,并且names存储的只有一个"0"->"id"映射.  
则

```java
return args[names.firstKey()];

/* 
则相当于  
return args[0];返回 1 
*/
``` 


如果使用了@Param注解,则遍历Map容器names,以value作为key和args数组对应索引的值作为value存储到Map容器param中  
此时相当于

```java  
param.put("id", 1);
```
然后返回param.
再回到原来的例子,多个参数情况  
同样遍历Map容器names,以value作为key和args数组对应索引的值作为value存储到Map容器param中.并且会根据GENERIC_NAME_PREFIX常量即"param"和当前的索引拼装成新的字符串,即param1,param2,...,paramN.然后和args数组里对应索引的值存储到Map容器param中,最后遍历完成后param的存储情况是:

```text
"id" -> 1
"param1" -> 1  
"name" -> "cruii"
"param2" -> "cruii"  
2 -> "1"  
"param3" -> "1"
```
所以可以在SQL映射文件中如下两种方式配置都可以获取到参数的值

第一种方式:

```xml
<select id="findByIdAndNameWithGender" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_id = #{empId}
    AND
        emp_name = #{empName}
    AND
        emp_gender = #{param3}
</select>
```
第二种方式:
 
```xml
<select id="findByIdAndNameWithGender" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_id = #{param1}
    AND
        emp_name = #{param2}
    AND
        emp_gender = #{param3}
</select>
```
至此,Mybatis的参数处理过程源码分析完毕.

## Mybatis的参数值获取方式
1. \#{} 方式: 以预编译的形式将参数添加到SQL语句中,防止SQL注入
2. ${} 方式: 获取的值会直接拼装在SQL语句中,会有SQL注入风险

大多数情况下,都是使用#{}方式来获取参数的值,但是也有例外.比如以下情况:  
背景: 数据库中有多张表,记录了2012-2017年的工资数据,表的命名都是[年份_salary],如:2017\_salary  
需求: 查询某年的某员工的工资数据  

此时如果想通过传入的参数来获取目标表名,则不能使用#{},就应该使用${}.  

假设EmployeeMapper中声明如下方法

```java
public interface EmployeeMapper{
	Salary findSalaryByEmpId(Map map);
}
```
测试代码

```java
public class AppTest{
	@Test
	public void test3() throws IOException{
		SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
		try(SqlSession sqlSession = sqlSessionFactory.openSession()){
			EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
			Map<String, Object> map = new HashMap<>();
			map.put("empId",1);
			/*
			添加目标表名
			*/
			map.put("tableName","2017_salary");
			Salary salary = findSalaryByEmpId(map);
			System.out.println(salary);
		}
	}
}
```
如果使用#{}方式

```xml
<select id="findSalaryByEmpId" resultType="com.crzmy.entity.Salary">
	SELECT
		* 
	FROM
		#{tableName}
	WHERE
		emp_id = #{empId}
</select>
```
则生成的SQL语句为

```sql
select * from ? where emp_id = ?;
```

很显然这是语法错误.  
现在使用${}方式

```xml
<select id="findSalaryByEmpId" resultType="com.crzmy.entity.Salary">
	SELECT
		* 
	FROM
		${tableName}
	WHERE
		emp_id = #{empId}
</select>
```
则生成的SQL语句为

```sql
select * from 2017_salary where emp_id = ?;
```
正常执行.

### \#{}方式获取参数值的其他用法

* 规定参数的规则
	* javaType
	* jdbcType
	* mode
	* numericScale
	* resultMap
	* typeHandler
	* jbbcTypeName
	* expression

从中讲几个代表性的	
jdbcType通常需要在特定的情况下配置	
在数据为null的时候,有些数据库可能不支持Mybatis对null的默认处理,比如Oracle数据库,在插入null值时会报错: 无效的列类型.	
Mybatis对null的默认映射类型是JdbcType.OTHER

枚举类JdbcType部分代码.

```java
public enum JdbcType {
  /*
   * This is added to enable basic support for the
   * ARRAY data type - but a custom type handler is still required
   */
  ARRAY(Types.ARRAY),
  BIT(Types.BIT),
  TINYINT(Types.TINYINT),
  /*
  ...
  */
  
  OTHER(Types.OTHER),
  
  /*
  ...
  */

  public final int TYPE_CODE;
  private static Map<Integer,JdbcType> codeLookup = new HashMap<Integer,JdbcType>();

  static {
    for (JdbcType type : JdbcType.values()) {
      codeLookup.put(type.TYPE_CODE, type);
    }
  }

  JdbcType(int code) {
    this.TYPE_CODE = code;
  }

  public static JdbcType forCode(int code)  {
    return codeLookup.get(code);
  }

}

```
而Oracle是不支持Jdbc的OTHER类型.	
所以可以再SQL映射文件中进行如下配置:

```xml
<insert id="insertEmp">
    INSERT INTO
        tb_employee
    VALUES
        (#{empId}, #{empPwd}, #{empName, jdbcType=NULL})
</insert>
```
在需要对null值进行处理的字段后添加jdbcType=NULL.这样Oracle就能正常的处理null值.如果此时还不能插入成功,记得检查表结构的Nullable是否是Yes.	
对于null值处理的情况,还有一个解决方案.	
在mybatis-config.xml里进行配置,在settings节点中加入新的setting节点,name属性为jdbcTypeForNull,value属性为NULL

```xml
<settings>
    <!--开启日志打印-->
    <!--<setting name="logImpl" value="STDOUT_LOGGING"/>-->
    <!--开启驼峰命名转换-->
    <setting name="mapUnderscoreToCamelCase" value="true"/>
    <!--全局默认null的处理方式-->
    <setting name="jdbcTypeForNull" value="NULL"/>
</settings>
```
此时在SQL映射文件中不进行任何配置也能正常运行

```xml
<insert id="insertEmp">
    INSERT INTO
        tb_employee
    VALUES
        (#{empId}, #{empPwd}, #{empName})
</insert>
```
正常插入.

## Select元素及部分属性用法

### 返回单个对象	
在EmployeeMapper中定义方法

```java
public interface EmployeeMapper{

	Employee findEmpByEmpId(Integer empId);
}
```
在SQL映射文件EmployeeMapper.xml中添加配置

```xml
<select id="findEmpByEmpId" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_id = #{empId}
</select>
```
编写测试方法

```java
@Test
public void test() throws IOException{
	// 获取SqlSessionFactory对象
	SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
	// 获取SqlSession对象
	try(SqlSession sqlSession = sqlSessionFactory.openSession()){
		// 会为接口创建一个代理对象,由代理对象去执行数据库操作.
		/*
		org.apache.ibatis.binding.MapperProxy@3c0ecd4b
		 */
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		
		/* 获取返回的单个对象 */
		Employee employee = mapper.findEmpByEmpId(1);
		System.out.println(employee);
	} catch (Exception e){
		e.printStackTrace();
	}

}
```
这样就可以获取返回的单个对象并控制台输出.

### 返回List\<T>	
有些情况下,一个查询条件可能有多个值,所以需要封装在List中返回,这种情况只需要更改EmployeeMapper.java里的返回值类型.比如,在EmployeeMapper.java文件中添加方法List\<Employee> findEmpsByGender(String gender);该方法通过传入性别筛选数据.

```java
public interface EmployeeMapper{

	Employee findEmpByEmpId(Integer empId);
	
	List<Employee> findEmpsByGender(String gender);
}
```

EmployeeMapper.xml里添加如下配置,

```xml
<select id="findEmpsByGender" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_gender = #{gender}
</select>
```
实际上只是原来的单个对象的配置方式改了id和判断条件而已,其他并无区别

编写测试方法

```java
@Test
public void test() throws IOException{
	// 获取SqlSessionFactory对象
	SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
	// 获取SqlSession对象
	try(SqlSession sqlSession = sqlSessionFactory.openSession()){
		// 会为接口创建一个代理对象,由代理对象去执行数据库操作.
		/*
		org.apache.ibatis.binding.MapperProxy@3c0ecd4b
		 */
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		
		/* 获取返回的多个Employee对象组成的List */
		List<Employee> employees = findEmpsByGender("1");
		
		for(Employee e : employees){
			System.out.println(e);
		}
	} catch (Exception e){
		e.printStackTrace();
	}

}
```
这样就可以获取多个Employee对象组成的List.

### 返回单条记录封装一个Map映射

有些时候需要把对象的属性作为Map的key, 查询到的数据为value.	
在EmployeeMapper.java中添加方法

```java
public interface EmployeeMapper{
	Map<String, Object> findEmpByEmpIdReturnMap(Integer empId);
}
```
在EmployeeMapper.xml文件中添加select节点

```xml
<select id="findEmpByEmpIdReturnMap" resultType="map">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_id = #{empId}
</select>
```
编写测试方法

```java
@Test
public void test() throws IOException{
	SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
	try(SqlSession sqlSession = sqlSessionFactory.openSession()){
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		
		/* 获取返回的Map */
		Map<String, Object> emp = mapper.findEmpByEmpIdReturnMap(1);
		System.out.println(emp);
	}
}
```
这样就可以正常获取封装好的Map,控制台打印的结果就是

```text
{emp_name=ray, emp_pwd=123456, emp_email=crrruiii@163.com, emp_gender=1, emp_id=1}
```

### 返回多条记录封装一个Map映射
key为主键, value是封装后的对象

在EmployeeMapper.java中添加如下方法, 注意MapKey注解

```java
public interface EmployeeMapper{
	@MapKey("empId")
	Map<String, Employee> findEmpsByGenderReturnMap(String gender);
}
```
在EmployeeMapper.xml文件中添加select节点

```xml
<select id="findEmpsByGenderReturnMap" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_gender = #{gender}
</select>
```
编写测试方法

```java
@Test
public void test() throws IOException{
	SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
	try(SqlSession sqlSession = sqlSessionFactory.openSession()){
		EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
		Map<String, Employee> emps = mapper.findEmpsByGenderReturnMap("1");
		for (Map.Entry kv : emps.entrySet()){
			System.out.println(kv.getKey() + " - " + kv.getValue());
		}
	}
}
```
这样就可以获取多条记录封装的map, key为雇员id, value则为Employee对象,控制台打印结果为

```text
1 - Employee(empId=1, empPwd=123456, empName=ray, empGender=1, empEmail=crrruiii@163.com)
2 - Employee(empId=2, empPwd=123456, empName=Jack, empGender=1, empEmail=jack@163.com)
3 - Employee(empId=3, empPwd=123456, empName=Bob, empGender=1, empEmail=Bob@163.com)
6 - Employee(empId=6, empPwd=123456, empName=Bob, empGender=1, empEmail=Bob@163.com)
7 - Employee(empId=7, empPwd=123456, empName=Bob, empGender=1, empEmail=Bob@163.com)
8 - Employee(empId=8, empPwd=123456, empName=Bob, empGender=1, empEmail=Bob@163.com)
9 - Employee(empId=9, empPwd=123456, empName=Bob, empGender=1, empEmail=Bob@163.com)
```
### select元素的属性

属性 | 描述
---|---
parameterType | 将传入该条语句的参数的类的全路径或别名,这个属性是可选的.Mybatis可以通过TypeHandler推断出具体传入的参数,默认值为unset.
resultType | 该条语句返回的期望类型的类的全路径或别名,如果是集合,则是集合里包含的类型,而不是集合本身(上文的例子).该属性和resultMap不能同时使用.
resultMap | 外部resultMap的明明引用,需要在SQL映射文件中使用resultMap元素自定义映射规则,然后在该属性指定.不能和resultType同时使用
flushCache | 将其设置为 true，任何时候只要语句被调用，都会导致本地缓存和二级缓存都会被清空，默认值：false。
useCache | 将其设置为 true，将会导致本条语句的结果被二级缓存，默认值：对 select 元素为 true。
timeout | 这个设置是在抛出异常之前，驱动程序等待数据库返回请求结果的秒数。默认值为 unset（依赖驱动）。
fetchSize | 这是尝试影响驱动程序每次批量返回的结果行数和这个设置值相等。默认值为 unset（依赖驱动）。
statementType | STATEMENT，PREPARED 或 CALLABLE 的一个。这会让 MyBatis 分别使用 Statement，PreparedStatement 或 CallableStatement，默认值：PREPARED。
resultSetType | FORWARD_ONLY，SCROLL_SENSITIVE 或 SCROLL_INSENSITIVE 中的一个，默认值为 unset （依赖驱动）。
databaseId | 如果配置了 databaseIdProvider，MyBatis 会加载所有的不带 databaseId 或匹配当前 databaseId 的语句；如果带或者不带的语句都有，则不带的会被忽略。
resultOrdered | 这个设置仅针对嵌套结果 select 语句适用：如果为 true，就是假设包含了嵌套结果集或是分组了，这样的话当返回一个主结果行的时候，就不会发生有对前面结果集的引用的情况。这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
resultSets | 这个设置仅对多结果集的情况适用，它将列出语句执行后返回的结果集并每个结果集给一个名称，名称是逗号分隔的。

#### 部分属性使用示例

* resultType

指定resultType为期望类型的类全路径,就可以将查询到的记录自动映射封装成JavaBean

```xml
<select id="findEmpByEmpId" resultType="com.crzmy.entity.Employee">
    SELECT
        *
    FROM
        tb_employee
    WHERE
        emp_id = #{empId}
</select>
```

* resultMap [最强大的功能]

	* 基本用法

	使用resultMap自定义映射规则, 然后在select元素的resultMap属性指定定义好的映射规则.这样就可以再关闭驼峰命名的情况下也能正常的映射封装.
	
	```xml
	<resultMap id="BaseMap" type="com.crzmy.entity.Employee">
	    <id column="emp_id" jdbcType="INTEGER" property="empId"/>
	    <result column="emp_pwd" jdbcType="VARCHAR" property="empPwd"/>
	    <result column="emp_name" jdbcType="VARCHAR" property="empName"/>
	    <result column="emp_gender" jdbcType="CHAR" property="empGender"/>
	    <result column="emp_email" jdbcType="VARCHAR" property="empEmail"/>
	</resultMap>
	
	<select id="findEmpByEmpId" resultMap="BaseMap">
	    SELECT
	        *
	    FROM
	        tb_employee
	    WHERE
	        emp_id = #{empId}
	</select>
	```
	
	* 高级用法

		* 联合查询

		比如查询员工的同时查询出所在部门的信息	
		
		在数据库中新建表tb\_department
		
		```sql
		CREATE TABLE tb_department (
		  	dept_id int(11) NOT NULL AUTO_INCREMENT,
		  	dept_name varchar(45) DEFAULT NULL,
		  	PRIMARY KEY (dept_id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8
		```
		
		在表tb\_employee中添加dept\_id字段,
		
		```sql
		ALTER TABLE tb_employee ADD COLUMN dept_id INT(11);
		```
		新建Department类
		
		```java
		@Getter
		@Setter
		@AllArgsConstructor
		@NoArgsConstructor
		@ToString
		public class Department {
			
			/**
			 * 部门id
			 */
			private Integer deptId;
			
			/**
			 * 部门名
			 */
			private String deptName;
			
		}
		```
		
		在Employee类中添加department属性
		
		```java
		@Getter
		@Setter
		@NoArgsConstructor
		@AllArgsConstructor
		@ToString
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
			private String empGender;
			
			/**
			* 雇员邮箱
			*/
			private String empEmail;
			
			/**
			* 雇员部门
			*/
			private Department department;
		}
		```
		在EmployeeMapper.java中声明方法
		
		```java
		public interface EmployeeMapper{
		
			Employee findEmpWithDeptByEmpId(Integer empId);
			
		}
		```
		
		在SQL映射文件中创建映射规则
		
		```xml
		<resultMap id="EmpWithDept" type="com.crzmy.entity.Employee">
			<id column="emp_id" property="empId"/>
			<result column="emp_name" property="empName"/>
			<result column="emp_gender" property="empGender"/>
			<result column="emp_email" property="empEmail"/>
			<result column="dept_id" property="department.deptId"/>
			<result column="dept_name" property="department.deptName"/>
		</resultMap>
		```
		
		创建select节点
		
		```xml
		<select id="findEmpWithDeptByEmpId" resultMap="EmpWithDept">
			SELECT
				*
			FROM
				tb_employee e, tb_department d
			WHERE
				e.dept_id = d.dept_id
			AND
				e.emp_id = #{empId}
		</select>
		```
		
		编写测试方法
		
		```java
		@Test
		public void test() throws IOException{
			SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
			try(SqlSession sqlSession = sqlSessionFactory.openSession()){
				EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
				Employee emp = mapper.findEmpWithDeptByEmpId(1);
				System.out.println(emp);
			}
		}
		```
		控制台打印
		
		```text
		Employee(empId=1, empPwd=123456, empName=ray, empGender=1, empEmail=crrruiii@163.com, department=Department(deptId=1, deptName=开发部))
		```
		
		* 联合查询使用association

		上面的方法在映射部门属性时,直接使用的级联属性,还有一种方法就是使用association.将上面的resultMap做如下修改.
		
		```xml
		<resultMap id="EmpWithDept" type="com.crzmy.entity.Employee">
			<id column="emp_id" property="empId"/>
			<result column="emp_name" property="empName"/>
			<result column="emp_gender" property="empGender"/>
			<result column="emp_email" property="empEmail"/>
			<!-- property为Employee中的department属性, javaType为Department类的全路径 -->
			<association property="department" javaType="com.crzmt.entity.Department">
				<id column="dept_id" property="deptId"/>
				<result column="dept_name" property="deptName"/>
			</association>
		</resultMap>
		```
		
		测试方法不作任何改变,同样能获取雇员的所在部门信息.
