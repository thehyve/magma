/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.magma.datasource.mongodb.converter;

import org.bson.types.BasicBSONList;
import org.obiba.magma.Attribute;
import org.obiba.magma.Category;
import org.obiba.magma.ValueType;
import org.obiba.magma.Variable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

public class VariableConverter {

  public static String normalizeFieldName(String name) {
    return name.replaceAll("[.$]", "_");
  }

  public static Variable unmarshall(DBObject object) {
    ValueType valueType = ValueType.Factory.forName(object.get("valueType").toString());
    String entityType = object.get("entityType").toString();

    Variable.Builder builder = Variable.Builder.newVariable(object.get("name").toString(), valueType, entityType) //
        .repeatable(Boolean.parseBoolean(object.get("repeatable").toString())) //
        .mimeType(getFieldAsString(object, "mimeType")) //
        .mimeType(getFieldAsString(object, "referencedEntityType")) //
        .mimeType(getFieldAsString(object, "occurrenceGroup")) //
        .mimeType(getFieldAsString(object, "unit"));

    if(object.containsField("categories")) {
      builder.addCategories(unmarshallCategories((BasicBSONList) object.get("categories")));
    }

    if(object.containsField("attributes")) {
      builder.addAttributes(unmarshallAttributes((BasicBSONList) object.get("attributes")));
    }

    return builder.build();
  }

  private static Iterable<Category> unmarshallCategories(BasicBSONList cats) {
    ImmutableList.Builder<Category> list = ImmutableList.builder();
    for(Object o : cats) {
      DBObject cat = (DBObject) o;
      Category.Builder catBuilder = Category.Builder.newCategory(cat.get("name").toString()).missing(Boolean.parseBoolean(cat.get("missing").toString()));
      if(cat.containsField("attributes")) {
        catBuilder.addAttributes(unmarshallAttributes((BasicBSONList) cat.get("attributes")));
      }
      list.add(catBuilder.build());
    }
    return list.build();
  }

  private static Iterable<Attribute> unmarshallAttributes(BasicBSONList attrs) {
    ImmutableList.Builder<Attribute> list = ImmutableList.builder();
    for(Object o : attrs) {
      DBObject attr = (DBObject) o;
      String value = getFieldAsString(attr, "value");
      if(!Strings.isNullOrEmpty(value)) {
        Attribute.Builder attrBuilder = Attribute.Builder.newAttribute(attr.get("name").toString()) //
            .withNamespace(getFieldAsString(attr, "namespace")).withValue(value);

        String locale = getFieldAsString(attr, "locale");
        if(!Strings.isNullOrEmpty(locale)) attrBuilder.withLocale(locale);

        list.add(attrBuilder.build());
      }
    }
    return list.build();
  }

  private static String getFieldAsString(DBObject object, String key) {
    if(!object.containsField(key)) return null;
    Object value = object.get(key);
    return value == null ? null : value.toString();
  }

  public static DBObject marshall(Variable variable) {
    BasicDBObjectBuilder builder = BasicDBObjectBuilder.start("_id", variable.getName()) //
        .add("name", variable.getName()) //
        .add("valueType", variable.getValueType().getName()) //
        .add("entityType", variable.getEntityType()) //
        .add("mimeType", variable.getMimeType()) //
        .add("repeatable", variable.isRepeatable()) //
        .add("occurrenceGroup", variable.getOccurrenceGroup()) //
        .add("referencedEntityType", variable.getReferencedEntityType()) //
        .add("unit", variable.getUnit());

    if(variable.hasCategories()) {
      BasicDBList list = new BasicDBList();
      for(Category category : variable.getCategories()) {
        list.add(marshall(category));
      }
      builder.add("categories", list);
    }

    if(variable.hasAttributes()) {
      BasicDBList list = new BasicDBList();
      for(Attribute attribute : variable.getAttributes()) {
        list.add(marshall(attribute));
      }
      builder.add("attributes", list);
    }

    return builder.get();
  }

  private static DBObject marshall(Category category) {
    BasicDBObjectBuilder builder = BasicDBObjectBuilder.start() //
        .add("name", category.getName()).add("missing", category.isMissing());
    if(category.hasAttributes()) {
      BasicDBList list = new BasicDBList();
      for(Attribute attribute : category.getAttributes()) {
        list.add(marshall(attribute));
      }
      builder.add("attributes", list);
    }
    return builder.get();
  }

  private static DBObject marshall(Attribute attribute) {
    return BasicDBObjectBuilder.start()//
        .add("namespace", attribute.getNamespace()) //
        .add("name", attribute.getName())//
        .add("locale", attribute.getLocale() == null ? null : attribute.getLocale().toString()) //
        .add("value", attribute.getValue().toString()).get();
  }

}