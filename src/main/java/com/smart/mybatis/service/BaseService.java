package com.smart.mybatis.service;



import com.github.pagehelper.PageInfo;
import com.smart.mybatis.page.Pageable;

import java.util.List;

public interface BaseService<T> {

    int insert(T entity);

    int update(T entity);

    int delete(T entity);

    Object findById(Long id, Class<T> cls);

    List<T> list(T entity, Class<T> cls);

    Object find(T entity);

    Object findLinkT(T entity);

    List<T> findLinkListT(T entity, Class<T> cls);

    PageInfo<T> page(Pageable pageable, T entity, Class<T> cls);
}
