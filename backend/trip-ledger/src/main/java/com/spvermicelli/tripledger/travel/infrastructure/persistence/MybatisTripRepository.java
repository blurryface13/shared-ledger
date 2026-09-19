package com.spvermicelli.tripledger.travel.infrastructure.persistence;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.travel.infrastructure.persistence.mapper.TripMapper;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DuplicateKeyException;
@Repository
public class MybatisTripRepository implements TripRepository {
    private final TripMapper mapper;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    public MybatisTripRepository(TripMapper mapper){this.mapper=mapper;}
    private String encode(Trip trip){try{return json.writeValueAsString(trip);}catch(Exception ex){throw new IllegalStateException("Trip serialization failed",ex);}}
    private Trip decode(TripMapper.Row row){try{return json.readValue(row.getPayload(),Trip.class).identified(row.getId(),row.getVersion());}catch(Exception ex){throw new IllegalStateException("Trip data invalid",ex);}}
    private TripMapper.Row row(long userId,Trip trip){var r=new TripMapper.Row();r.setId(trip.id());r.setOwnerId(userId);r.setVersion(trip.version());r.setBookId(trip.bookId());r.setPayload(encode(trip));return r;}
    public List<Trip> list(long userId){return mapper.list(userId).stream().map(this::decode).toList();}
    public Trip find(long userId,long id){var r=mapper.find(userId,id);if(r==null)throw new BusinessException(ErrorCode.NOT_FOUND,"行程不存在或无权访问");return decode(r);}
    public Trip create(long userId,Trip trip){var r=row(userId,trip);try{mapper.insert(r);}catch(DuplicateKeyException ex){throw conflict("该账本已绑定其他行程，请先解绑");}return trip.identified(r.getId(),0);}
    public Trip update(long userId,Trip trip){try{if(mapper.update(row(userId,trip))!=1)throw conflict("行程已被修改，请刷新后重试");}catch(DuplicateKeyException ex){throw conflict("该账本已绑定其他行程，请先解绑");}return trip.identified(trip.id(),trip.version()+1);}
    public long storePlan(long userId,long tripId,long version,String payload){var r=new TripMapper.Row();r.setOwnerId(userId);r.setBookId(tripId);r.setVersion(version);r.setPayload(payload);mapper.insertPlan(r);return r.getId();}
    public String getPlan(long userId,long tripId,long planId,long version){String s=mapper.plan(userId,tripId,planId,version);if(s==null)throw conflict("草案已过期、已采纳或行程已变化，请重新生成");return s;}
    public void applied(long planId){if(mapper.applied(planId)!=1)throw conflict("草案已采纳");}
    private BusinessException conflict(String message){return new BusinessException(ErrorCode.CONFLICT,message);}
}
