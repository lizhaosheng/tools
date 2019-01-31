package com.lzs.tools.bean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * 使用reflectasm 比传统的common包提供的BeanUtils 效率高非常多
 */
@Slf4j
public class BeanUtils {
	
	private static ThreadLocal<Map<Class<?>,MethodAccess>> localMap = new ThreadLocal<>();
    private static ThreadLocal<Map<String,String[][]>> getterSetterMap = new ThreadLocal<>();
    
    public static void copyProperties(Object src, Object dest){
    	copyProperties(src, dest, true, true);
    }
    public static void copyProperties(Object src, Object dest, boolean ignoreProptiesSettingException, boolean ignoreNull){
    	if(src == null || dest == null){
    		return ;
    	}
    	Map<Class<?>, MethodAccess> map = localMap.get();
    	if(map == null){
    		map = new HashMap<>();
    		localMap.set(map );
    	}
    	
        MethodAccess access1 = map.get(src.getClass());
    	if(access1 == null){
    		access1 = MethodAccess.get(src.getClass());  
    		map.put(src.getClass(), access1);
    	}
    	MethodAccess access2 = map.get(dest.getClass());
    	if(access2 == null){
    		access2 = MethodAccess.get(dest.getClass());  
    		map.put(dest.getClass(), access2);
    	}
        
    	String[][] pairs = getMethodPairs(src.getClass(), dest.getClass());
    	
    	if(!ignoreProptiesSettingException){
    		if(ignoreNull){
				for (int i=0;i<pairs.length;i++) {
					if(pairs[i][0] == null){
						break;
					}
					Object val = access1.invoke(src, pairs[i][0]);
					if(val == null) {
						continue;
					}
					access2.invoke(dest, pairs[i][1],val);
				}
			} else {
				for (int i = 0; i < pairs.length; i++) {
					if (pairs[i][0] == null) {
						break;
					}
					access2.invoke(dest, pairs[i][1], access1.invoke(src, pairs[i][0]));
				}
			}

    	} else {
    		if(ignoreNull) {
				for (int i = 0; i < pairs.length; i++) {
					if (pairs[i][0] == null) {
						break;
					}
					try {
						Object val = access1.invoke(src, pairs[i][0]);
						if(val == null) {
							continue;
						}
						access2.invoke(dest, pairs[i][1],val);
					} catch (Exception e) {
						log.warn("setting property exception. setter method name=" + pairs[i][1] + "getter method name=" + pairs[i][0]);
					}
				}
			} else {
				for (int i = 0; i < pairs.length; i++) {
					if (pairs[i][0] == null) {
						break;
					}
					try {
						access2.invoke(dest, pairs[i][1],access1.invoke(src, pairs[i][0]));
					} catch (Exception e) {
						log.warn("setting property exception. setter method name=" + pairs[i][1] + "getter method name=" + pairs[i][0]);
					}
				}
			}
    	}
        
    }

	private static String[][] getMethodPairs(Class<?> class1, Class<?> class2) {
		Map<String,String[][]> map = getterSetterMap.get();
    	if(map == null){
    		map = new HashMap<>();
    		getterSetterMap.set(map );
    	}
    	String key = class1.getName() + "#" + class2.getName();
    	if(!map.containsKey(key)){
    		Method[] declaredMethods1 = class1.getMethods();
            Method[] declaredMethods2 = class2.getMethods();
            Set<String> m2map = new HashSet<>();
            for(Method m : declaredMethods2){
            	m2map.add(m.getName());
            }
    		
    		List<String> tempList = new ArrayList<>();
    		for (Method m : declaredMethods1) {
            	if(m.getName().startsWith("get")){
            		tempList.add(m.getName());
            	}
            }
    		
    		
    		String[][] array = new String[tempList.size()][2];
    		int i = 0;
    		String temp = null;
    		for (String m1 : tempList) {
    			temp = "set" + m1.substring(3);
        		if(m2map.contains(temp)){
        			array[i][0] = m1;
            		array[i][1] = temp;
            		i++;
        		}
            }
    		
    		map.put(key, array);
    	}
    	
		return map.get(key);
	}

	public static <T> List<T> copyList(List src, Class<T> cl) {
		List<T> dest = new ArrayList<>();
		copyList(src,dest,cl,true,true);
		return dest;
	}
	public static <T> void copyList(List src, List dest, Class<T> cl) {
		copyList(src,dest,cl,true,true);
	}
	public static <T> void copyList(List src, List dest, Class<T> cl, boolean ignoreProptiesSettingException, boolean ignoreNull) {
		try {
			for (Object obj : src) {
				Object newobj = cl.newInstance();
				copyProperties(obj, newobj,ignoreProptiesSettingException,ignoreNull);
				dest.add(newobj);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("cl.newInstance() exception, please check if there is a default 'constructor' method in class '" + cl.getName() + "'");
			e.printStackTrace();
		}
	}
}

