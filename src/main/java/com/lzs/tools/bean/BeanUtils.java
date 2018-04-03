package com.weidai.backgoods.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.reflectasm.MethodAccess;

/**
* 使用esotericsoftware.reflectasm进行高效反射设置对象（springframework的太慢）
*/
public class BeanUtils {
    private final static Logger logger  = LoggerFactory.getLogger(BeanUtils.class);
	
    private static ThreadLocal<Map<Class<?>,MethodAccess>> localMap = new ThreadLocal<>();
    private static ThreadLocal<Map<String,String[][]>> getterSetterMap = new ThreadLocal<>();
    
    public static void copyProperties(Object src, Object dest){
    	copyProperties(src, dest, null);
    }
    public static void copyProperties(Object src, Object dest, Boolean ignoreProptiesSettingException){
    	if(src == null || dest == null){
    		return ;
    	}
    	if(ignoreProptiesSettingException == null){
    		ignoreProptiesSettingException = Boolean.FALSE;
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
    		for (int i=0;i<pairs.length;i++) {
            	if(pairs[i][0] == null){
            		break;
            	}
            	access2.invoke(dest, pairs[i][1],access1.invoke(src, pairs[i][0]));
            }
    	} else {
    		for (int i=0;i<pairs.length;i++) {
            	if(pairs[i][0] == null){
            		break;
            	}
            	try{
            		access2.invoke(dest, pairs[i][1],access1.invoke(src, pairs[i][0]));
            	} catch (Exception e){
            		logger.warn("setting property exception. setter method name=" + pairs[i][1] + "getter method name=" + pairs[i][0]);
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
}
