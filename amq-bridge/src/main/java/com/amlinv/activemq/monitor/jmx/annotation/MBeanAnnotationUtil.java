package com.amlinv.activemq.monitor.jmx.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by art on 3/31/15.
 */
public class MBeanAnnotationUtil {
    public static String getLocationONamePattern (Object mbeanLocation) {
        MBeanLocation location = mbeanLocation.getClass().getAnnotation(MBeanLocation.class);

        if ( location == null ) {
            return  null;
        }

        return location.onamePattern();
    }

    public static Map<String, Method> getAttributes (Object mbeanLocation) {
        Map<String, Method> result = new TreeMap<String, Method>();

        Method[] methods = mbeanLocation.getClass().getMethods();

        for ( Method oneMethod : methods ) {
            MBeanAttribute attribute = oneMethod.getAnnotation(MBeanAttribute.class);

            if ( attribute != null ) {
                //
                // Record the name of the attribute with the method, which must be the setter.
                //
                result.put(attribute.name(), oneMethod);
            }
        }

        return  result;
    }
}
