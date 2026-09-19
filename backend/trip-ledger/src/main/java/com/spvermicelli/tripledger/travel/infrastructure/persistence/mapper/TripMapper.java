package com.spvermicelli.tripledger.travel.infrastructure.persistence.mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
import lombok.Data;
public interface TripMapper {
    @Data class Row { private Long id; private Long ownerId; private Long version; private Long bookId; private String payload; }
    @Select("SELECT id,owner_id,version,book_id,payload FROM tb_trip WHERE owner_id=#{userId} ORDER BY updated_at DESC,id DESC LIMIT 200")
    List<Row> list(long userId);
    @Select("SELECT id,owner_id,version,book_id,payload FROM tb_trip WHERE id=#{id} AND owner_id=#{userId}")
    Row find(@Param("userId")long userId,@Param("id")long id);
    @Insert("INSERT INTO tb_trip(owner_id,version,book_id,payload) VALUES(#{ownerId},0,#{bookId},#{payload})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(Row row);
    @Update("UPDATE tb_trip SET payload=#{payload},book_id=#{bookId},version=version+1 WHERE id=#{id} AND owner_id=#{ownerId} AND version=#{version}")
    int update(Row row);
    @Insert("INSERT INTO tb_trip_plan(owner_id,trip_id,base_version,payload) VALUES(#{ownerId},#{bookId},#{version},#{payload})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertPlan(Row row);
    @Select("SELECT payload FROM tb_trip_plan WHERE id=#{id} AND owner_id=#{userId} AND trip_id=#{tripId} AND base_version=#{version} AND applied=0 AND created_at>DATE_SUB(NOW(),INTERVAL 1 DAY)")
    String plan(@Param("userId")long userId,@Param("tripId")long tripId,@Param("id")long id,@Param("version")long version);
    @Update("UPDATE tb_trip_plan SET applied=1 WHERE id=#{id} AND applied=0") int applied(long id);
}
