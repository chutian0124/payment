package com.sunreal.payment.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sunreal.payment.dao.model.TransOrder;
import com.sunreal.payment.dao.model.TransOrderExample;
@Mapper
public interface TransOrderMapper {
    int countByExample(TransOrderExample example);

    int deleteByExample(TransOrderExample example);

    int deleteByPrimaryKey(String transOrderId);

    int insert(TransOrder record);

    int insertSelective(TransOrder record);

    List<TransOrder> selectByExample(TransOrderExample example);

    TransOrder selectByPrimaryKey(String transOrderId);

    int updateByExampleSelective(@Param("record") TransOrder record, @Param("example") TransOrderExample example);

    int updateByExample(@Param("record") TransOrder record, @Param("example") TransOrderExample example);

    int updateByPrimaryKeySelective(TransOrder record);

    int updateByPrimaryKey(TransOrder record);
}