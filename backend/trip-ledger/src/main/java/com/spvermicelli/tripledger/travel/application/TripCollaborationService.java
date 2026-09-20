package com.spvermicelli.tripledger.travel.application;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.travel.domain.TripRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.List;
import java.util.Map;

@Service
public class TripCollaborationService {
    private final JdbcTemplate jdbc;
    private final TripRepository trips;
    public TripCollaborationService(JdbcTemplate jdbc, TripRepository trips) { this.jdbc=jdbc; this.trips=trips; }
    // Writers and membership changes lock the same parent row, so revocation cannot race a write.
    @Transactional(propagation=Propagation.MANDATORY)
    public long requireOwner(long user, long id) {
        long owner=ownerLocked(id);
        if(owner!=user) throw denied();
        return owner;
    }
    private long ownerLocked(long id) {
        var owners=jdbc.queryForList("SELECT owner_id FROM tb_trip WHERE id=? FOR UPDATE",Long.class,id);
        if(owners.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND,"行程不存在或无权访问");
        return owners.getFirst();
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void requireEditor(long user,long id) {
        long owner=ownerLocked(id);
        if(owner!=user && jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_member WHERE trip_id=? AND user_id=? AND role='EDITOR'",Integer.class,id,user)==0) throw denied();
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(long user,long id,long version,String action) {
        jdbc.update("INSERT INTO tb_trip_change(trip_id,actor_id,version,action) VALUES(?,?,?,?)",id,user,version,action);
    }
    @Transactional
    public void setMember(long owner,long id,long target,String role) {
        requireOwner(owner,id);
        if(target==owner || role==null || !List.of("EDITOR","VIEWER").contains(role)) throw new BusinessException(ErrorCode.INVALID_PARAM,"成员角色无效");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM tb_user WHERE id=? AND status='ACTIVE'",Integer.class,target)!=1) throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");
        jdbc.update("INSERT INTO tb_trip_member(trip_id,user_id,role) VALUES(?,?,?) ON DUPLICATE KEY UPDATE role=VALUES(role)",id,target,role);
        membershipRecord(owner,id,"MEMBER_"+target+"_"+role);
    }
    @Transactional
    public void removeMember(long owner,long id,long target) {
        requireOwner(owner,id);
        jdbc.update("UPDATE tb_trip_invite SET status='REVOKED' WHERE trip_id=? AND target_user_id=? AND status='PENDING'",id,target);
        if(jdbc.update("DELETE FROM tb_trip_member WHERE trip_id=? AND user_id=?",id,target)>0) membershipRecord(owner,id,"REMOVED_"+target);
    }
    private void membershipRecord(long actor,long id,String action) {
        record(actor,id,jdbc.queryForObject("SELECT version FROM tb_trip WHERE id=?",Long.class,id),action);
    }
    public List<Map<String,Object>> members(long user,long id) {
        trips.find(user,id);
        return jdbc.queryForList("SELECT owner_id AS user_id,'OWNER' AS role FROM tb_trip WHERE id=? UNION ALL SELECT user_id,role FROM tb_trip_member WHERE trip_id=?",id,id);
    }
    public List<Map<String,Object>> changes(long user,long id,long after) {
        trips.find(user,id);
        if(after<0) throw new BusinessException(ErrorCode.INVALID_PARAM,"游标无效");
        return jdbc.queryForList("SELECT id,actor_id,version,action,created_at FROM tb_trip_change WHERE trip_id=? AND id>? ORDER BY id LIMIT 100",id,after);
    }
    private BusinessException denied(){return new BusinessException(ErrorCode.FORBIDDEN,"无权修改该行程");}
}
