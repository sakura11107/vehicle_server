package com.vehicle.server.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vehicle.server.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("""
            SELECT m.* FROM message m
            INNER JOIN (
                SELECT
                    CASE WHEN sender_id = #{currentUserId} THEN receiver_id ELSE sender_id END as contact_id,
                    MAX(id) as max_id
                FROM message
                WHERE deleted = 0 AND (sender_id = #{currentUserId} OR receiver_id = #{currentUserId})
                GROUP BY contact_id
            ) latest ON m.id = latest.max_id
            ORDER BY m.created_time DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Message> selectLatestMessagesPerContact(
            @Param("currentUserId") Long currentUserId,
            @Param("size") int size,
            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(DISTINCT
                CASE WHEN sender_id = #{currentUserId} THEN receiver_id ELSE sender_id END
            )
            FROM message
            WHERE deleted = 0 AND (sender_id = #{currentUserId} OR receiver_id = #{currentUserId})
            """)
    long countConversations(@Param("currentUserId") Long currentUserId);
}
