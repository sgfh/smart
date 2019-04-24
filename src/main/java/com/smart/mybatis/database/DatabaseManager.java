package com.smart.mybatis.database;

import com.smart.mybatis.annotation.Column;
import com.smart.mybatis.annotation.GeneratedValue;
import com.smart.mybatis.annotation.Id;
import com.smart.mybatis.annotation.Table;
import com.smart.mybatis.util.DataSourceUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 自动创建数据库表
 */
public class DatabaseManager {
    private static final String CLASS_SUFFIX = ".class";
    private static final String CLASS_FILE_PREFIX = File.separator + "classes" + File.separator;
    private static final String PACKAGE_SEPARATOR = ".";


    public void init(String url, String dbName, String packageName, String username, String password) throws SQLException {
        //获取包下所有class类，获取到后，扫描注解，完成数据库表生成
        List<String> classList = getClazzName(packageName, false);
        Connection con = DataSourceUtil.getConnection(url, username, password);
        Statement sm = null;


        for (String className : classList) {
            String valueName = "";
            try {
                Object object = Class.forName(className).newInstance();
                if (object.getClass().getAnnotation(Table.class) == null)
                    continue;
                String tableName = object.getClass().getAnnotation(Table.class).value();

                if (isTableExist(url, username, password, "SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA=(select database()) and `table_name` ='" + tableName + "'")) {
                    String excuteAddSql = "";
                    String excuteModifySql="";
                    //表已经存在
                    Map<String, Object> map = findTableFields(url, username, password, "SELECT column_name FROM information_schema.columns where `table_name` ='" + tableName + "'");
                    //遍历属性，如果在结果集中不存在，则需要添加字段
                    Field[] superFields = object.getClass().getSuperclass().getDeclaredFields();
                    addFiledSql(superFields, map, excuteAddSql);

                    Field[] fields = object.getClass().getDeclaredFields();
                    addFiledSql(fields, map, excuteAddSql);

                    if (excuteAddSql.length() != 0) {
                        excuteAddSql = excuteAddSql.substring(0, excuteAddSql.length() - 1);
                        excuteAddSql = "ALTER TABLE " + tableName + " ADD " + excuteAddSql;
                        executeSql(con, sm, excuteAddSql, url, username, password, true);
                    }
                    continue;
                }

                //父类，此时扫描出父类的注解
                Field[] superFields = object.getClass().getSuperclass().getDeclaredFields();
                for (Field superField : superFields) {
                    if (superField.getAnnotation(GeneratedValue.class) != null)
                        valueName += (superField.getAnnotation(Id.class).value() + " " + superField.getAnnotation(Id.class).columnDefinition() + " " + "AUTO_INCREMENT PRIMARY KEY,");

                    if (superField.getAnnotation(Column.class) != null)
                        valueName += (superField.getAnnotation(Column.class).value() + " " + superField.getAnnotation(Column.class).columnDefinition() + ",");

                }
                Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getAnnotation(Column.class) != null)
                        valueName += (field.getAnnotation(Column.class).value() + " " + field.getAnnotation(Column.class).columnDefinition() + ",");

                }
                valueName = valueName.substring(0, valueName.length() - 1);
                String executeSql = "create table " + tableName + "(" + valueName + ")";
                //表不存在，直接生成
                executeSql(con, sm, executeSql, url, username, password, true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 组装属性sql
     */
    private void addFiledSql(Field[] fields, Map<String, Object> map, String excuteAddSql) {
        for (Field field : fields) {
            if (field.getAnnotation(Column.class) != null) {
                String keyColumn = field.getAnnotation(Column.class).value();
                if (map.get(keyColumn) == null)
                    excuteAddSql += (keyColumn + " " + field.getAnnotation(Column.class).columnDefinition() + ",");
            }

        }
    }

    /**
     * 查询表中否存在
     */
    private boolean isTableExist(String url, String username, String password, String executeSql) {
        boolean flag = false;
        try {
            Connection con = DataSourceUtil.getConnection(url, username, password);
            Statement sm = con.createStatement();   //创建对象
            sm.execute(executeSql);
            System.out.println(executeSql);
            ResultSet resultSet = sm.getResultSet();
            if (resultSet.next())
                flag = true;
            sm.close();
            DataSourceUtil.closeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 查询表属性
     */
    private Map<String, Object> findTableFields(String url, String username, String password, String executeSql) {
        Map<String, Object> map = null;
        try {
            Connection con = DataSourceUtil.getConnection(url, username, password);
            Statement sm = con.createStatement();   //创建对象
            sm.execute(executeSql);
            System.out.println(executeSql);
            ResultSet resultSet = sm.getResultSet();
            map = convertList(resultSet);
            sm.close();
            DataSourceUtil.closeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 将数据集合存放到list集合中
     */
    private static Map<String, Object> convertList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();//获取键名
        int columnCount = md.getColumnCount();//获取行的数量
        Map<String, Object> rowData = new HashMap();//声明Map
        while (rs.next()) {

            for (int i = 1; i <= columnCount; i++) {
                rowData.put(rs.getObject(i) + "", rs.getObject(i));//获取键名及值
            }
        }
        return rowData;
    }


    /**
     * 执行sql语句
     */
    private ResultSet executeSql(Connection con, Statement sm, String executeSql, String url, String username, String password, boolean isNeedClose) {
        try {
            con = getConnection(url, username, password);
            sm = con.createStatement();   //创建对象
            sm.execute(executeSql);
            System.out.println(executeSql);
            ResultSet resultSet = sm.getResultSet();
            if (isNeedClose) {
                sm.close();
                con.close();
            }
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;

    }


    /**
     * 获取数据库链接
     */
    private static Connection getConnection(String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }


    /**
     * 26
     * 查找包下的所有类的名字
     * 27
     *
     * @param packageName          28
     * @param showChildPackageFlag 是否需要显示子包内容
     *                             29
     * @return List集合，内容为类的全名
     * 30
     */
    private List<String> getClazzName(String packageName, boolean showChildPackageFlag) {
        List<String> result = new ArrayList<>();
        String suffixPath = packageName.replaceAll("\\.", "/");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> urls = loader.getResources(suffixPath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        String path = url.getPath();
                        System.out.println(path);
                        result.addAll(getAllClassNameByFile(new File(path), showChildPackageFlag));
                    } else if ("jar".equals(protocol)) {
                        JarFile jarFile = null;
                        try {
                            jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (jarFile != null) {
                            result.addAll(getAllClassNameByJar(jarFile, packageName, showChildPackageFlag));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 66
     * 递归获取所有class文件的名字
     * 67
     *
     * @param file 68
     * @param flag 是否需要迭代遍历
     *             69
     * @return List
     * 70
     */
    private static List<String> getAllClassNameByFile(File file, boolean flag) {
        List<String> result = new ArrayList<>();
        if (!file.exists()) {
            return result;
        }
        if (file.isFile()) {
            String path = file.getPath();
            // 注意：这里替换文件分割符要用replace。因为replaceAll里面的参数是正则表达式,而windows环境中File.separator="\\"的,因此会有问题
            if (path.endsWith(CLASS_SUFFIX)) {
                path = path.replace(CLASS_SUFFIX, "");
                // 从"/classes/"后面开始截取
                String clazzName = path.substring(path.indexOf(CLASS_FILE_PREFIX) + CLASS_FILE_PREFIX.length())
                        .replace(File.separator, PACKAGE_SEPARATOR);
                if (-1 == clazzName.indexOf("$")) {
                    result.add(clazzName);
                }
            }
            return result;

        } else {
            File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File f : listFiles) {
                    if (flag) {
                        result.addAll(getAllClassNameByFile(f, flag));
                    } else {
                        if (f.isFile()) {
                            String path = f.getPath();
                            if (path.endsWith(CLASS_SUFFIX)) {
                                path = path.replace(CLASS_SUFFIX, "");
                                // 从"/classes/"后面开始截取
                                String clazzName = path.substring(path.indexOf(CLASS_FILE_PREFIX) + CLASS_FILE_PREFIX.length())
                                        .replace(File.separator, PACKAGE_SEPARATOR);
                                if (-1 == clazzName.indexOf("$")) {
                                    result.add(clazzName);
                                }
                            }
                        }

                    }
                }
            }
            System.out.println(result.toString());
            return result;
        }
    }


    /**
     * 118
     * 递归获取jar所有class文件的名字
     * 119
     *
     * @param jarFile     120
     * @param packageName 包名
     *                    121
     * @param flag        是否需要迭代遍历
     *                    122
     * @return List
     * 123
     */
    private static List<String> getAllClassNameByJar(JarFile jarFile, String packageName, boolean flag) {
        List<String> result = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            // 判断是不是class文件
            if (name.endsWith(CLASS_SUFFIX)) {
                name = name.replace(CLASS_SUFFIX, "").replace("/", ".");
                if (flag) {
                    // 如果要子包的文件,那么就只要开头相同且不是内部类就ok
                    if (name.startsWith(packageName) && -1 == name.indexOf("$")) {
                        result.add(name);
                    }
                } else {
                    // 如果不要子包的文件,那么就必须保证最后一个"."之前的字符串和包名一样且不是内部类
                    if (packageName.equals(name.substring(0, name.lastIndexOf("."))) && -1 == name.indexOf("$")) {
                        result.add(name);
                    }
                }
            }
        }
        return result;
    }
}
