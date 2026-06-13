package io.teaql.core.sql;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLProperty;
import io.teaql.core.RepositoryException;
import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.ObjectUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SqlEntityMetadata {
    private final EntityDescriptor entityDescriptor;
    
    private String versionTableName;
    private List<String> primaryTableNames = new ArrayList<>();
    private String thisPrimaryTableName;
    private Set<String> allTableNames = new LinkedHashSet<>();
    private List<String> types = new ArrayList<>();
    private List<String> auxiliaryTableNames;
    private List<PropertyDescriptor> allProperties = new ArrayList<>();

    public SqlEntityMetadata(EntityDescriptor entityDescriptor) {
        this.entityDescriptor = entityDescriptor;
        initSQLMeta(entityDescriptor);
    }

    private void initSQLMeta(EntityDescriptor entityDescriptor) {
        EntityDescriptor descriptor = entityDescriptor;
        while (descriptor != null) {
            types.add(descriptor.getType());
            List<PropertyDescriptor> properties = descriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                allProperties.add(property);
                if (property instanceof Relation && !shouldHandle((Relation) property)) {
                    continue;
                }
                List<SQLColumn> sqlColumns = getSqlColumns(property);
                if (ObjectUtil.isEmpty(sqlColumns)) {
                    throw new RepositoryException(
                            "property :" + property.getName() + " miss sql table columns");
                }

                String firstTable = sqlColumns.get(0).getTableName();
                if (property.isVersion()) {
                    this.versionTableName = firstTable;
                }
                if (property.isId()) {
                    if (!this.primaryTableNames.contains(firstTable)) {
                        this.primaryTableNames.add(firstTable);
                    }
                    if (property.getOwner() == this.entityDescriptor) {
                        this.thisPrimaryTableName = firstTable;
                    }
                }
                this.allTableNames.addAll(CollStreamUtil.toList(sqlColumns, SQLColumn::getTableName));
            }
            descriptor = descriptor.getParent();
        }
        this.auxiliaryTableNames =
                new ArrayList<>(CollectionUtil.subtract(this.allTableNames, this.primaryTableNames));
    }

    private boolean shouldHandle(Relation relation) {
        // SQLRepository specific logic for relations
        return true; 
    }

    private List<SQLColumn> getSqlColumns(PropertyDescriptor property) {
        if (property instanceof SQLProperty) {
            return ((SQLProperty) property).columns();
        }
        throw new RepositoryException("SQLRepository only support SQLProperty");
    }

    public EntityDescriptor getEntityDescriptor() { return entityDescriptor; }
    public String getVersionTableName() { return versionTableName; }
    public List<String> getPrimaryTableNames() { return primaryTableNames; }
    public String getThisPrimaryTableName() { return thisPrimaryTableName; }
    public Set<String> getAllTableNames() { return allTableNames; }
    public List<String> getTypes() { return types; }
    public List<String> getAuxiliaryTableNames() { return auxiliaryTableNames; }
    public List<PropertyDescriptor> getAllProperties() { return allProperties; }
}
