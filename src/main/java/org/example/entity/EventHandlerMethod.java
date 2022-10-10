package org.example.entity;

import lombok.*;
import org.nutz.dao.entity.annotation.Id;
import org.nutz.dao.entity.annotation.One;
import org.nutz.dao.entity.annotation.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("event_handler_method")
public class EventHandlerMethod {
    @Id
    Integer id;
    Integer classID;
    String name;
    String desc;
    String signature;
    @One(field = "classID", key = "id")
    @Setter(AccessLevel.NONE)
    EventHandlerClass master;
}