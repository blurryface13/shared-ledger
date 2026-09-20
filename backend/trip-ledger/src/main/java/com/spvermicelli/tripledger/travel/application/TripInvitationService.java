package com.spvermicelli.tripledger.travel.application;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;

@Service
public class TripInvitationService {
    private final JdbcTemplate jdbc;
    private final TripCollaborationService collaboration;
    public TripInvitationService(JdbcTemplate jdbc, TripCollaborationService collaboration) {
        this.jdbc=jdbc; this.collaboration=collaboration;
    }
    public record Invitation(long id, String token, int expiresInHours) {}
    @Transactional
    public Invitation create(long owner,long trip,long target,String role) {
        collaboration.requireOwner(owner,trip);
        if(target==owner || role==null || !List.of("EDITOR","VIEWER").contains(role)) throw invalid();
        if(jdbc.queryForObject("SELECT COUNT(*) FROM tb_user WHERE id=? AND status='ACTIVE'",Integer.class,target)!=1) throw invalid();
        // Reissuing invalidates previous pending tokens for the same recipient.
        jdbc.update("UPDATE tb_trip_invite SET status='REVOKED' WHERE trip_id=? AND target_user_id=? AND status='PENDING'",trip,target);
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash=hash(token);
        jdbc.update("INSERT INTO tb_trip_invite(trip_id,target_user_id,role,token_hash,expires_at) VALUES(?,?,?,?,DATE_ADD(NOW(),INTERVAL 24 HOUR))",trip,target,role,hash);
        long id=jdbc.queryForObject("SELECT id FROM tb_trip_invite WHERE token_hash=?",Long.class,hash);
        audit(owner,trip,"INVITED_"+target);
        return new Invitation(id,token,24);
    }
    @Transactional
    public void revoke(long owner,long trip,long invite) {
        collaboration.requireOwner(owner,trip);
        if(jdbc.update("UPDATE tb_trip_invite SET status='REVOKED' WHERE id=? AND trip_id=? AND status='PENDING'",invite,trip)>0) audit(owner,trip,"INVITE_REVOKED_"+invite);
    }
    @Transactional
    public long accept(long user,String token) {
        String hash=hash(token);
        var ids=jdbc.queryForList("SELECT trip_id FROM tb_trip_invite WHERE token_hash=?",Long.class,hash);
        if(ids.isEmpty()) throw invalid();
        long trip=ids.getFirst();
        // Parent first: same lock order as editing and member revocation.
        var owners=jdbc.queryForList("SELECT owner_id FROM tb_trip WHERE id=? FOR UPDATE",Long.class,trip);
        if(owners.isEmpty()) throw invalid();
        var row=jdbc.queryForMap("SELECT *,expires_at>NOW() AS valid FROM tb_trip_invite WHERE token_hash=? FOR UPDATE",hash);
        if(((Number)row.get("target_user_id")).longValue()!=user) throw invalid();
        if(jdbc.queryForObject("SELECT COUNT(*) FROM tb_user WHERE id=? AND status='ACTIVE'",Integer.class,user)!=1) throw invalid();
        String status=(String)row.get("status");
        if("ACCEPTED".equals(status)) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_member WHERE trip_id=? AND user_id=?",Integer.class,trip,user)==0) throw invalid();
            return trip;
        }
        if(!"PENDING".equals(status) || !Boolean.TRUE.equals(row.get("valid")) && !Integer.valueOf(1).equals(row.get("valid")) && !Long.valueOf(1).equals(row.get("valid"))) throw invalid();
        // Existing membership is preserved; an old invitation must not change a current role.
        jdbc.update("INSERT INTO tb_trip_member(trip_id,user_id,role) VALUES(?,?,?) ON DUPLICATE KEY UPDATE user_id=user_id",trip,user,row.get("role"));
        jdbc.update("UPDATE tb_trip_invite SET status='ACCEPTED' WHERE token_hash=?",hash);
        audit(user,trip,"INVITE_ACCEPTED");
        return trip;
    }
    private void audit(long actor,long trip,String action) {
        collaboration.record(actor,trip,jdbc.queryForObject("SELECT version FROM tb_trip WHERE id=?",Long.class,trip),action);
    }
    private String hash(String token) {
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_PARAM,"邀请无效、已过期或不属于当前用户");}
}
