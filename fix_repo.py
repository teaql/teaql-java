import re

file_path = "teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove legacy imports
content = content.replace("import io.teaql.core.repository.AbstractRepository;", "")
content = content.replace("import io.teaql.core.DefaultUserContext;", "")

# 2. Modify class declaration
content = content.replace(
    "public class PortableSQLRepository<T extends Entity> extends AbstractRepository<T>\n        implements SqlCompilerDelegate {",
    "public class PortableSQLRepository<T extends Entity> implements SqlCompilerDelegate {"
)

# 3. Add resolver and update constructor
constructor_old = """    public PortableSQLRepository(EntityDescriptor entityDescriptor, TeaQLDatabase database) {
        this.entityDescriptor = entityDescriptor;
        this.database = database;
        initSQLMeta(entityDescriptor);
    }"""

constructor_new = """    public interface PortableSQLRepositoryResolver {
        PortableSQLRepository<?> resolve(String typeName);
    }

    private PortableSQLRepositoryResolver resolver;

    public PortableSQLRepositoryResolver getResolver() {
        return resolver;
    }

    public PortableSQLRepository(EntityDescriptor entityDescriptor, TeaQLDatabase database, PortableSQLRepositoryResolver resolver) {
        this.entityDescriptor = entityDescriptor;
        this.database = database;
        this.resolver = resolver;
        initSQLMeta(entityDescriptor);
    }"""

content = content.replace(constructor_old, constructor_new)

# 4. Remove afterLoad call
after_load_old = """        if (userContext instanceof DefaultUserContext) {
            ((DefaultUserContext) userContext).afterLoad(getEntityDescriptor(), entity);
        }"""
content = content.replace(after_load_old, "")

# 5. Remove prepareId generator fallback check
prepare_id_old = """        if (userContext instanceof DefaultUserContext) {
            Long id = ((DefaultUserContext) userContext).generateId(entity);
            if (id != null) return id;
        }"""
content = content.replace(prepare_id_old, "")

# 6. Update ensureTableEnabled check
ensure_table_old = """    protected boolean ensureTableEnabled(UserContext ctx) {
        if (ctx instanceof DefaultUserContext dctx) {
            return dctx.config() != null && dctx.config().isEnsureTable();
        }
        return false;
    }"""
ensure_table_new = """    protected boolean ensureTableEnabled(UserContext ctx) {
        return ctx.getBool("ensureTable", true);
    }"""
content = content.replace(ensure_table_old, ensure_table_new)

# 7. Remove overrides for repository internal operations
overrides = [
    "@Override\n    public EntityDescriptor getEntityDescriptor()",
    "@Override\n    public void createInternal(UserContext userContext, Collection<T> createItems)",
    "@Override\n    public void updateInternal(UserContext userContext, Collection<T> updateItems)",
    "@Override\n    public void deleteInternal(UserContext userContext, Collection<T> entities)",
    "@Override\n    public void recoverInternal(UserContext userContext, Collection<T> entities)",
    "@Override\n    public Long prepareId(UserContext userContext, T entity)",
    "@Override\n    protected AggregationResult doAggregateInternal(UserContext userContext, SearchRequest<T> request)",
    "@Override\n    public Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request, int enhanceBatch)",
    "@Override\n    public String escapeIdentifier(String identifier)",
    "@Override\n    public List<SQLColumn> getPropertyColumns(String idTable, String propertyName)"
]

for o in overrides:
    o_no_override = o.replace("@Override\n", "")
    content = content.replace(o, o_no_override)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("PortableSQLRepository successfully updated via python script.")
