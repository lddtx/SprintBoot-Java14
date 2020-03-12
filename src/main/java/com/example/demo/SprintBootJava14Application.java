package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;
import java.util.StringJoiner;

@SpringBootApplication
public class SprintBootJava14Application {

    public static void main(String[] args) {
        SpringApplication.run(SprintBootJava14Application.class, args);
    }
}

enum EmotionalState {
    SAD, HAPPY, NEUTRAL
}

class Person{
    private int id;
    private String name;
    private int emotionalState;

    public Person() {
    }

    public Person(String name, Integer emotionalState) {
        this.name = name;
        this.emotionalState = emotionalState;
    }

    public Person(Integer id, String name, Integer emotionalState) {
        this.id = id;
        this.name = name;
        this.emotionalState = emotionalState;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEmotionalState() {
        return emotionalState;
    }

    public void setEmotionalState(int emotionalState) {
        this.emotionalState = emotionalState;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Person.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("emotionalState=" + emotionalState)
                .toString();
    }
}

@Service
class PeopleService {

    private final JdbcTemplate template;

    PeopleService(JdbcTemplate template) {
        this.template = template;
    }

    private final String findByIdSql =
    """
            select * from PEOPLE 
            where id = ? 
    """;

    private final String insertSql =
    """
        insert into PEOPLE(name, emotional_state)
        values (?,?);
    """;

    private final RowMapper<Person> personRowMapper =
            (rs, rowNum) -> new Person(rs.getInt("id"), rs.getString("name"),
                    rs.getInt("emotional_state"));

    public Person create(String name, EmotionalState state) {
        var index = switch (state) {
            case SAD -> -1;
            case HAPPY -> 1;
            case NEUTRAL -> 0;
        };
        var declaredParameters = List.of(
                new SqlParameter(Types.VARCHAR, "name"),
                new SqlParameter(Types.INTEGER, "emotional_state"));
        var pscf = new PreparedStatementCreatorFactory(insertSql, declaredParameters) {
            {
                setReturnGeneratedKeys(true);
                setGeneratedKeysColumnNames("id");
            }
        };
        var psc = pscf.newPreparedStatementCreator(List.of(name, index));
        // 该类返回新增记录时的自增长主键值
        var kh = new GeneratedKeyHolder();
        template.update(psc, kh);
        // 获取主键值
        if (kh.getKey() instanceof Integer id) {
            return findById(id);
        }
        throw new IllegalArgumentException("we couldn't create the " + name + "!");
    }

    public Person findById(Integer id) {
        return this.template.queryForObject(this.findByIdSql, new Object[]{id}, this.personRowMapper);
    }
}


@Component
class Runner {

    private final PeopleService peopleService;

    Runner(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void exercise() throws Exception {
        var elizabeth = peopleService.create("Elizabeth", EmotionalState.SAD);
        System.out.println(elizabeth);
    }
}