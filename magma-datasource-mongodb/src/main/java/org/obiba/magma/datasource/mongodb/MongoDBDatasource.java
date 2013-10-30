/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.magma.datasource.mongodb;

import java.util.Set;

import javax.annotation.Nonnull;

import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.support.AbstractDatasource;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBPort;

public class MongoDBDatasource extends AbstractDatasource {

  public static final String TYPE = "mongodb";

  static final String DEFAULT_HOST = "localhost";

  static final int DEFAULT_PORT = DBPort.PORT;

  private static final String VALUE_TABLE_COLLECTION = "value_table";

  @Nonnull
  private final MongoDBFactory mongoDBFactory;

  /**
   * See <a href="http://docs.mongodb.org/manual/reference/connection-string">MongoDB connection string specifications</a>.
   *
   * @param name
   * @param mongoURI
   */
  public MongoDBDatasource(@Nonnull String name, @Nonnull MongoDBFactory mongoDBFactory) {
    super(name, TYPE);
    this.mongoDBFactory = mongoDBFactory;
  }

  DB getDB() {
    return mongoDBFactory.getDB();
  }

  @Nonnull
  MongoDBFactory getMongoDBFactory() {
    return mongoDBFactory;
  }

  DBCollection getValueTableCollection() {
    return getDB().getCollection(VALUE_TABLE_COLLECTION);
  }

  @Override
  protected void onInitialise() {
    mongoDBFactory.getMongoClient();
  }

  @Override
  public boolean canDropTable(String tableName) {
    return true;
  }

  @Override
  public void dropTable(@Nonnull String tableName) {
    MongoDBValueTable valueTable = (MongoDBValueTable) getValueTable(tableName);
    valueTable.drop();
    removeValueTable(tableName);
  }

  @Override
  public boolean canDrop() {
    return true;
  }

  @Override
  public void drop() {
    for(ValueTable valueTable : getValueTables()) {
      dropTable(valueTable.getName());
    }
    getDB().dropDatabase();
  }

  @Nonnull
  @Override
  public ValueTableWriter createWriter(@Nonnull String tableName, @Nonnull String entityType) {
    MongoDBValueTable valueTable = null;
    if(hasValueTable(tableName)) {
      valueTable = (MongoDBValueTable) getValueTable(tableName);
    } else {
      addValueTable(valueTable = new MongoDBValueTable(this, tableName, entityType));
    }
    return new MongoDBValueTableWriter(valueTable);
  }

  @Override
  protected Set<String> getValueTableNames() {
    DBCursor cursor = getValueTableCollection()
        .find(BasicDBObjectBuilder.start().add("datasource",getName()).get(), BasicDBObjectBuilder.start().add("name", 1).get());
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    try {
      while(cursor.hasNext()) {
        builder.add(cursor.next().get("name").toString());
      }
    } finally {
      cursor.close();
    }
    return builder.build();
  }

  @Override
  protected ValueTable initialiseValueTable(String tableName) {
    return new MongoDBValueTable(this, tableName);
  }
}