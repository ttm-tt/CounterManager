/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.prefs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chtheis
 */
public class Preferences {
    
    public static void loadProperties(Object obj, String section, boolean inherit) {
        Properties properties = new Properties();
        try {
            properties.load(section);
            
            loadProperties(obj, obj.getClass(), properties, inherit);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(obj.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    private static void loadProperties(Object obj, Class clazz, Properties properties, boolean inherit) {
        
        if (clazz.equals(Object.class))
            return;
        
        if (inherit && clazz.getSuperclass() != null)
            loadProperties(obj, clazz.getSuperclass(), properties, inherit);
        
        // Load the properties via reflection
        java.lang.reflect.Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            char[] c = field.getName().toCharArray();
            c[0] = Character.toUpperCase(c[0]);
            String name = new String(c);
            String value = properties.getProperty(name);
            if (value == null) {
                // Try to initialize with original value
                try {
                    try {
                        value = (String) obj.getClass().getMethod("get" + name).invoke(obj);
                    } catch (NoSuchMethodException e) {
                        value = (String) obj.getClass().getMethod("is" + name).invoke(obj);                            
                    }

                } catch (Throwable t) {

                }
            }
            java.lang.reflect.Method method = null;
            try {
                // Cast String value to given type
                Class fieldClazz = field.getType();
                // Map primitive types to Object types
                if (fieldClazz.equals(Integer.TYPE))
                    fieldClazz = Integer.class;
                else if (fieldClazz.equals(Long.TYPE))
                    fieldClazz = Long.class;
                else if (fieldClazz.equals(Boolean.TYPE))
                    fieldClazz = Boolean.class;
                Object tmp = 
                        value == null ?
                        fieldClazz.getConstructor().newInstance() :
                        fieldClazz.getConstructor(String.class).newInstance(value);
                // And call setter
                method = obj.getClass().getMethod("set" + name, field.getType());
                method.invoke(obj, tmp);
            }catch (NoSuchMethodException e) {
                // Ignore
            }catch (IllegalArgumentException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            }catch (IllegalAccessException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            }catch (InvocationTargetException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            }catch (InstantiationException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            }
        }
    }

    public static void saveProperties(Object obj, String section, boolean inherit) {
        Properties properties = new Properties();
        
        Class clazz = obj.getClass();
        
        saveProperties(obj, clazz, properties, inherit);
        
        try {
            properties.store(section);
        } catch (IOException ex) {
            Logger.getLogger(obj.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void saveProperties(Object obj, Class clazz, Properties properties, boolean inherit) {
        if (clazz.equals(Object.class))
            return;
        
        if (inherit && clazz.getSuperclass() != null)
            saveProperties(obj, clazz.getSuperclass(), properties, inherit);
        
        // Save the properties via reflection
        java.lang.reflect.Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            char[] c = field.getName().toCharArray();
            c[0] = Character.toUpperCase(c[0]);
            String name = new String(c);
            java.lang.reflect.Method method = null;
            try {
                // getter method
                try {
                    method = obj.getClass().getMethod("get" + name);
                } catch (NoSuchMethodException e) {

                }

                if (method == null)
                    method = obj.getClass().getMethod("is" + name);

                // Call getter, convert to String and store in properties
                properties.setProperty(name, method.invoke(obj).toString());
            } catch (NoSuchMethodException e) {
                // Ignore
            } catch (IllegalArgumentException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            } catch (IllegalAccessException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            } catch (InvocationTargetException e) {
                Logger.getLogger(obj.getClass().getName()).log(Level.FINE, null, e);
            }
        }
    }
}
