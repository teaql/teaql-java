
# TeaQL Java CRUD Guide

Generated for `com.teaql.merchantorderprivacyservice`. Use this guide when adding controllers, services, jobs, or integration code in this workspace.

## Setup

```java
import com.teaql.merchantorderprivacyservice.Q;
import com.teaql.merchantorderprivacyservice.MerchantOrderPrivacyServiceUserContext;
import io.teaql.core.web.WebResponse;
```

The console entry point manually builds TeaQLRuntime and creates `new CustomUserContext(runtime)`.
Pass that context to generated Q/E APIs and audited saves.

## Non-Negotiable Database Rule

Never use SQL to operate on the database from workspace business code. This includes select, insert, update, delete, schema changes, JDBC templates, native queries, direct repositories, and ad hoc SQL helpers.

If the generated TeaQL API does not provide a path for the requested change, stop and report the missing API. Do not implement a SQL workaround.

## Mandatory Update Method Rule

Use generated `updateXxx(...)` methods for all entity field changes. Do not use `setXxx(...)` in new code. Setters are deprecated even when the IDE or generated class makes them visible.

```java
entity.updateName("new value");
```

Do not write:

```java
entity.setName("new value");
```

## Read One Entity

Prefer typed generated query entry points. Controller methods that return one object should wrap the result in `WebResponse.of(...)`:

```java
public WebResponse getOne(@TQLContext UserContext userContext, Long id) {
    try {
        var entity = Q.merchants()
            .filterById(id)
            .selectSelf()
            .comment("load detail by id")
            .purpose("get task detail")
            .executeForOne(userContext);
        return WebResponse.of(entity);
    } catch (Exception e) {
        return WebResponse.fail(e.getMessage());
    }
}
```

## Read A Page

Controller methods that return multiple objects should wrap the list in `WebResponse.of(...)`:

```java
public WebResponse list(@TQLContext UserContext userContext) {
    try {
        var list = Q.merchants()
            .selectSelf()
            .page(1, 20)
            .comment("list tasks")
            .purpose("render dashboard list")
            .executeForList(userContext);
        return WebResponse.of(list);
    } catch (Exception e) {
        return WebResponse.fail(e.getMessage());
    }
}
```

## Create

```java
var entity = new Merchant();
// Fill fields with generated updateXxx(...) methods, not setXxx(...).
entity.auditAs("Create new item").save(userContext);
```

## Update

```java
var entity = Q.merchants()
    .filterById(id)
    .selectSelf()
    .comment("load for update")
    .purpose("edit entity")
    .executeForOne(userContext);

// Use updateXxx(...) methods for state changes.
// entity.updateName("new value");
entity.auditAs("Update entity properties").save(userContext);
```

## Load Relations

Use generated selectors. Do not write a loop that queries children one row at a time. The examples below focus on the entities with the highest reverse relation counts because they are stronger aggregate-root candidates.

```java
var list = Q.merchants()
    .selectCustomerProfileListWith(Q.customerProfiles().selectSelf())
    .comment("load with customerProfileList")
    .purpose("fetch child customerProfileList")
    .executeForList(userContext);
```

```java
var list = Q.merchants()
    .selectCustomerOrderListWith(Q.customerOrders().selectSelf())
    .comment("load with customerOrderList")
    .purpose("fetch child customerOrderList")
    .executeForList(userContext);
```

```java
var list = Q.customerProfiles()
    .selectMerchantWith(Q.merchants().selectSelf())
    .comment("load with merchant")
    .purpose("fetch associated merchant")
    .executeForList(userContext);
```


```java
var list = Q.customerProfiles()
    .selectCustomerOrderListWith(Q.customerOrders().selectSelf())
    .comment("load with customerOrderList")
    .purpose("fetch child customerOrderList")
    .executeForList(userContext);
```


## Delete

Use `java-assist-delete/[entity]` with the original KSML input to get the exact
soft-delete API. If Assist lacks the operation, report `MISSING_ASSIST`.

Do not hard-delete rows. Do not write SQL `DELETE` or `UPDATE` statements. If the soft-delete API is not visible, stop and report that blocker instead of changing data through SQL.

## Common Mistakes

- Do not instantiate repositories directly in workspace business code.
- Do not use SQL for any database operation.
- Do not use deprecated `setXxx(...)` methods for updates. Use `updateXxx(...)`.
- Do not return raw entities or lists from controller query methods. Return `WebResponse.of(entity)` or `WebResponse.of(list)`.
- Do not forget `.executeForOne(userContext)` or `.executeForList(userContext)`.
- Do not forget `.comment("...")` and `.purpose("...")` on queries.
- Do not forget `.auditAs("...")` before calling `.save(context)` on entities.
- Do not assume relations are loaded unless the query selected them.
- Do not bypass `UserContext`; context carries logging, tenant, security, and repository resolution behavior.

## Entity Cheat Sheet

These entities are selected by reverse relation count, not by model declaration order.

- `Merchant`: reverse relations `2`, query `Q.merchants()`, save `new Merchant().auditAs("comment").save(userContext)`, request `com.teaql.merchantorderprivacyservice.merchant.MerchantRequest`
- `CustomerProfile`: reverse relations `1`, query `Q.customerProfiles()`, save `new CustomerProfile().auditAs("comment").save(userContext)`, request `com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileRequest`