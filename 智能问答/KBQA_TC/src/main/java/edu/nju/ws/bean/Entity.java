package edu.nju.ws.bean;

import java.util.Objects;

/**
 * 实体/属性 查询和建索引的基本单位
 */
public class Entity {
    public String IRI; //若为literal，则为label原型
    public String label; //包含标识符、中文名称等
    public String type; //指明类型，包括实体/属性

    public Entity(String IRI, String label, String type) {
        this.IRI = IRI;
        this.label = label;
        this.type = type;
    }

    @Override
    public Entity clone() {
        return new Entity(this.IRI, this.label, this.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return Objects.equals(IRI, entity.IRI) &&
                Objects.equals(label, entity.label) &&
                Objects.equals(type, entity.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(IRI, label, type);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "IRI='" + IRI + '\'' +
                ", label='" + label + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
