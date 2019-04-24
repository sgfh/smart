package com.smart.mybatis.mapper;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface BaseMapper {

    int insert(Map params);

    int update(Map params);

    int delete(Map params);

    HashMap findById(Map params);

    List<Map<String,Object>> list(Map params);

    HashMap find(Map params);

    HashMap findLinkT(Map params);

    List<Map<String,Object>> findLinkListT(Map params);
}
