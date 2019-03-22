package com.dfire.platform.alchemy.api.logstash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mutate {

    /***
     * 添加新字段
     * mutate {add_field => ["new_filed", "%{syslog_host}"]}
     */
    public static void addField(Map<String, Object> map, String newField, Object value) {
        if (map == null) {
            return;
        }
        map.put(newField, value);
    }

    /***
     * 删除字段，可传入多个
     * mutate {add_field => ["new_filed", "%{syslog_host}"]}
     */
    public static void removeFields(Map<String, Object> map, String... fields) {
        for (String field : fields) {
            map.remove(field);
        }
    }

    /***
     * 更新字段内容，如果字段不存在，不会新建
     */
    public static void update(Map<String, Object> map, String field, Object value) {
        if (map.get(field) == null) {
            return;
        }
        map.put(field, value);
    }


    /***
     * 与update功能相同，区别在于如果字段不存在则会新建字段
     * mutate {replace => { "message" => "%{source_host}: My new message" }}
     */
    public static void replace(Map<String, Object> map, String field, Object value) {
        map.put(field, value);
    }

    /***
     * 添加tag
     * mutate {add_tag => ["tag1", "tag2"]}
     */
    public static void addTags(Map<String, Object> map, String... tags) {
        if (map.get("tags") == null) {
            map.put("tags", new ArrayList<String>());
        }
        List<String> tagList = (List<String>) map.get("tags");
        for (String tag : tags) {
            tagList.add(tag);
        }
    }

    /***
     * 删除tag
     */
    public static void removeTags(Map<String, Object> map, String... tags) {
        if (map.get("tags") == null) {
            map.put("tags", new ArrayList<String>());
        }
        List<String> tagList = (List<String>) map.get("tags");
        for (String tag : tags) {
            tagList.remove(tag);
        }
    }


    /***
     * 字符串替换。用正则表达式和字符串都行。它只能用于字符串，如果不是字符串，那么什么都不会做，也不会报错
     * mutate {gsub => ["field_name", "[\\?#-]", "."]}
     */
    public static void gsub(Map<String, Object> map, String field, String oldValue, String newValue) {
        Object obj = map.get(field);
        if (obj instanceof String) {
            String value = (String) obj;
            value = value.replace(oldValue, newValue);
            map.put(field, value);
        }
    }

    /***
     * 转换某个字段的value为小写
     */
    public static void lowercase(Map<String, Object> map, String... fields) {
        for (String field : fields) {
            Object obj = map.get(field);
            if (obj != null) {
                String value = (String) obj;
                value = value.toLowerCase();
                map.put(field, value);
            }
        }
    }

    /***
     * 转换某个字段的value为大写
     */
    public static void uppercase(Map<String, Object> map, String... fields) {
        for (String field : fields) {
            Object obj = map.get(field);
            if (obj != null) {
                String value = (String) obj;
                value = value.toUpperCase();
                map.put(field, value);
            }
        }
    }

    /***
     * 重命名字段名称
     */
    public static void rename(Map<String, Object> map, String oldField, String newFiled) {
        map.put(newFiled, map.get(oldField));
        map.remove(oldField);
    }


    /***
     * 将提取到的某个字段按照某个字符分割,(重新赋值到原先的字段中)
     * mutate {split => ["message", "|"]}
     */
    public static void split(Map<String, Object> map, String field, String seperator) {
        Object obj = map.get(field);
        if (obj != null && obj instanceof String) {
            String value = (String) obj;
            map.put(field, value.split(seperator));
        }
    }



    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        Mutate.addField(map, "field1", "aaa111");
        Mutate.gsub(map, "field1", "1", "2");
        System.out.println(map);
        System.out.println("aaa?b?bb#ccc-ddd".replace("?", "."));
        System.out.println("aaa?bbb#ccc-ddd".replaceAll("[\\?#-]", "."));

        //Pattern pattern = Pattern.compile("[0-9]*");

    }

}