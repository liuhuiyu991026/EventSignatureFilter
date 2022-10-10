package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nutz.dao.entity.annotation.EL;
import org.nutz.dao.entity.annotation.Id;
import org.nutz.dao.entity.annotation.Prev;
import org.nutz.dao.entity.annotation.Table;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("event_handler_class")
public class EventHandlerClass {
    private static AtomicInteger IdCounter = new AtomicInteger();
    public static int nextId(){
        return IdCounter.incrementAndGet();
    }
    public static class Type {
        // Classes in android.jar
        public static final Integer SDK = 0;
        // Classes in App source code
        public static final Integer APP = 1;
        // Classes in the third part library
        public static final Integer LIB = 2;
    }
    // id of the class in database
    @Id(auto = false)
    Integer id;
    // Name of the class
    String name;
    // Package name
    String pkg;
    // name of super class
    String superName;
    // This Handler Class belong to?
    Integer type;
    // android sdk api level
    String version;
    // Library this class belong to
    String libName;
}
